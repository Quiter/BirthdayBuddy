package com.heckmannch.birthdaybuddy.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.model.ContactActions
import com.heckmannch.birthdaybuddy.utils.calculateAgeAndDays

/**
 * Konstanten für spezifische Kontakt-MIME-Types.
 */
object ContactMimes {
    const val WHATSAPP = "vnd.android.cursor.item/vnd.com.whatsapp.profile"
    const val SIGNAL = "vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.contact"
    const val TELEGRAM = "vnd.android.cursor.item/vnd.org.telegram.messenger.android.profile"
}

/**
 * Optimierte Funktion zum Laden aller Geburtstagskontakte.
 * Reduziert die Datenbankabfragen drastisch (Batch-Processing statt Einzelabfragen).
 */
fun fetchBirthdays(context: Context): List<BirthdayContact> {
    val contactList = mutableListOf<BirthdayContact>()
    val contactIds = mutableSetOf<String>()
    
    // 1. Alle Geburtstag-Events abfragen
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Event.CONTACT_ID,
        ContactsContract.CommonDataKinds.Event.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Event.START_DATE
    )
    val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ${ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY}"
    val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)

    val tempContactsMap = mutableMapOf<String, Pair<String, String>>()

    context.contentResolver.query(
        ContactsContract.Data.CONTENT_URI,
        projection, selection, selectionArgs, null
    )?.use { cursor ->
        val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.CONTACT_ID)
        val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.DISPLAY_NAME)
        val bdayIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE)

        while (cursor.moveToNext()) {
            val id = cursor.getString(idIdx) ?: continue
            val name = cursor.getString(nameIdx) ?: "Unbekannt"
            val bday = cursor.getString(bdayIdx) ?: ""
            if (!tempContactsMap.containsKey(id)) {
                tempContactsMap[id] = name to bday
                contactIds.add(id)
            }
        }
    }

    if (contactIds.isEmpty()) return emptyList()

    // 2. Batch-Abfrage für Labels und Aktionen
    val allLabels = getAllContactLabels(context, contactIds)
    val allActions = getAllContactActions(context, contactIds)

    for ((id, data) in tempContactsMap) {
        val (name, bday) = data
        val photoUri = try {
            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id.toLong()).let {
                Uri.withAppendedPath(it, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY).toString()
            }
        } catch (e: Exception) { null }

        val (age, remainingDays) = calculateAgeAndDays(bday)
        
        contactList.add(BirthdayContact(
            id = id,
            name = name,
            birthday = bday,
            labels = allLabels[id] ?: listOf("Ohne Label"),
            remainingDays = remainingDays,
            age = age,
            actions = allActions[id] ?: ContactActions(),
            photoUri = photoUri
        ))
    }
    return contactList
}

private fun getAllContactLabels(context: Context, contactIds: Set<String>): Map<String, List<String>> {
    val result = mutableMapOf<String, MutableList<String>>()
    if (contactIds.isEmpty()) return result

    val groupTitles = mutableMapOf<String, String>()
    context.contentResolver.query(
        ContactsContract.Groups.CONTENT_URI,
        arrayOf(ContactsContract.Groups._ID, ContactsContract.Groups.TITLE),
        null, null, null
    )?.use { cursor ->
        val idIdx = cursor.getColumnIndex(ContactsContract.Groups._ID)
        val titleIdx = cursor.getColumnIndex(ContactsContract.Groups.TITLE)
        while (cursor.moveToNext()) {
            val id = cursor.getString(idIdx)
            val title = cursor.getString(titleIdx)?.replace("System Group: ", "")
            if (id != null && !title.isNullOrBlank()) groupTitles[id] = title
        }
    }

    contactIds.chunked(900).forEach { chunk ->
        val idList = chunk.joinToString(",") { "?" }
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data.CONTACT_ID, ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID),
            "${ContactsContract.Data.CONTACT_ID} IN ($idList) AND ${ContactsContract.Data.MIMETYPE} = ?",
            chunk.toTypedArray() + ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
            null
        )?.use { cursor ->
            val contactIdIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val groupIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)
            while (cursor.moveToNext()) {
                val cId = cursor.getString(contactIdIdx)
                val gId = cursor.getString(groupIdIdx)
                groupTitles[gId]?.let { title ->
                    result.getOrPut(cId) { mutableListOf() }.add(title)
                }
            }
        }
    }
    return result
}

private fun getAllContactActions(context: Context, contactIds: Set<String>): Map<String, ContactActions> {
    val resultMap = mutableMapOf<String, ContactActions>()
    if (contactIds.isEmpty()) return resultMap

    val phoneMap = mutableMapOf<String, String>()
    val emailMap = mutableMapOf<String, String>()
    val waSet = mutableSetOf<String>()
    val sigSet = mutableSetOf<String>()
    val tgSet = mutableSetOf<String>()

    contactIds.chunked(900).forEach { chunk ->
        val idList = chunk.joinToString(",") { "?" }
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data.CONTACT_ID, ContactsContract.Data.MIMETYPE, ContactsContract.Data.DATA1),
            "${ContactsContract.Data.CONTACT_ID} IN ($idList)",
            chunk.toTypedArray(),
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val mimeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val data1Idx = cursor.getColumnIndex(ContactsContract.Data.DATA1)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx) ?: continue
                val mime = cursor.getString(mimeIdx)
                val data1 = cursor.getString(data1Idx)

                when (mime) {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> if (!phoneMap.containsKey(id)) phoneMap[id] = data1
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> if (!emailMap.containsKey(id)) emailMap[id] = data1
                    ContactMimes.WHATSAPP -> waSet.add(id)
                    ContactMimes.SIGNAL -> sigSet.add(id)
                    ContactMimes.TELEGRAM -> tgSet.add(id)
                }
            }
        }
    }

    contactIds.forEach { id ->
        resultMap[id] = ContactActions(phoneMap[id], emailMap[id], waSet.contains(id), sigSet.contains(id), tgSet.contains(id))
    }
    return resultMap
}
