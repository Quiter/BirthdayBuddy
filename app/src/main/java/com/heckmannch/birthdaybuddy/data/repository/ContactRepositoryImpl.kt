package com.heckmannch.birthdaybuddy.data.repository

import android.util.Log
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.ContactUserDataDao
import com.heckmannch.birthdaybuddy.data.local.LabelConfigDao
import com.heckmannch.birthdaybuddy.data.local.LabelConfigEntity
import com.heckmannch.birthdaybuddy.data.mapper.ContactDbMapper
import com.heckmannch.birthdaybuddy.data.mapper.LabelConfigMapper
import com.heckmannch.birthdaybuddy.di.DefaultDispatcher
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
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
import java.time.LocalDate
import javax.inject.Inject

/**
 * Implementation of [ContactRepository] that acts as the single source of truth
 * for contacts, caching, and user-defined label configurations in the application.
 *
 * It bridges the local Room database cache with the Android system's contact provider
 * ([SystemContactDataSource]). It handles contact synchronization, label config synchronization,
 * and handles coroutine dispatching offloading heavy mapping calculations to [DefaultDispatcher]
 * or [IoDispatcher].
 */
class ContactRepositoryImpl @Inject constructor(
    private val permissionChecker: PermissionChecker,
    private val contactDao: ContactDao,
    private val labelConfigDao: LabelConfigDao,
    private val contactUserDataDao: ContactUserDataDao,
    private val systemContactDataSource: SystemContactDataSource,
    private val settingsRepository: SettingsRepository,
    private val contactDbMapper: ContactDbMapper,
    private val labelConfigMapper: LabelConfigMapper,
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

    override val labelsEnabled: Flow<Boolean> = settingsRepository.settings
        .map { it.labelsEnabled }
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
                    val finalEntities = withContext(defaultDispatcher) {
                        systemContacts.map { systemContact ->
                            val lookupKey = systemContact.lookupKey
                            val existing = dbContacts[lookupKey]
                            val userData = userDataMap[lookupKey]

                            val contact = systemContact.copy(
                                giftIdeas = userData?.giftIdeas ?: existing?.giftIdeas ?: emptyList(),
                                spouseLookupKey = userData?.spouseLookupKey
                            )
                            contactDbMapper.toEntity(contact, localId = existing?.localId ?: 0)
                        }
                    }

                    // 4. Batch update via transaction
                    contactDao.refreshContacts(finalEntities)

                    // 5. Update timestamp
                    settingsRepository.updateSettings {
                        it.copy(lastSyncTimestamp = System.currentTimeMillis())
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error during contact sync: ${e.message}", e)
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

    override suspend fun updateLabelConfig(config: LabelConfig): Unit = withContext(ioDispatcher) {
        labelConfigDao.upsertConfig(labelConfigMapper.toEntity(config))
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
    }

    companion object {
        private const val TAG = "ContactRepository"
    }
}
