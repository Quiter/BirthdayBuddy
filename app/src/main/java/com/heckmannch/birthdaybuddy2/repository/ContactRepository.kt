package com.heckmannch.birthdaybuddy2.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy2.database.Contact
import com.heckmannch.birthdaybuddy2.database.ContactDao
import com.heckmannch.birthdaybuddy2.database.LabelConfig
import com.heckmannch.birthdaybuddy2.database.LabelConfigDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class GroupInfo(val title: String, val isSystem: Boolean)

@Singleton
class ContactRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val contactDao: ContactDao,
    private val labelConfigDao: LabelConfigDao,
) {

    val allContacts: Flow<List<Contact>> = contactDao.getAllContacts()
    val labelConfigs: Flow<List<LabelConfig>> = labelConfigDao.getAllConfigs()

    private val dateFormats = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyyMMdd"),
    )

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
                val groupsDeferred = async { fetchContactGroups() }
                val dbContactsDeferred = async { contactDao.getAllContactsImmediate().associateBy { it.lookupKey } }
                val dbConfigsDeferred = async { labelConfigDao.getAllConfigsImmediate().associateBy { it.name } }

                val groups = groupsDeferred.await()
                val systemContacts = fetchBirthdaysFromSystem(groups)
                val dbContacts = dbContactsDeferred.await()
                val dbConfigs = dbConfigsDeferred.await()

                // 2. Labels synchronisieren
                syncLabelConfigs(systemContacts, dbConfigs, groups)

                // 3. Diffing: Kontakte abgleichen
                val finalContacts = systemContacts.map { systemContact ->
                    dbContacts[systemContact.lookupKey]?.let { existing ->
                        // Update: localId und Geschenkideen erhalten
                        systemContact.copy(
                            localId = existing.localId,
                            giftIdeas = existing.giftIdeas
                        )
                    } ?: systemContact
                }

                // 4. Batch Update via Transaction
                contactDao.refreshContacts(finalContacts)
            }
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
            if (existing == null || existing.isSystem != group.isSystem) {
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
            labelConfigDao.insertConfigs(configsToInsert)
        }
    }

    suspend fun updateGiftIdeas(lookupKey: String, ideas: String) {
        withContext(Dispatchers.IO) {
            contactDao.getContactByLookupKey(lookupKey)?.let { contact ->
                contactDao.insertContact(contact.copy(giftIdeas = ideas))
            }
        }
    }

    suspend fun updateLabelConfig(config: LabelConfig) {
        labelConfigDao.insertConfig(config)
    }

    suspend fun exportGiftIdeas(): String = withContext(Dispatchers.IO) {
        val contacts = contactDao.getAllContactsImmediate().filter { !it.giftIdeas.isNullOrBlank() }
        val root = JSONArray()
        contacts.forEach { contact ->
            val obj = JSONObject().apply {
                put("lookupKey", contact.lookupKey)
                put("fullName", contact.fullName)
                put("giftIdeas", contact.giftIdeas)
            }
            root.put(obj)
        }
        root.toString(2)
    }

    suspend fun importGiftIdeas(jsonString: String): Int = withContext(Dispatchers.IO) {
        try {
            val root = JSONArray(jsonString)
            if (root.length() == 0) return@withContext 0

            val dbContacts = contactDao.getAllContactsImmediate()
            val contactsByLookup = dbContacts.associateBy { it.lookupKey }
            val contactsByName = dbContacts.associateBy { it.fullName }
            var count = 0

            for (i in 0 until root.length()) {
                val obj = root.getJSONObject(i)
                val lookupKey = obj.optString("lookupKey")
                val giftIdeas = obj.optString("giftIdeas")
                val fullName = obj.optString("fullName")

                if (giftIdeas.isNullOrBlank()) continue

                // Match via LookupKey (Best) oder Name (Fallback)
                val target = contactsByLookup[lookupKey] ?: contactsByName[fullName]

                if (target != null) {
                    contactDao.insertContact(target.copy(giftIdeas = giftIdeas))
                    count++
                }
            }
            count
        } catch (e: Exception) {
            Log.e("ContactRepository", "Import fehlgeschlagen", e)
            -1
        }
    }

    private suspend fun fetchBirthdaysFromSystem(groups: Map<Long, GroupInfo>): List<Contact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Event.START_DATE,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.PHOTO_URI,
        )

        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ?"
        val selectionArgs = arrayOf(
            ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString()
        )

        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val lookupIdx = cursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val dateIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE)
            val photoIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

            val contactIds = mutableSetOf<String>()

            while (cursor.moveToNext()) {
                val contactId = cursor.getString(idIdx) ?: continue
                val birthday = parseDate(cursor.getString(dateIdx)) ?: continue
                
                contactIds.add(contactId)
                contacts.add(
                    Contact(
                        contactId = contactId,
                        lookupKey = cursor.getString(lookupIdx) ?: "",
                        fullName = cursor.getString(nameIdx) ?: "Unbekannt",
                        birthday = birthday,
                        imageUri = cursor.getString(photoIdx)
                    )
                )
            }

            if (contacts.isEmpty()) return@withContext emptyList()
            
            val labelsMap = fetchLabelsForContacts(contactIds, groups)
            return@withContext contacts.map { it.copy(labels = labelsMap[it.contactId] ?: emptyList()) }
        }
        emptyList()
    }

    private suspend fun fetchLabelsForContacts(
        contactIds: Set<String>,
        groups: Map<Long, GroupInfo>
    ): Map<String, List<String>> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, MutableSet<String>>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID,
            ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID
        )

        contactIds.chunked(900).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.CONTACT_ID} IN ($placeholders)"
            val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE, *chunk.toTypedArray())

            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID)
                val groupRowIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIdx)
                    val groupId = cursor.getLong(groupRowIdIdx)
                    groups[groupId]?.let { groupInfo ->
                        val lowerTitle = groupInfo.title.lowercase()
                        val isRedundant = lowerTitle in REDUNDANT_LABELS
                        if (!isRedundant) {
                            result.getOrPut(contactId) { mutableSetOf() }.add(groupInfo.title)
                        }
                    }
                }
            }
        }
        result.mapValues { it.value.toList() }
    }

    private suspend fun fetchContactGroups(): Map<Long, GroupInfo> = withContext(Dispatchers.IO) {
        val groups = mutableMapOf<Long, GroupInfo>()
        val projection = arrayOf(
            ContactsContract.Groups._ID,
            ContactsContract.Groups.TITLE,
            ContactsContract.Groups.SYSTEM_ID
        )

        context.contentResolver.query(
            ContactsContract.Groups.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Groups._ID)
            val titleIdx = cursor.getColumnIndex(ContactsContract.Groups.TITLE)
            val systemIdIdx = cursor.getColumnIndex(ContactsContract.Groups.SYSTEM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val title = cursor.getString(titleIdx)
                val systemId = cursor.getString(systemIdIdx)
                if (!title.isNullOrBlank()) {
                    groups[id] = GroupInfo(title, systemId != null)
                }
            }
        }
        groups
    }

    private fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr == null) return null
        return try {
            if (dateStr.startsWith("--")) {
                LocalDate.parse("1900-${dateStr.substring(2)}")
            } else {
                dateFormats.firstNotNullOfOrNull { format ->
                    runCatching { LocalDate.parse(dateStr, format) }.getOrNull()
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val REDUNDANT_LABELS = setOf("my contacts", "contacts", "ich", "me")
    }
}
