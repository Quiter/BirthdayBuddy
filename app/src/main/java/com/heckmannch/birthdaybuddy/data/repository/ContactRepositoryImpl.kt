package com.heckmannch.birthdaybuddy.data.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.provider.ContactsContract
import android.util.Log
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.ContactEntity
import com.heckmannch.birthdaybuddy.data.local.ContactUserDataDao
import com.heckmannch.birthdaybuddy.data.local.GiftIdeaConverters
import com.heckmannch.birthdaybuddy.data.local.LabelConfigDao
import com.heckmannch.birthdaybuddy.data.local.LabelConfigEntity
import com.heckmannch.birthdaybuddy.data.mapper.ContactDbMapper
import com.heckmannch.birthdaybuddy.data.mapper.LabelConfigMapper
import com.heckmannch.birthdaybuddy.di.DefaultDispatcher
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import com.heckmannch.birthdaybuddy.util.hasYear
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

/**
 * Implementation of [ContactRepository] that acts as the single source of truth
 * for contacts, caching, and user-defined label configurations in the application.
 *
 * It bridges the local Room database cache with the Android system's contact provider
 * ([SystemContactDataSource]). It handles contact synchronization, label config synchronization,
 * downstream widget and calendar synchronization, and coroutine dispatching offloading heavy
 * mapping calculations to [DefaultDispatcher] or [IoDispatcher].
 */
