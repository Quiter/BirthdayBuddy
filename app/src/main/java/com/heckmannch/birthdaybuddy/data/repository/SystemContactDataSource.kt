package com.heckmannch.birthdaybuddy.data.repository

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.domain.util.NO_YEAR_MARKER
import com.heckmannch.birthdaybuddy.util.hasYear
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class GroupInfo(val title: String, val isSystem: Boolean)

@Singleton
class SystemContactDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val dateFormats = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyyMMdd"),
    )

    companion object {
        private const val TAG = "SystemContactDataSource"

        val redundantLabels: Set<String> = ContactLabels.REDUNDANT_LABELS
    }

    /**
     * System-Gruppen, die nicht dazu führen sollen, dass die Label-Bar angezeigt wird.
     */
    private val pureSystemGroups =
        setOf("my contacts", "all contacts", "contacts")


    /**
     * Updates or inserts the birthday event for the given [contactId] in the system contacts.
     *
     * Dates without a birth year are formatted as `--MM-DD` using [Locale.ROOT] so that
     * ASCII digits are guaranteed regardless of the device's default locale.
     */
    suspend fun updateContactBirthday(contactId: String, birthday: LocalDate): Boolean =
        withContext(ioDispatcher) {
            try {
                val dateStr = if (birthday.hasYear) {
                    birthday.format(DateTimeFormatter.ISO_LOCAL_DATE)
                } else {
                    String.format(Locale.ROOT, "--%02d-%02d", birthday.monthValue, birthday.dayOfMonth)
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
            } catch (e: CancellationException) {
                throw e
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
        val selection =
            "${ContactsContract.RawContacts.CONTACT_ID} = ? AND ${ContactsContract.RawContacts.DELETED} = 0"
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
            type == null || !readOnlyApps.contains(type.lowercase(Locale.ROOT))
        } ?: rawContacts.firstOrNull()

        return bestContact?.first
    }

    suspend fun fetchContactGroups(): Map<Long, GroupInfo> = withContext(ioDispatcher) {
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
                    val isSystem = when (title.lowercase(Locale.ROOT)) {
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
        withContext(ioDispatcher) {
            val contactsMap = mutableMapOf<String, Contact>()

            // 1. Alle Kontakte mit Namen laden
            val baseProjection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                ContactsContract.Contacts.STARRED,
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
                val starredIdx = cursor.getColumnIndex(ContactsContract.Contacts.STARRED)

                if (idIdx != -1 && lookupIdx != -1) {
                    while (cursor.moveToNext()) {
                        val contactId = cursor.getString(idIdx) ?: continue
                        val lookupKey = cursor.getString(lookupIdx)
                        if (lookupKey.isNullOrBlank()) {
                            Log.w(
                                TAG,
                                "Skipping contact with empty or blank lookupKey (contactId=$contactId)"
                            )
                            continue
                        }
                        val isStarred = starredIdx != -1 && cursor.getInt(starredIdx) == 1
                        contactsMap[contactId] = Contact(
                            contactId = contactId,
                            lookupKey = lookupKey,
                            fullName = (if (nameIdx != -1) cursor.getString(nameIdx) else null)
                                ?: context.getString(R.string.contact_unknown),
                            birthday = null,
                            imageUri = if (photoIdx != -1) cursor.getString(photoIdx) else null,
                            isFavorite = isStarred,
                        )
                    }
                } else if (idIdx != -1) {
                    Log.w(TAG, "LOOKUP_KEY column missing in contacts cursor; skipping contacts")
                }
            }

            // 2. Ereignisse (Geburtstag, Hochzeitstag, Namenstag, Sonstiges & benutzerdefinierte Datumsfelder) laden
            val eventProjection = arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.Event.START_DATE,
                ContactsContract.CommonDataKinds.Event.TYPE,
                ContactsContract.CommonDataKinds.Event.LABEL,
            )
            val eventSelection = "${ContactsContract.Data.MIMETYPE} = ?"
            val eventArgs = arrayOf(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)

            val anniversaryLabels = setOf("hochzeitstag", "anniversary")
            val nameDayLabels = setOf("namenstag", "name day", "name-day")

            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                eventProjection,
                eventSelection,
                eventArgs,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                val dateIdx =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE)
                val typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.TYPE)
                val labelIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.LABEL)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIdx) ?: continue
                    val dateStr = cursor.getString(dateIdx)
                    val date = parseDate(dateStr) ?: continue
                    val type = cursor.getInt(typeIdx)
                    val rawLabel = cursor.getString(labelIdx)?.trim()
                    val normalizedLabel = rawLabel?.lowercase(Locale.ROOT)

                    contactsMap[contactId]?.let { contact ->
                        when {
                            type == ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY -> {
                                contactsMap[contactId] = contact.copy(birthday = date)
                            }

                            type == ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY ||
                                    (type == ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM && normalizedLabel in anniversaryLabels) -> {
                                contactsMap[contactId] = contact.copy(anniversary = date)
                            }

                            type == ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM && normalizedLabel in nameDayLabels -> {
                                contactsMap[contactId] = contact.copy(nameDay = date)
                            }

                            type == ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM -> {
                                val eventKey = if (!rawLabel.isNullOrBlank()) {
                                    ContactLabels.customEventLabel(rawLabel)
                                } else {
                                    ContactLabels.LABEL_OTHER_EVENT
                                }
                                contactsMap[contactId] = contact.copy(
                                    customEvents = contact.customEvents + (eventKey to date)
                                )
                            }

                            type == ContactsContract.CommonDataKinds.Event.TYPE_OTHER -> {
                                contactsMap[contactId] = contact.copy(
                                    customEvents = contact.customEvents + (ContactLabels.LABEL_OTHER_EVENT to date)
                                )
                            }
                        }
                    }
                }
            }

            if (contactsMap.isEmpty()) return@withContext emptyList()

            val contactIds = contactsMap.keys
            val (labelsMap, phonesMap, messengerMap) = coroutineScope {
                val labelsDeferred = async { fetchLabelsForContacts(contactIds, groups) }
                val phonesDeferred = async { fetchPhoneNumbersForContacts(contactIds) }
                val messengerDeferred = async { fetchMessengerAvailabilityForContacts(contactIds) }
                Triple(labelsDeferred.await(), phonesDeferred.await(), messengerDeferred.await())
            }

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
        withContext(ioDispatcher) {
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
                    if (idIdx == -1 || mimeIdx == -1) return@forEach

                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idIdx) ?: continue
                        val mime = cursor.getString(mimeIdx) ?: continue

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
        withContext(ioDispatcher) {
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
                    if (idIdx == -1 || numberIdx == -1) return@forEach

                    while (cursor.moveToNext()) {
                        val contactId = cursor.getString(idIdx) ?: continue
                        val number = cursor.getString(numberIdx) ?: continue
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
    ): Map<String, List<String>> = withContext(ioDispatcher) {
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
                if (idIdx == -1 || groupRowIdIdx == -1) return@forEach

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIdx) ?: continue
                    val groupId = cursor.getLong(groupRowIdIdx)
                    groups[groupId]?.let { groupInfo ->
                        val lowerTitle = groupInfo.title.lowercase(Locale.ROOT)
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

    internal fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr == null) return null
        val trimmed = dateStr.trim()
        return try {
            if (trimmed.startsWith("--")) {
                val clean = trimmed.removePrefix("--")
                val parts = clean.split("-")
                if (parts.size == 2) {
                    val month = parts[0].toIntOrNull()
                    val day = parts[1].toIntOrNull()
                    if (month != null && day != null) {
                        LocalDate.of(NO_YEAR_MARKER, month, day)
                    } else null
                } else if (clean.length == 4 && clean.all { it.isDigit() }) {
                    val month = clean.substring(0, 2).toIntOrNull()
                    val day = clean.substring(2, 4).toIntOrNull()
                    if (month != null && day != null) {
                        LocalDate.of(NO_YEAR_MARKER, month, day)
                    } else null
                } else {
                    runCatching {
                        val monthDay = java.time.MonthDay.parse(trimmed)
                        monthDay.atYear(NO_YEAR_MARKER)
                    }.getOrNull()
                }
            } else {
                // Falls ein Zeitstempel enthalten ist (z. B. "1990-05-15T00:00:00.000Z"),
                // schneiden wir auf die ersten 10 Zeichen ("yyyy-MM-dd") ab, wenn das Muster passt.
                val normalizedStr = if (trimmed.length > 10 &&
                    trimmed[4] == '-' &&
                    trimmed[7] == '-' &&
                    (trimmed[10] == 'T' || trimmed[10] == ' ')
                ) {
                    trimmed.substring(0, 10)
                } else {
                    trimmed
                }

                dateFormats.firstNotNullOfOrNull { format ->
                    runCatching { LocalDate.parse(normalizedStr, format) }.getOrNull()
                } ?: runCatching {
                    // Fallback für sonstige ISO-Datums-/Zeit-Formate (z. B. mit Zeitzone oder Offset)
                    java.time.OffsetDateTime.parse(trimmed).toLocalDate()
                }.getOrNull() ?: runCatching {
                    java.time.Instant.parse(trimmed).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                }.getOrNull()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fehler beim Parsen der Datumszeichenkette: $dateStr", e)
            null
        }
    }
}
