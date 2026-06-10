package com.heckmannch.birthdaybuddy.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy.data.local.AppSettings
import com.heckmannch.birthdaybuddy.data.local.AppSettingsDao
import com.heckmannch.birthdaybuddy.data.local.Contact
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.ContactUserData
import com.heckmannch.birthdaybuddy.data.local.ContactUserDataDao
import com.heckmannch.birthdaybuddy.data.local.LabelConfig
import com.heckmannch.birthdaybuddy.data.local.LabelConfigDao
import com.heckmannch.birthdaybuddy.ui.model.GiftIdea
import com.heckmannch.birthdaybuddy.util.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val contactDao: ContactDao,
    private val labelConfigDao: LabelConfigDao,
    private val appSettingsDao: AppSettingsDao,
    private val contactUserDataDao: ContactUserDataDao,
    private val systemContactDataSource: SystemContactDataSource,
    private val giftIdeaBackupManager: GiftIdeaBackupManager,
    private val calendarSyncRepository: CalendarSyncRepository,
    private val widgetUpdater: WidgetUpdater,
) {

    val allContacts: Flow<List<Contact>> = contactDao.getAllContacts()
    val labelConfigs: Flow<List<LabelConfig>> = labelConfigDao.getAllConfigs()
    val otherEventsEnabled: Flow<Boolean> = appSettingsDao.getSettings()
        .map { it?.otherEventsEnabled ?: false }
        .distinctUntilChanged()

    val ignoredCouplePairs: Flow<List<String>> = appSettingsDao.getSettings()
        .map { it?.ignoredCouplePairs ?: emptyList() }
        .distinctUntilChanged()

    suspend fun getAllContactsImmediate(): List<Contact> = withContext(Dispatchers.IO) {
        contactDao.getAllContactsImmediate()
    }

    /**
     * Intelligenter Sync: Vergleicht System-Kontakte mit DB, erhält lokale Daten (Geschenkideen)
     * und führt nur notwendige Änderungen durch (Diffing).
     */
    suspend fun syncContacts() = withContext(Dispatchers.IO) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED
            ) return@withContext

            coroutineScope {
                // 1. Daten aus allen Quellen parallel laden
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

                // 2. Labels synchronisieren
                syncLabelConfigs(systemContacts, dbConfigs, groups)

                // 3. Diffing: Kontakte abgleichen
                val finalContacts = systemContacts.map { systemContact ->
                    val lookupKey = systemContact.lookupKey
                    val existing = dbContacts[lookupKey]
                    val userData = userDataMap[lookupKey]

                    systemContact.copy(
                        localId = existing?.localId ?: 0,
                        giftIdeas = userData?.giftIdeas ?: existing?.giftIdeas ?: emptyList(),
                        spouseLookupKey = userData?.spouseLookupKey
                    )
                }

                // 4. Batch Update via Transaction
                contactDao.refreshContacts(finalContacts)

                // 5. Kalender synchronisieren, falls aktiviert
                val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettings()
                if (currentSettings.calendarSyncEnabled) {
                    calendarSyncRepository.syncBirthdays(finalContacts)
                }

                // 6. Zeitstempel aktualisieren
                appSettingsDao.upsertSettings(currentSettings.copy(lastSyncTimestamp = System.currentTimeMillis()))
            }
            widgetUpdater.updateWidget()
        } catch (e: Exception) {
            Log.e("ContactRepository", "Fehler beim Sync: ${e.message}", e)
        }
    }

    private suspend fun syncLabelConfigs(
        systemContacts: List<Contact>,
        existingConfigs: Map<String, LabelConfig>,
        groups: Map<Long, GroupInfo>
    ) {
        val allLabelsInSystem = systemContacts.asSequence().flatMap { it.labels }.toSet()
        val configsToInsert = mutableListOf<LabelConfig>()

        // Alle System-Gruppen verarbeiten
        groups.values.asSequence().distinctBy { it.title }.forEach { group ->
            val existing = existingConfigs[group.title]
            if ((existing == null) || (existing.isSystem != group.isSystem)) {
                configsToInsert.add(
                    LabelConfig(
                        name = group.title,
                        isHiddenFromFilter = existing?.isHiddenFromFilter ?: false,
                        isIgnored = existing?.isIgnored ?: false,
                        isSystem = group.isSystem
                    )
                )
            }
        }

        // Fehlende Labels (die vielleicht keine System-Gruppe haben) hinzufügen
        allLabelsInSystem.forEach { label ->
            if (!existingConfigs.containsKey(label) && groups.values.none { it.title == label }) {
                configsToInsert.add(LabelConfig(name = label))
            }
        }

        if (configsToInsert.isNotEmpty()) {
            labelConfigDao.upsertConfigs(configsToInsert)
        }
    }

    private suspend fun updateGiftIdeas(lookupKey: String, ideas: List<GiftIdea>) {
        withContext(Dispatchers.IO) {
            // 1. In der persistenten UserData-Tabelle speichern (für Backup)
            contactUserDataDao.upsertUserData(
                ContactUserData(lookupKey = lookupKey, giftIdeas = ideas)
            )

            // 2. Im Cache aktualisieren (für sofortige UI-Anzeige)
            contactDao.getContactByLookupKey(lookupKey)?.let { contact ->
                contactDao.upsertContact(contact.copy(giftIdeas = ideas))
            }
        }
        widgetUpdater.updateWidget()
    }

    suspend fun addGiftIdea(lookupKey: String, newIdea: GiftIdea) = withContext(Dispatchers.IO) {
        val contact = contactDao.getContactByLookupKey(lookupKey) ?: return@withContext
        val updatedIdeas = GiftIdea.withNewIdea(contact.giftIdeas, newIdea)
        updateGiftIdeas(lookupKey, updatedIdeas)
    }

    suspend fun toggleGiftIdea(lookupKey: String, idea: GiftIdea, isChecked: Boolean) =
        withContext(Dispatchers.IO) {
            val contact = contactDao.getContactByLookupKey(lookupKey) ?: return@withContext
            val updatedIdeas = GiftIdea.withToggledIdea(contact.giftIdeas, idea, isChecked)
            updateGiftIdeas(lookupKey, updatedIdeas)
        }

    suspend fun deleteGiftIdea(lookupKey: String, ideaId: String) = withContext(Dispatchers.IO) {
        val contact = contactDao.getContactByLookupKey(lookupKey) ?: return@withContext
        val updatedIdeas = contact.giftIdeas.filter { it.id != ideaId }
        updateGiftIdeas(lookupKey, updatedIdeas)
    }

    suspend fun updateGiftIdeaText(lookupKey: String, ideaId: String, newText: String) =
        withContext(Dispatchers.IO) {
            val contact = contactDao.getContactByLookupKey(lookupKey) ?: return@withContext
            val updatedIdeas = contact.giftIdeas.map {
                if (it.id == ideaId) it.copy(text = newText) else it
            }
            updateGiftIdeas(lookupKey, updatedIdeas)
        }

    suspend fun updateLabelConfig(config: LabelConfig) {
        labelConfigDao.upsertConfig(config)
        widgetUpdater.updateWidget()
    }

    suspend fun updateContactBirthday(contactId: String, birthday: java.time.LocalDate): Boolean {
        val success = systemContactDataSource.updateContactBirthday(contactId, birthday)
        if (success) {
            syncContacts()
        }
        return success
    }

    suspend fun exportGiftIdeas(): String = giftIdeaBackupManager.exportGiftIdeas()

    suspend fun importGiftIdeas(jsonString: String): Int {
        val count = giftIdeaBackupManager.importGiftIdeas(jsonString)
        if (count > 0) {
            syncContacts() // Cache aktualisieren
        }
        return count
    }

    suspend fun linkAsCouple(lookupKey1: String, lookupKey2: String) {
        withContext(Dispatchers.IO) {
            val userData1 = contactUserDataDao.getUserDataForContact(lookupKey1) ?: ContactUserData(
                lookupKey = lookupKey1
            )
            val userData2 = contactUserDataDao.getUserDataForContact(lookupKey2) ?: ContactUserData(
                lookupKey = lookupKey2
            )

            contactUserDataDao.upsertUserData(userData1.copy(spouseLookupKey = lookupKey2))
            contactUserDataDao.upsertUserData(userData2.copy(spouseLookupKey = lookupKey1))

            contactDao.getContactByLookupKey(lookupKey1)?.let { contact ->
                contactDao.upsertContact(contact.copy(spouseLookupKey = lookupKey2))
            }
            contactDao.getContactByLookupKey(lookupKey2)?.let { contact ->
                contactDao.upsertContact(contact.copy(spouseLookupKey = lookupKey1))
            }
        }
        updateWidgetAndSyncCalendar()
    }

    suspend fun unlinkCouple(lookupKey: String) {
        withContext(Dispatchers.IO) {
            val contact = contactDao.getContactByLookupKey(lookupKey) ?: return@withContext
            val spouseKey = contact.spouseLookupKey ?: return@withContext

            contactUserDataDao.getUserDataForContact(lookupKey)?.let {
                contactUserDataDao.upsertUserData(it.copy(spouseLookupKey = null))
            }
            contactUserDataDao.getUserDataForContact(spouseKey)?.let {
                contactUserDataDao.upsertUserData(it.copy(spouseLookupKey = null))
            }

            contactDao.getContactByLookupKey(lookupKey)?.let {
                contactDao.upsertContact(it.copy(spouseLookupKey = null))
            }
            contactDao.getContactByLookupKey(spouseKey)?.let {
                contactDao.upsertContact(it.copy(spouseLookupKey = null))
            }
        }
        updateWidgetAndSyncCalendar()
    }

    private suspend fun updateWidgetAndSyncCalendar() {
        widgetUpdater.updateWidget()
        val allContactsImmediate = getAllContactsImmediate()
        val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettings()
        if (currentSettings.calendarSyncEnabled) {
            calendarSyncRepository.syncBirthdays(allContactsImmediate)
        }
    }

    suspend fun ignoreCoupleSuggestion(lookupKey1: String, lookupKey2: String) {
        withContext(Dispatchers.IO) {
            val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettings()
            val pairKey =
                if (lookupKey1 < lookupKey2) "$lookupKey1:$lookupKey2" else "$lookupKey2:$lookupKey1"
            if (!currentSettings.ignoredCouplePairs.contains(pairKey)) {
                val updatedList = currentSettings.ignoredCouplePairs + pairKey
                appSettingsDao.upsertSettings(currentSettings.copy(ignoredCouplePairs = updatedList))
            }
        }
    }

    suspend fun clearIgnoredCouplePairs() {
        withContext(Dispatchers.IO) {
            val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettings()
            appSettingsDao.upsertSettings(currentSettings.copy(ignoredCouplePairs = emptyList()))
        }
    }
}
