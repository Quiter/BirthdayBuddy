package com.heckmannch.birthdaybuddy.data.repository

import android.content.ContentResolver
import android.util.Log
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.heckmannch.birthdaybuddy.data.local.AppDatabase
import com.heckmannch.birthdaybuddy.data.local.AppSettingsDao
import com.heckmannch.birthdaybuddy.data.local.AppSettingsEntity
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.ContactEntity
import com.heckmannch.birthdaybuddy.data.local.ContactUserData
import com.heckmannch.birthdaybuddy.data.local.ContactUserDataDao
import com.heckmannch.birthdaybuddy.data.local.LabelConfigDao
import com.heckmannch.birthdaybuddy.data.local.LabelConfigEntity
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase
import com.heckmannch.birthdaybuddy.data.mapper.ContactDbMapper
import com.heckmannch.birthdaybuddy.data.mapper.LabelConfigMapper
import com.heckmannch.birthdaybuddy.di.DefaultDispatcher
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
import com.heckmannch.birthdaybuddy.domain.model.PotentialCouple
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of [ContactRepository] that acts as the single source of truth
 * for contacts, user-defined labels, and gift ideas in the application.
 *
 * It bridges the local Room database ([AppDatabase], [SettingsDatabase]) with the Android
 * system's contact provider ([SystemContactDataSource]). It handles contact synchronization,
 * updates home widgets, exports/imports gift ideas, and handles coroutine dispatching
 * offloading heavy mapping calculations to [DefaultDispatcher] or [IoDispatcher].
 */
