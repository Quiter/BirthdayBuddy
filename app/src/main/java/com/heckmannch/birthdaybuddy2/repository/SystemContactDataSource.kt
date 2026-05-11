package com.heckmannch.birthdaybuddy2.repository

import android.content.Context
import android.provider.ContactsContract
import com.heckmannch.birthdaybuddy2.database.Contact
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
    @param:ApplicationContext private val context: Context,
) {
    private val dateFormats = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyyMMdd"),
    )

    private val redundantLabels = setOf("my contacts", "contacts", "ich", "me")

    suspend fun fetchContactGroups(): Map<Long, GroupInfo> = withContext(Dispatchers.IO) {
        val groups = mutableMapOf<Long, GroupInfo>()
        val projection = arrayOf(
            ContactsContract.Groups._ID,
            ContactsContract.Groups.TITLE,
            ContactsContract.Groups.SYSTEM_ID,
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

    suspend fun fetchBirthdaysFromSystem(groups: Map<Long, GroupInfo>): List<Contact> = withContext(Dispatchers.IO) {
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
}
