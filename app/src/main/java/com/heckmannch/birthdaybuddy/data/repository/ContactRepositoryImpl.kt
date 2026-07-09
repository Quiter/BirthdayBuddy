package com.heckmannch.birthdaybuddy.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import com.heckmannch.birthdaybuddy.data.local.AppDatabase
import com.heckmannch.birthdaybuddy.data.local.AppSettingsDao
import com.heckmannch.birthdaybuddy.data.local.AppSettingsEntity
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.ContactUserData
import com.heckmannch.birthdaybuddy.data.local.ContactUserDataDao
import com.heckmannch.birthdaybuddy.data.local.LabelConfigDao
import com.heckmannch.birthdaybuddy.data.local.LabelConfigEntity
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase
import com.heckmannch.birthdaybuddy.data.mapper.ContactDbMapper
import com.heckmannch.birthdaybuddy.data.mapper.LabelConfigMapper
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
import com.heckmannch.birthdaybuddy.domain.model.PotentialCouple
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of [ContactRepository] that acts as the single source of truth
 * for contacts, user-defined labels, and gift ideas in the application.
 *
 * It bridges the local Room database ([AppDatabase], [SettingsDatabase]) with the Android
 * system's contact provider ([SystemContactDataSource]). It handles contact synchronization,
 * updates home widgets, exports/imports gift ideas, and handles coroutine dispatching
 * offloading heavy mapping calculations to [Dispatchers.Default] or [Dispatchers.IO].
 */
class ContactRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
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
) : ContactRepository {

    // The entity-to-domain mapping is O(n) CPU work and is therefore explicitly
    // offloaded to Dispatchers.Default (project guideline §2.7). flowOn is placed
    // *before* distinctUntilChanged so that the deduplication check runs downstream
    // on whatever dispatcher the collector uses.
    override val allContacts: Flow<List<Contact>> = contactDao.getAllContacts()
        .map { entities -> entities.map { contactDbMapper.toDomain(it) } }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()

    override val potentialCouples: Flow<List<PotentialCouple>> = contactDao.getPotentialCouples()
        .distinctUntilChanged()

    // Same reasoning as allContacts: O(n) mapping offloaded to Dispatchers.Default.
    override val labelConfigs: Flow<List<LabelConfig>> = labelConfigDao.getAllConfigs()
        .map { entities -> entities.map { labelConfigMapper.toDomain(it) } }
        .flowOn(Dispatchers.Default)
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

    override suspend fun getAllContactsImmediate(): List<Contact> = withContext(Dispatchers.IO) {
        contactDao.getAllContactsImmediate().map { contactDbMapper.toDomain(it) }
    }

    override suspend fun syncContacts() = withContext(Dispatchers.IO) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED
            ) return@withContext

            coroutineScope {
                // 1. Load data from all sources in parallel
                val groupsDeferred = async { systemContactDataSource.fetchContactGroups() }
                val dbContactsDeferred =
                    async { contactDao.getAllContactsImmediate().associateBy { it.lookupKey } }
                val dbConfigsDeferred =
                    async { labelConfigDao.getAllConfigsImmediate().associateBy { it.name } }
                val userDataDeferred =
                    async {
                        contactUserDataDao.getAllUserDataImmediate().associateBy { it.lookupKey }
                    }

                val groups = groupsDeferred.await()
                val systemContacts = systemContactDataSource.fetchContactsFromSystem(groups)
                val dbContacts = dbContactsDeferred.await()
                val dbConfigs = dbConfigsDeferred.await()
                val userDataMap = userDataDeferred.await()

                // 2. Synchronize labels
                syncLabelConfigs(systemContacts, dbConfigs, groups)

                // 3. Diffing: Reconcile contacts (CPU-intensive operation offloaded to Dispatchers.Default)
                val (finalContacts, finalEntities) = withContext(Dispatchers.Default) {
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
        } catch (e: Exception) {
            Log.e("ContactRepository", "Error during contact sync: ${e.message}", e)
        }
    }

    private suspend fun syncLabelConfigs(
        systemContacts: List<Contact>,
        existingConfigs: Map<String, LabelConfigEntity>,
        groups: Map<Long, GroupInfo>
    ) {
        val allLabelsInSystem = systemContacts.asSequence().flatMap { it.labels }.toSet()
        val configsToInsert = mutableListOf<LabelConfigEntity>()

        // Process all system groups
        groups.values.asSequence().distinctBy { it.title }.forEach { group ->
            val existing = existingConfigs[group.title]
            if ((existing == null) || (existing.isSystem != group.isSystem)) {
                configsToInsert.add(
                    LabelConfigEntity(
                        name = group.title,
                        isHiddenFromFilter = existing?.isHiddenFromFilter ?: false,
                        isIgnored = existing?.isIgnored ?: false,
                        isSystem = group.isSystem
                    )
                )
            }
        }

        // Add missing labels (which might not have a system group)
        allLabelsInSystem.forEach { label ->
            if (!existingConfigs.containsKey(label) && groups.values.none { it.title == label }) {
                configsToInsert.add(LabelConfigEntity(name = label))
            }
        }

        if (configsToInsert.isNotEmpty()) {
            labelConfigDao.upsertConfigs(configsToInsert)
        }
    }

    private suspend fun updateGiftIdeas(lookupKey: String, ideas: List<GiftIdea>) {
        withContext(Dispatchers.IO) {
            // Save previous state for potential rollback
            val prevUserData = contactUserDataDao.getUserDataForContact(lookupKey)

            // 1. Write the source of truth first atomically (SettingsDB)
            settingsDatabase.withTransaction {
                contactUserDataDao.upsertUserData(
                    ContactUserData(lookupKey = lookupKey, giftIdeas = ideas)
                )
            }

            // 2. Update cache atomically (AppDB); rollback SettingsDB on failure
            try {
                appDatabase.withTransaction {
                    contactDao.getContactByLookupKey(lookupKey)?.let { contact ->
                        contactDao.upsertContact(contact.copy(giftIdeas = ideas))
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    "ContactRepository",
                    "Failed to update gift idea cache, rolling back: ${e.message}",
                    e
                )
                settingsDatabase.withTransaction {
                    val rollbackData = prevUserData
                        ?: ContactUserData(lookupKey = lookupKey, giftIdeas = emptyList())
                    contactUserDataDao.upsertUserData(rollbackData)
                }
                throw e
            }
        }
        widgetUpdater.updateWidget()
    }

    override suspend fun addGiftIdea(lookupKey: String, newIdea: GiftIdea) =
        withContext(Dispatchers.IO) {
            val contact = contactDao.getContactByLookupKey(lookupKey) ?: return@withContext
            val updatedIdeas = GiftIdea.withNewIdea(contact.giftIdeas, newIdea)
            updateGiftIdeas(lookupKey, updatedIdeas)
        }

    override suspend fun toggleGiftIdea(lookupKey: String, idea: GiftIdea, isChecked: Boolean) =
        withContext(Dispatchers.IO) {
            val contact = contactDao.getContactByLookupKey(lookupKey) ?: return@withContext
            val updatedIdeas = GiftIdea.withToggledIdea(contact.giftIdeas, idea, isChecked)
            updateGiftIdeas(lookupKey, updatedIdeas)
        }

    override suspend fun deleteGiftIdea(lookupKey: String, ideaId: String) =
        withContext(Dispatchers.IO) {
            val contact = contactDao.getContactByLookupKey(lookupKey) ?: return@withContext
            val updatedIdeas = contact.giftIdeas.filter { it.id != ideaId }
            updateGiftIdeas(lookupKey, updatedIdeas)
        }

    override suspend fun updateGiftIdeaText(lookupKey: String, ideaId: String, newText: String) =
        withContext(Dispatchers.IO) {
            val contact = contactDao.getContactByLookupKey(lookupKey) ?: return@withContext
            val updatedIdeas = contact.giftIdeas.map {
                if (it.id == ideaId) it.copy(text = newText) else it
            }
            updateGiftIdeas(lookupKey, updatedIdeas)
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

    override suspend fun exportGiftIdeas(): String = giftIdeaBackupManager.exportGiftIdeas()

    override suspend fun importGiftIdeas(jsonString: String): Int {
        val count = giftIdeaBackupManager.importGiftIdeas(jsonString)
        if (count > 0) {
            syncContacts() // Update cache
        }
        return count
    }

    override suspend fun linkAsCouple(lookupKey1: String, lookupKey2: String) {
        withContext(Dispatchers.IO) {
            // Read previous state for potential rollback
            val prevUserData1 = contactUserDataDao.getUserDataForContact(lookupKey1)
            val prevUserData2 = contactUserDataDao.getUserDataForContact(lookupKey2)

            // 1. Write the source of truth first atomically (SettingsDB)
            settingsDatabase.withTransaction {
                val userData1 = prevUserData1 ?: ContactUserData(lookupKey = lookupKey1)
                val userData2 = prevUserData2 ?: ContactUserData(lookupKey = lookupKey2)
                contactUserDataDao.upsertUserData(userData1.copy(spouseLookupKey = lookupKey2))
                contactUserDataDao.upsertUserData(userData2.copy(spouseLookupKey = lookupKey1))
            }

            // 2. Update cache atomically (AppDB); rollback SettingsDB on failure
            try {
                appDatabase.withTransaction {
                    contactDao.getContactByLookupKey(lookupKey1)?.let { contact ->
                        contactDao.upsertContact(contact.copy(spouseLookupKey = lookupKey2))
                    }
                    contactDao.getContactByLookupKey(lookupKey2)?.let { contact ->
                        contactDao.upsertContact(contact.copy(spouseLookupKey = lookupKey1))
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    "ContactRepository",
                    "Cache update failed for linkAsCouple, rolling back settings: ${e.message}",
                    e
                )
                settingsDatabase.withTransaction {
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
                }
                throw e
            }
        }
        updateWidgetAndSyncCalendar()
    }

    override suspend fun unlinkCouple(lookupKey: String) {
        withContext(Dispatchers.IO) {
            val contact = contactDao.getContactByLookupKey(lookupKey) ?: return@withContext
            val spouseKey = contact.spouseLookupKey ?: return@withContext

            // Read previous state for potential rollback
            val prevUserData1 = contactUserDataDao.getUserDataForContact(lookupKey)
            val prevUserData2 = contactUserDataDao.getUserDataForContact(spouseKey)

            // 1. Update source of truth first atomically (SettingsDB)
            settingsDatabase.withTransaction {
                prevUserData1?.let { contactUserDataDao.upsertUserData(it.copy(spouseLookupKey = null)) }
                prevUserData2?.let { contactUserDataDao.upsertUserData(it.copy(spouseLookupKey = null)) }
            }

            // 2. Update cache atomically (AppDB); rollback SettingsDB on failure
            try {
                appDatabase.withTransaction {
                    contactDao.getContactByLookupKey(lookupKey)?.let {
                        contactDao.upsertContact(it.copy(spouseLookupKey = null))
                    }
                    contactDao.getContactByLookupKey(spouseKey)?.let {
                        contactDao.upsertContact(it.copy(spouseLookupKey = null))
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    "ContactRepository",
                    "Cache update failed for unlinkCouple, rolling back settings: ${e.message}",
                    e
                )
                settingsDatabase.withTransaction {
                    prevUserData1?.let { contactUserDataDao.upsertUserData(it) }
                    prevUserData2?.let { contactUserDataDao.upsertUserData(it) }
                }
                throw e
            }
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
        withContext(Dispatchers.IO) {
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
        withContext(Dispatchers.IO) {
            settingsDatabase.withTransaction {
                val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettingsEntity()
                appSettingsDao.upsertSettings(currentSettings.copy(ignoredCouplePairs = emptyList()))
            }
        }
    }

    override suspend fun updateLabelsEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        settingsDatabase.withTransaction {
            val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettingsEntity()
            appSettingsDao.upsertSettings(currentSettings.copy(labelsEnabled = enabled))
        }
    }
}
