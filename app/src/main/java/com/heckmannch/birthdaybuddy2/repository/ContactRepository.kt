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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
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
    suspend fun syncContacts() {
        withContext(Dispatchers.IO) {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) 
                    != PackageManager.PERMISSION_GRANTED) return@withContext

                // 1. Daten aus beiden Quellen holen
                val systemContacts = fetchBirthdaysFromSystem()
                val dbContacts = contactDao.getAllContactsImmediate().associateBy { it.lookupKey }
                val dbConfigs = labelConfigDao.getAllConfigsImmediate().associateBy { it.name }
                
                // 2. Labels synchronisieren (isSystem Status und neue Labels)
                syncLabelConfigs(systemContacts, dbConfigs)

                // 3. Diffing: Kontakte abgleichen
                val finalContacts = systemContacts.map { systemContact ->
                    val existing = dbContacts[systemContact.lookupKey]
                    if (existing != null) {
                        // Update: Bestehende localId und Geschenkideen erhalten
                        systemContact.copy(
                            localId = existing.localId,
                            giftIdeas = existing.giftIdeas
                        )
                    } else {
                        // Neu: Einfach übernehmen (localId = 0 sorgt für Insert)
                        systemContact
                    }
                }

                // 4. Batch Update (Room Transaction via refreshContacts löscht alles, was nicht mehr da ist)
                // Da wir die localIds der bestehenden Kontakte erhalten haben, 
                // bleibt die DB-Konsistenz gewahrt.
                contactDao.refreshContacts(finalContacts)

            } catch (e: Exception) {
                Log.e("ContactRepository", "Fehler beim Sync: ${e.message}", e)
            }
        }
    }

    private suspend fun syncLabelConfigs(
        systemContacts: List<Contact>,
        existingConfigs: Map<String, LabelConfig>
    ) {
        val groups = fetchContactGroups()
        val allLabelsInSystem = systemContacts.asSequence().flatMap { it.labels }.toSet()
        val configsToInsert = mutableListOf<LabelConfig>()
        
        // Alle System-Gruppen verarbeiten
        groups.values.distinctBy { it.title }.forEach { group ->
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

    private suspend fun fetchBirthdaysFromSystem(): List<Contact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<Contact>()
        val groups = fetchContactGroups()
        
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
                val lookupKey = cursor.getString(lookupIdx) ?: ""
                val name = cursor.getString(nameIdx) ?: "Unbekannt"
                val dateStr = cursor.getString(dateIdx)
                val photoUri = cursor.getString(photoIdx)

                val birthday = parseDate(dateStr)
                if (birthday != null) {
                    contactIds.add(contactId)
                    contacts.add(
                        Contact(
                            contactId = contactId,
                            lookupKey = lookupKey,
                            fullName = name,
                            birthday = birthday,
                            imageUri = photoUri
                        )
                    )
                }
            }
            
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
        if (contactIds.isEmpty()) return@withContext emptyMap()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID,
            ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID
        )

        // Verhindern von SQL-Injection und Handling großer Listen via Chunking
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
                        // Redundante System-Labels wie "My Contacts" filtern
                        val lowerTitle = groupInfo.title.lowercase()
                        val isRedundant = lowerTitle == "my contacts" || lowerTitle == "contacts" || lowerTitle == "ich" || lowerTitle == "me"
                        
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
                val id = idIdx.takeIf { it != -1 }?.let { cursor.getLong(it) } ?: continue
                val title = titleIdx.takeIf { it != -1 }?.let { cursor.getString(it) }
                val systemId = systemIdIdx.takeIf { it != -1 }?.let { cursor.getString(it) }
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
                var result: LocalDate? = null
                for (format in dateFormats) {
                    try {
                        result = LocalDate.parse(dateStr, format)
                        break
                    } catch (_: Exception) {}
                }
                result
            }
        } catch (_: Exception) {
            null
        }
    }
}