class ContactRepositoryImpl @Inject constructor(
    private val permissionChecker: PermissionChecker,
    private val contactDao: ContactDao,
    private val labelConfigDao: LabelConfigDao,
    private val contactUserDataDao: ContactUserDataDao,
    private val systemContactDataSource: SystemContactDataSource,
    private val settingsRepository: SettingsRepository,
    private val calendarSyncRepository: CalendarSyncRepository,
    private val widgetUpdater: WidgetUpdater,
    private val contactDbMapper: ContactDbMapper,
    private val labelConfigMapper: LabelConfigMapper,
    private val contentResolver: ContentResolver,
    private val giftIdeaConverters: GiftIdeaConverters = GiftIdeaConverters(),
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ContactRepository {

    /**
     * Mutex ensuring mutual exclusion for [syncContacts] so that concurrent synchronization
     * requests execute sequentially and do not cause race conditions on database caches.
     */
    private val syncMutex = Mutex()

    // The entity-to-domain mapping is O(n) CPU work and is therefore explicitly
    // offloaded to Dispatchers.Default (project guideline §2.7). flowOn is placed
    // *before* distinctUntilChanged so that the deduplication check runs downstream
    // on whatever dispatcher the collector uses.
    override val allContacts: Flow<List<Contact>> = contactDao.getAllContacts()
        .map { entities -> entities.map { contactDbMapper.toDomain(it) } }
        .flowOn(defaultDispatcher)
        .distinctUntilChanged()

    // Same reasoning as allContacts: O(n) mapping offloaded to Dispatchers.Default.
    override val labelConfigs: Flow<List<LabelConfig>> = labelConfigDao.getAllConfigs()
        .map { entities -> entities.map { labelConfigMapper.toDomain(it) } }
        .flowOn(defaultDispatcher)
        .distinctUntilChanged()

    override val otherEventsEnabled: Flow<Boolean> = settingsRepository.settings
        .map { it.otherEventsEnabled }
        .distinctUntilChanged()

    override val disabledOtherEventLabels: Flow<Set<String>> = settingsRepository.settings
        .map { it.disabledOtherEventLabels.toSet() }
        .distinctUntilChanged()

    override val labelsEnabled: Flow<Boolean> = settingsRepository.settings
        .map { it.labelsEnabled }
        .distinctUntilChanged()

    /**
     * Observes changes to [ContactsContract.Contacts.CONTENT_URI] using a [ContentObserver]
     * within a [callbackFlow]. Closes cleanly if registering the observer fails due to a
     * [SecurityException], and automatically unregisters the observer in [awaitClose] only
     * if registration succeeded.
     */
    override val contactChanges: Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }

        val isRegistered = try {
            contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                observer
            )
            true
        } catch (_: SecurityException) {
            // Keine Berechtigung vorhanden — Flow sauber schließen, da kein Observer registriert werden kann
            close()
            false
        }

        awaitClose {
            if (isRegistered) {
                try {
                    contentResolver.unregisterContentObserver(observer)
                } catch (_: Exception) {
                    // Observer konnte nicht deregistriert werden
                }
            }
        }
    }

    override suspend fun getAllContactsImmediate(): List<Contact> = withContext(ioDispatcher) {
        contactDao.getAllContactsImmediate().map { contactDbMapper.toDomain(it) }
    }

    override suspend fun syncContacts() = withContext(ioDispatcher) {
        syncMutex.withLock {
            try {
                if (!permissionChecker.hasContactsPermission()) return@withLock

                val reconciledContacts = coroutineScope {
                    // 1. Load data from all sources in parallel
                    val systemDataDeferred = async {
                        val groups = systemContactDataSource.fetchContactGroups()
                        val contacts = systemContactDataSource.fetchContactsFromSystem(groups)
                        groups to contacts
                    }
                    val dbContactsDeferred =
                        async { contactDao.getAllContactsImmediate().associateBy { it.lookupKey } }
                    val dbConfigsDeferred =
                        async { labelConfigDao.getAllConfigsImmediate().associateBy { it.name } }
                    val userDataDeferred =
                        async {
                            contactUserDataDao.getAllUserDataImmediate().associateBy { it.lookupKey }
                        }

                    val (groups, systemContacts) = systemDataDeferred.await()
                    val dbContacts = dbContactsDeferred.await()
                    val dbConfigs = dbConfigsDeferred.await()
                    val userDataMap = userDataDeferred.await()

                    // 2. Diffing & Deduplication: Filter invalid lookupKeys, deduplicate by lookupKey,
                    //    and reconcile contacts (CPU-intensive operation offloaded to DefaultDispatcher).
                    //    Room enforces a UNIQUE index on lookupKey, so blank or duplicate keys would
                    //    otherwise trigger a SQLiteConstraintException in refreshContacts().
                    val (contacts, finalEntities) = withContext(defaultDispatcher) {
                        val deduplicatedByLookupKey = deduplicateContacts(systemContacts)
                        val reconciled = ArrayList<Contact>(deduplicatedByLookupKey.size)
                        val entities = ArrayList<ContactEntity>(deduplicatedByLookupKey.size)
                        for (systemContact in deduplicatedByLookupKey) {
                            val lookupKey = systemContact.lookupKey
                            val existing = dbContacts[lookupKey]
                            val userData = userDataMap[lookupKey]

                            val rawGiftIdeasJson = userData?.giftIdeasJson ?: existing?.giftIdeasJson
                            val contact = systemContact.copy(
                                giftIdeas = giftIdeaConverters.toGiftIdeaList(rawGiftIdeasJson),
                                spouseLookupKey = userData?.spouseLookupKey
                            )
                            reconciled.add(contact)
                            entities.add(
                                contactDbMapper.toEntity(contact, localId = existing?.localId ?: 0)
                            )
                        }
                        reconciled to entities
                    }

                    // 3. Synchronize labels using sanitized contacts
                    syncLabelConfigs(contacts, dbConfigs, groups)

                    // 4. Batch update via transaction
                    contactDao.refreshContacts(finalEntities)

                    // 5. Update timestamp
                    settingsRepository.updateSettings {
                        it.copy(lastSyncTimestamp = System.currentTimeMillis())
                    }

                    contacts
                }

                // 6. Notify home screen widget and Android system calendar defensively
                notifyWidgetAndCalendar(reconciledContacts)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error during contact sync: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Triggers an immediate home screen widget refresh and, when calendar sync is enabled
     * and calendar permissions are granted, synchronizes contact events with Android's system
     * calendar.
     *
     * Both side effects are independently guarded so a failure in either operation cannot
     * abort the other or break contact/birthday persistence. [CancellationException] is always
     * rethrown to preserve structured concurrency.
     */
    private suspend fun notifyWidgetAndCalendar(contacts: List<Contact>) {
        try {
            widgetUpdater.updateWidget()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget after contact sync: ${e.message}", e)
        }

        try {
            val currentSettings = settingsRepository.getSettingsImmediate()
            if (currentSettings.calendarSyncEnabled && permissionChecker.hasCalendarPermission()) {
                calendarSyncRepository.syncBirthdays(contacts)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing calendar after contact sync: ${e.message}", e)
        }
    }

    private suspend fun syncLabelConfigs(
        systemContacts: List<Contact>,
        existingConfigs: Map<String, LabelConfigEntity>,
        groups: Map<Long, GroupInfo>
    ) {
        labelConfigDao.deleteConfigsByNames(SystemContactDataSource.redundantLabels.toList())

        val allLabelsInSystem = systemContacts.asSequence().flatMap { it.labels }.toSet()
        val configsToInsert = buildList {
            // Process all system groups (excluding redundant ones)
            groups.values.asSequence()
                .filter { it.title.lowercase() !in SystemContactDataSource.redundantLabels }
                .distinctBy { it.title }
                .forEach { group ->
                    val existing = existingConfigs[group.title]
                    if ((existing == null) || (existing.isSystem != group.isSystem)) {
                        add(
                            LabelConfigEntity(
                                name = group.title,
                                isHiddenFromFilter = existing?.isHiddenFromFilter ?: false,
                                isIgnored = existing?.isIgnored ?: false,
                                isSystem = group.isSystem,
                                notificationsEnabled = existing?.notificationsEnabled ?: true,
                                showInWidget = existing?.showInWidget ?: true
                            )
                        )
                    }
                }

            // Add missing labels (which might not have a system group)
            allLabelsInSystem.forEach { label ->
                if (label.lowercase() !in SystemContactDataSource.redundantLabels &&
                    !existingConfigs.containsKey(label) &&
                    groups.values.none { it.title == label }
                ) {
                    add(LabelConfigEntity(name = label))
                }
            }
        }

        if (configsToInsert.isNotEmpty()) {
            labelConfigDao.upsertConfigs(configsToInsert)
        }
    }

    override suspend fun updateLabelConfig(config: LabelConfig): Unit = withContext(ioDispatcher) {
        labelConfigDao.upsertConfig(labelConfigMapper.toEntity(config))
        updateWidgetSafely()
    }

    override suspend fun updateContactBirthday(
        contactId: String,
        birthday: LocalDate
    ): Boolean = withContext(ioDispatcher) {
        val success = systemContactDataSource.updateContactBirthday(contactId, birthday)
        if (success) {
            syncContacts()
        }
        success
    }

    override suspend fun updateLabelsEnabled(enabled: Boolean) = withContext(ioDispatcher) {
        settingsRepository.updateSettings { it.copy(labelsEnabled = enabled) }
        updateWidgetSafely()
    }

    /**
     * Triggers a best-effort home screen widget refresh so widget rendering failures cannot
     * abort or mask already-committed label configuration updates.
     * [CancellationException] is rethrown to preserve structured concurrency.
     */
    private suspend fun updateWidgetSafely() {
        try {
            widgetUpdater.updateWidget()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget after label configuration update: ${e.message}", e)
        }
    }

    /**
     * Filters out contacts with blank `lookupKey` values and deterministically deduplicates
     * entries sharing the same `lookupKey`.
     *
     * Preference order on collision:
     * 1. Entry with a birthday over an entry without a birthday.
     * 2. Entry whose birthday includes a birth year over one without a year.
     * 3. Entry with a higher [dataCompletenessScore].
     * 4. First seen entry on an exact tie.
     */
    private fun deduplicateContacts(systemContacts: List<Contact>): List<Contact> {
        val byLookupKey = LinkedHashMap<String, Contact>(systemContacts.size)
        for (contact in systemContacts) {
            val lookupKey = contact.lookupKey
            if (lookupKey.isBlank()) {
                Log.w(
                    TAG,
                    "Skipping contact with empty or blank lookupKey during sync (contactId=${contact.contactId})"
                )
                continue
            }
            val existing = byLookupKey[lookupKey]
            if (existing == null) {
                byLookupKey[lookupKey] = contact
            } else {
                Log.w(
                    TAG,
                    "Duplicate lookupKey detected during sync ($lookupKey); selecting preferred entry"
                )
                if (isPreferredDuplicate(candidate = contact, current = existing)) {
                    byLookupKey[lookupKey] = contact
                }
            }
        }
        return byLookupKey.values.toList()
    }

    private fun isPreferredDuplicate(candidate: Contact, current: Contact): Boolean {
        val candidateHasBirthday = candidate.birthday != null
        val currentHasBirthday = current.birthday != null
        if (candidateHasBirthday != currentHasBirthday) {
            return candidateHasBirthday
        }
        val candidateHasYear = candidate.birthday?.hasYear == true
        val currentHasYear = current.birthday?.hasYear == true
        if (candidateHasYear != currentHasYear) {
            return candidateHasYear
        }
        return candidate.dataCompletenessScore() > current.dataCompletenessScore()
    }

    private fun Contact.dataCompletenessScore(): Int {
        var score = 0
        if (anniversary != null) score += 1
        if (nameDay != null) score += 1
        score += customEvents.size
        if (!phoneNumber.isNullOrBlank()) score += 1
        if (!imageUri.isNullOrBlank()) score += 1
        score += labels.size
        if (isFavorite) score += 1
        if (hasWhatsApp) score += 1
        if (hasSignal) score += 1
        if (fullName.isNotBlank()) score += 1
        return score
    }

    companion object {
        private const val TAG = "ContactRepository"
    }
}