class ContactRepositoryImpl @Inject constructor(
    private val permissionChecker: PermissionChecker,
    private val contentResolver: ContentResolver,
    private val contactDao: ContactDao,
    private val labelConfigDao: LabelConfigDao,
    private val appSettingsDao: AppSettingsDao,
    private val contactUserDataDao: ContactUserDataDao,
    private val systemContactDataSource: SystemContactDataSource,
    private val giftIdeaBackupManager: GiftIdeaBackupManager,
    private val calendarSyncRepository: CalendarSyncRepository,
    private val widgetUpdater: WidgetUpdater,
    private val appDatabase: AppDatabase,
    private val settingsDatabase: SettingsDatabase,
    private val contactDbMapper: ContactDbMapper,
    private val labelConfigMapper: LabelConfigMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ContactRepository {

    /**
     * Mutex ensuring mutual exclusion for [syncContacts] so that concurrent synchronization
     * requests execute sequentially and do not cause race conditions on database and calendar caches.
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

    override val potentialCouples: Flow<List<PotentialCouple>> = contactDao.getPotentialCouples()
        .distinctUntilChanged()

    // Same reasoning as allContacts: O(n) mapping offloaded to Dispatchers.Default.
    override val labelConfigs: Flow<List<LabelConfig>> = labelConfigDao.getAllConfigs()
        .map { entities -> entities.map { labelConfigMapper.toDomain(it) } }
        .flowOn(defaultDispatcher)
        .distinctUntilChanged()

    override val otherEventsEnabled: Flow<Boolean> = appSettingsDao.getSettings()
        .map { it?.otherEventsEnabled ?: false }
        .distinctUntilChanged()

    override val ignoredCouplePairs: Flow<List<String>> = appSettingsDao.getSettings()
        .map { it?.ignoredCouplePairs ?: emptyList() }
        .distinctUntilChanged()

    override val labelsEnabled: Flow<Boolean> = appSettingsDao.getSettings()
        .map { it?.labelsEnabled ?: true }
        .distinctUntilChanged()

    override suspend fun getAllContactsImmediate(): List<Contact> = withContext(ioDispatcher) {
        contactDao.getAllContactsImmediate().map { contactDbMapper.toDomain(it) }
    }

    override suspend fun syncContacts() = withContext(ioDispatcher) {
        syncMutex.withLock {
            try {
                if (!permissionChecker.hasContactsPermission()) return@withLock

                coroutineScope {
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

                    // 2. Synchronize labels
                    syncLabelConfigs(systemContacts, dbConfigs, groups)

                    // 3. Diffing: Reconcile contacts (CPU-intensive operation offloaded to Dispatchers.Default)
                    val (finalContacts, finalEntities) = withContext(defaultDispatcher) {
                        val contacts = systemContacts.map { systemContact ->
                            val lookupKey = systemContact.lookupKey
                            val existing = dbContacts[lookupKey]
                            val userData = userDataMap[lookupKey]

                            systemContact.copy(
                                localId = existing?.localId ?: 0,
                                giftIdeas = userData?.giftIdeas ?: existing?.giftIdeas ?: emptyList(),
                                spouseLookupKey = userData?.spouseLookupKey
                            )
                        }
                        val entities = contacts.map { contactDbMapper.toEntity(it) }
                        contacts to entities
                    }

                    // 4. Batch update via transaction
                    contactDao.refreshContacts(finalEntities)

                    // 5. Synchronize calendar if enabled
                    val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettingsEntity()
                    if (currentSettings.calendarSyncEnabled) {
                        calendarSyncRepository.syncBirthdays(finalContacts)
                    }

                    // 6. Update timestamp
                    appSettingsDao.upsertSettings(currentSettings.copy(lastSyncTimestamp = System.currentTimeMillis()))
                }
                widgetUpdater.updateWidget()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ContactRepository", "Error during contact sync: ${e.message}", e)
            }
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

    /**
     * Executes a 2-phase transactional update:
     * 1. Reads previous state from SettingsDB / Room.
     * 2. Writes updated data to SettingsDB (Source of Truth).
     * 3. Updates AppDB cache. If this fails, rolls back the SettingsDB change and rethrows.
     */
    private suspend fun <T> executeWithSettingsRollback(
        readPreviousState: suspend () -> T,
        writeSettings: suspend (T) -> Unit,
        rollbackSettings: suspend (T) -> Unit,
        updateAppDbCache: suspend () -> Unit,
        errorMessage: String
    ) {
        val previousState = readPreviousState()
        settingsDatabase.withTransaction {
            writeSettings(previousState)
        }
        try {
            appDatabase.withTransaction {
                updateAppDbCache()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("ContactRepository", "$errorMessage: ${e.message}", e)
            settingsDatabase.withTransaction {
                rollbackSettings(previousState)
            }
            throw e
        }
    }

    private suspend fun updateGiftIdeas(lookupKey: String, ideas: List<GiftIdea>) {
        withContext(ioDispatcher) {
            executeWithSettingsRollback(
                readPreviousState = {
                    contactUserDataDao.getUserDataForContact(lookupKey)
                },
                writeSettings = { prevUserData ->
                    contactUserDataDao.upsertUserData(
                        ContactUserData(
                            lookupKey = lookupKey,
                            giftIdeas = ideas,
                            spouseLookupKey = prevUserData?.spouseLookupKey
                        )
                    )
                },
                rollbackSettings = { prevUserData ->
                    val rollbackData = prevUserData
                        ?: ContactUserData(lookupKey = lookupKey, giftIdeas = emptyList())
                    contactUserDataDao.upsertUserData(rollbackData)
                },
                updateAppDbCache = {
                    contactDao.getContactByLookupKey(lookupKey)?.let { contact ->
                        contactDao.upsertContact(contact.copy(giftIdeas = ideas))
                    }
                },
                errorMessage = "Failed to update gift idea cache, rolling back"
            )
        }
        widgetUpdater.updateWidget()
    }

    private suspend fun resolveContactForGiftIdea(
        lookupKey: String,
        ideaId: String
    ): Pair<String, ContactEntity>? {
        val contact = contactDao.getContactByLookupKey(lookupKey) ?: return null
        if (contact.giftIdeas.any { it.id == ideaId }) {
            return lookupKey to contact
        }
        val spouseKey = contact.spouseLookupKey ?: return null
        val spouseContact = contactDao.getContactByLookupKey(spouseKey) ?: return null
        if (spouseContact.giftIdeas.any { it.id == ideaId }) {
            return spouseKey to spouseContact
        }
        return null
    }

    override suspend fun addGiftIdea(lookupKey: String, newIdea: GiftIdea) =
        withContext(ioDispatcher) {
            val contact = contactDao.getContactByLookupKey(lookupKey) ?: return@withContext
            val updatedIdeas = GiftIdea.withNewIdea(contact.giftIdeas, newIdea)
            updateGiftIdeas(lookupKey, updatedIdeas)
        }

    override suspend fun toggleGiftIdea(lookupKey: String, idea: GiftIdea, isChecked: Boolean) =
        withContext(ioDispatcher) {
            val (targetKey, targetContact) = resolveContactForGiftIdea(lookupKey, idea.id)
                ?: return@withContext
            val updatedIdeas = GiftIdea.withToggledIdea(targetContact.giftIdeas, idea, isChecked)
            updateGiftIdeas(targetKey, updatedIdeas)
        }

    override suspend fun deleteGiftIdea(lookupKey: String, ideaId: String) =
        withContext(ioDispatcher) {
            val (targetKey, targetContact) = resolveContactForGiftIdea(lookupKey, ideaId)
                ?: return@withContext
            val updatedIdeas = targetContact.giftIdeas.filter { it.id != ideaId }
            updateGiftIdeas(targetKey, updatedIdeas)
        }

    override suspend fun updateGiftIdeaText(lookupKey: String, ideaId: String, newText: String) =
        withContext(ioDispatcher) {
            val (targetKey, targetContact) = resolveContactForGiftIdea(lookupKey, ideaId)
                ?: return@withContext
            val updatedIdeas = targetContact.giftIdeas.map {
                if (it.id == ideaId) it.copy(text = newText) else it
            }
            updateGiftIdeas(targetKey, updatedIdeas)
        }

    override suspend fun updateLabelConfig(config: LabelConfig) {
        labelConfigDao.upsertConfig(labelConfigMapper.toEntity(config))
        widgetUpdater.updateWidget()
    }

    override suspend fun updateContactBirthday(
        contactId: String,
        birthday: java.time.LocalDate
    ): Boolean {
        val success = systemContactDataSource.updateContactBirthday(contactId, birthday)
        if (success) {
            syncContacts()
        }
        return success
    }

    private suspend fun exportGiftIdeasToJson(): String = giftIdeaBackupManager.exportGiftIdeas()

    private suspend fun importGiftIdeasFromJson(jsonString: String): Int {
        val count = giftIdeaBackupManager.importGiftIdeas(jsonString)
        if (count > 0) {
            syncContacts() // Update cache
        }
        return count
    }

    override suspend fun exportGiftIdeas(uriString: String) {
        val json = exportGiftIdeasToJson()
        val uri = uriString.toUri()
        withContext(ioDispatcher) {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
            }
        }
    }

    override suspend fun importGiftIdeas(uriString: String): Int {
        val uri = uriString.toUri()
        val json = withContext(ioDispatcher) {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            }
        } ?: return -1
        return importGiftIdeasFromJson(json)
    }

    override suspend fun linkAsCouple(lookupKey1: String, lookupKey2: String) {
        withContext(ioDispatcher) {
            executeWithSettingsRollback(
                readPreviousState = {
                    val prevUserData1 = contactUserDataDao.getUserDataForContact(lookupKey1)
                    val prevUserData2 = contactUserDataDao.getUserDataForContact(lookupKey2)
                    prevUserData1 to prevUserData2
                },
                writeSettings = { (prevUserData1, prevUserData2) ->
                    val userData1 = prevUserData1 ?: ContactUserData(lookupKey = lookupKey1)
                    val userData2 = prevUserData2 ?: ContactUserData(lookupKey = lookupKey2)
                    contactUserDataDao.upsertUserData(userData1.copy(spouseLookupKey = lookupKey2))
                    contactUserDataDao.upsertUserData(userData2.copy(spouseLookupKey = lookupKey1))
                },
                rollbackSettings = { (prevUserData1, prevUserData2) ->
                    if (prevUserData1 != null) {
                        contactUserDataDao.upsertUserData(prevUserData1)
                    } else {
                        contactUserDataDao.getUserDataForContact(lookupKey1)?.let {
                            contactUserDataDao.upsertUserData(it.copy(spouseLookupKey = null))
                        }
                    }
                    if (prevUserData2 != null) {
                        contactUserDataDao.upsertUserData(prevUserData2)
                    } else {
                        contactUserDataDao.getUserDataForContact(lookupKey2)?.let {
                            contactUserDataDao.upsertUserData(it.copy(spouseLookupKey = null))
                        }
                    }
                },
                updateAppDbCache = {
                    contactDao.getContactByLookupKey(lookupKey1)?.let { contact ->
                        contactDao.upsertContact(contact.copy(spouseLookupKey = lookupKey2))
                    }
                    contactDao.getContactByLookupKey(lookupKey2)?.let { contact ->
                        contactDao.upsertContact(contact.copy(spouseLookupKey = lookupKey1))
                    }
                },
                errorMessage = "Cache update failed for linkAsCouple, rolling back settings"
            )
        }
        updateWidgetAndSyncCalendar()
    }

    override suspend fun unlinkCouple(lookupKey: String) {
        withContext(ioDispatcher) {
            val contact = contactDao.getContactByLookupKey(lookupKey) ?: return@withContext
            val spouseKey = contact.spouseLookupKey ?: return@withContext

            executeWithSettingsRollback(
                readPreviousState = {
                    val prevUserData1 = contactUserDataDao.getUserDataForContact(lookupKey)
                    val prevUserData2 = contactUserDataDao.getUserDataForContact(spouseKey)
                    prevUserData1 to prevUserData2
                },
                writeSettings = { (prevUserData1, prevUserData2) ->
                    prevUserData1?.let { contactUserDataDao.upsertUserData(it.copy(spouseLookupKey = null)) }
                    prevUserData2?.let { contactUserDataDao.upsertUserData(it.copy(spouseLookupKey = null)) }
                },
                rollbackSettings = { (prevUserData1, prevUserData2) ->
                    prevUserData1?.let { contactUserDataDao.upsertUserData(it) }
                    prevUserData2?.let { contactUserDataDao.upsertUserData(it) }
                },
                updateAppDbCache = {
                    contactDao.getContactByLookupKey(lookupKey)?.let {
                        contactDao.upsertContact(it.copy(spouseLookupKey = null))
                    }
                    contactDao.getContactByLookupKey(spouseKey)?.let {
                        contactDao.upsertContact(it.copy(spouseLookupKey = null))
                    }
                },
                errorMessage = "Cache update failed for unlinkCouple, rolling back settings"
            )
        }
        updateWidgetAndSyncCalendar()
    }

    private suspend fun updateWidgetAndSyncCalendar() {
        widgetUpdater.updateWidget()
        val allContactsImmediate = getAllContactsImmediate()
        val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettingsEntity()
        if (currentSettings.calendarSyncEnabled) {
            calendarSyncRepository.syncBirthdays(allContactsImmediate)
        }
    }

    override suspend fun ignoreCoupleSuggestion(lookupKey1: String, lookupKey2: String) {
        withContext(ioDispatcher) {
            settingsDatabase.withTransaction {
                val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettingsEntity()
                val pairKey =
                    if (lookupKey1 < lookupKey2) "$lookupKey1:$lookupKey2" else "$lookupKey2:$lookupKey1"
                if (!currentSettings.ignoredCouplePairs.contains(pairKey)) {
                    val updatedList = currentSettings.ignoredCouplePairs + pairKey
                    appSettingsDao.upsertSettings(currentSettings.copy(ignoredCouplePairs = updatedList))
                }
            }
        }
    }

    override suspend fun clearIgnoredCouplePairs() {
        withContext(ioDispatcher) {
            settingsDatabase.withTransaction {
                val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettingsEntity()
                appSettingsDao.upsertSettings(currentSettings.copy(ignoredCouplePairs = emptyList()))
            }
        }
    }

    override suspend fun updateLabelsEnabled(enabled: Boolean) = withContext(ioDispatcher) {
        settingsDatabase.withTransaction {
            val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettingsEntity()
            appSettingsDao.upsertSettings(currentSettings.copy(labelsEnabled = enabled))
        }
    }
}
