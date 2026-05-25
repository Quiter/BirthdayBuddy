package com.heckmannch.birthdaybuddy.data.repository

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import com.heckmannch.birthdaybuddy.data.local.Contact
import com.heckmannch.birthdaybuddy.util.NO_YEAR_MARKER
import com.heckmannch.birthdaybuddy.util.hasYear
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class GroupInfo(val title: String, val isSystem: Boolean)

@Singleton
class SystemContactDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dateFormats = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyyMMdd"),
    )

    private val redundantLabels = setOf("my contacts", "contacts", "ich", "me")

    /**
     * System-Gruppen, die nicht dazu führen sollen, dass die Label-Bar angezeigt wird.
     * "Starred in Android" ist oft kein offizielles System-ID-Feld, verhält sich aber so.
     */
    private val pureSystemGroups =
        setOf("starred in android", "my contacts", "all contacts", "contacts")


    suspend fun updateContactBirthday(contactId: String, birthday: LocalDate): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val dateStr = if (birthday.hasYear) {
                    birthday.format(DateTimeFormatter.ISO_LOCAL_DATE)
                } else {
                    "--%02d-%02d".format(birthday.monthValue, birthday.dayOfMonth)
                }

                val ops = arrayListOf<ContentProviderOperation>()

                // 1. Prüfen, ob bereits ein Geburtstag existiert
                val projection = arrayOf(ContactsContract.Data._ID)
                val selection =
                    "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ?"
                val selectionArgs = arrayOf(
                    contactId,
                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString(),
                )

                var existingDataId: Long? = null
                context.contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        existingDataId = cursor.getLong(0)
                    }
                }

                if (existingDataId != null) {
                    // Update
                    ops.add(
                        ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection(
                                "${ContactsContract.Data._ID} = ?",
                                arrayOf(existingDataId.toString())
                            )
                            .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, dateStr)
                            .build()
                    )
                } else {
                    // Insert: Wir brauchen die RawContactId
                    val rawContactId = getRawContactId(contactId) ?: return@withContext false
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.Event.TYPE,
                                ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY
                            )
                            .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, dateStr)
                            .build()
                    )
                }

                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                true
            } catch (e: Exception) {
                Log.e("SystemContactDataSource", "Failed to update contact birthday", e)
                false
            }
        }

    private fun getRawContactId(contactId: String): Long? {
        val projection = arrayOf(
            ContactsContract.RawContacts._ID,
            ContactsContract.RawContacts.ACCOUNT_TYPE,
            ContactsContract.RawContacts.DELETED
        )
        val selection = "${ContactsContract.RawContacts.CONTACT_ID} = ? AND ${ContactsContract.RawContacts.DELETED} = 0"
        val selectionArgs = arrayOf(contactId)
        
        val rawContacts = mutableListOf<Pair<Long, String?>>()
        
        try {
            context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.RawContacts._ID)
                val typeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
                
                if (idIdx != -1 && typeIdx != -1) {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIdx)
                        val accountType = cursor.getString(typeIdx)
                        rawContacts.add(Pair(id, accountType))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SystemContactDataSource", "Error querying raw contacts", e)
        }
        
        if (rawContacts.isEmpty()) return null
        
        val readOnlyApps = setOf(
            "com.whatsapp",
            "org.thoughtcrime.securesms",
            "org.telegram.messenger",
            "com.facebook.messenger",
            "com.skype.raider",
            "com.google.android.apps.tachyon",
            "com.viber.voip"
        )
        
        val bestContact = rawContacts.firstOrNull { (_, type) ->
            type == null || !readOnlyApps.contains(type.lowercase())
        } ?: rawContacts.firstOrNull()
        
        return bestContact?.first
    }

    suspend fun fetchContactGroups(): Map<Long, GroupInfo> = withContext(Dispatchers.IO) {
        val groups = mutableMapOf<Long, GroupInfo>()
        val projection = arrayOf(
            ContactsContract.Groups._ID,
            ContactsContract.Groups.TITLE,
        )

        context.contentResolver.query(
            ContactsContract.Groups.CONTENT_URI,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Groups._ID)
            val titleIdx = cursor.getColumnIndex(ContactsContract.Groups.TITLE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val title = cursor.getString(titleIdx)

                if (!title.isNullOrBlank()) {
                    val isSystem = when (title.lowercase()) {
                        in pureSystemGroups -> true
                        else -> false
                    }
                    groups[id] = GroupInfo(title, isSystem)
                }
            }
        }
        groups
    }

    suspend fun fetchContactsFromSystem(groups: Map<Long, GroupInfo>): List<Contact> =
        withContext(Dispatchers.IO) {
            val contactsMap = mutableMapOf<String, Contact>()

            // 1. Alle Kontakte mit Namen laden
            val baseProjection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
            )

            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                baseProjection,
                null,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val lookupIdx = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
                val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val photoIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIdx) ?: continue
                    contactsMap[contactId] = Contact(
                        contactId = contactId,
                        lookupKey = cursor.getString(lookupIdx) ?: "",
                        fullName = cursor.getString(nameIdx) ?: "Unbekannt",
                        birthday = null,
                        imageUri = cursor.getString(photoIdx)
                    )
                }
            }

            // 2. Geburtstage laden und zuordnen
            val birthdayProjection = arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.Event.START_DATE,
            )
            val birthdaySelection =
                "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ?"
            val birthdayArgs = arrayOf(
                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString()
            )

            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                birthdayProjection,
                birthdaySelection,
                birthdayArgs,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                val dateIdx =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIdx) ?: continue
                    val birthday = parseDate(cursor.getString(dateIdx))
                    if (birthday != null) {
                        contactsMap[contactId]?.let {
                            contactsMap[contactId] = it.copy(birthday = birthday)
                        }
                    }
                }
            }

            if (contactsMap.isEmpty()) return@withContext emptyList()

            val contactIds = contactsMap.keys
            val labelsMap = fetchLabelsForContacts(contactIds, groups)
            val phonesMap = fetchPhoneNumbersForContacts(contactIds)
            val messengerMap = fetchMessengerAvailabilityForContacts(contactIds)

            return@withContext contactsMap.values.map {
                it.copy(
                    labels = labelsMap[it.contactId] ?: emptyList(),
                    phoneNumber = phonesMap[it.contactId],
                    hasWhatsApp = messengerMap[it.contactId]?.first ?: false,
                    hasSignal = messengerMap[it.contactId]?.second ?: false
                )
            }
        }

    private suspend fun fetchMessengerAvailabilityForContacts(contactIds: Set<String>): Map<String, Pair<Boolean, Boolean>> =
        withContext(Dispatchers.IO) {
            val result =
                mutableMapOf<String, Pair<Boolean, Boolean>>() // contactId -> (hasWhatsApp, hasSignal)

            val whatsappMimeType = "vnd.android.cursor.item/vnd.com.whatsapp.profile"
            val signalMimeType = "vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.contact"

            val projection = arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.MIMETYPE
            )

            contactIds.chunked(900).forEach { chunk ->
                val placeholders = chunk.joinToString(",") { "?" }
                val selection =
                    "${ContactsContract.Data.MIMETYPE} IN (?, ?) AND ${ContactsContract.Data.CONTACT_ID} IN ($placeholders)"
                val selectionArgs = arrayOf(whatsappMimeType, signalMimeType, *chunk.toTypedArray())

                context.contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                    val mimeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)

                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idIdx)
                        val mime = cursor.getString(mimeIdx)

                        val current = result.getOrDefault(id, Pair(false, false))
                        if (mime == whatsappMimeType) {
                            result[id] = current.copy(first = true)
                        } else if (mime == signalMimeType) {
                            result[id] = current.copy(second = true)
                        }
                    }
                }
            }
            result
        }

    private suspend fun fetchPhoneNumbersForContacts(contactIds: Set<String>): Map<String, String> =
        withContext(Dispatchers.IO) {
            val result = mutableMapOf<String, String>()
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY
            )

            contactIds.chunked(900).forEach { chunk ->
                val placeholders = chunk.joinToString(",") { "?" }
                val selection =
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} IN ($placeholders)"
                val selectionArgs = chunk.toTypedArray()

                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC"
                )?.use { cursor ->
                    val idIdx =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val numberIdx =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    while (cursor.moveToNext()) {
                        val contactId = cursor.getString(idIdx)
                        val number = cursor.getString(numberIdx)
                        // Wir nehmen die erste (primäre) Nummer
                        if (!result.containsKey(contactId)) {
                            result[contactId] = number
                        }
                    }
                }
            }
            result
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
            val selection =
                "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.CONTACT_ID} IN ($placeholders)"
            val selectionArgs = arrayOf(
                ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
                *chunk.toTypedArray()
            )

            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idIdx =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID)
                val groupRowIdIdx =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIdx)
                    val groupId = cursor.getLong(groupRowIdIdx)
                    groups[groupId]?.let { groupInfo ->
                        val lowerTitle = groupInfo.title.lowercase()
                        val isRedundant = lowerTitle in redundantLabels
                        if (!isRedundant) {
                            result.getOrPut(contactId) { mutableSetOf() }.add(groupInfo.title)
                        }
                    }
                }
            }
        }
        result.mapValues { it.value.toList() }
    }

    private fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr == null) return null
        return try {
            if (dateStr.startsWith("--")) {
                LocalDate.parse("$NO_YEAR_MARKER-${dateStr.substring(2)}")
            } else {
                dateFormats.firstNotNullOfOrNull { format ->
                    runCatching { LocalDate.parse(dateStr, format) }.getOrNull()
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
