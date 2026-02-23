package com.heckmannch.birthdaybuddy.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.model.ContactActions
import com.heckmannch.birthdaybuddy.utils.calculateAgeAndDays

object ContactMimes {
    const val WHATSAPP = "vnd.android.cursor.item/vnd.com.whatsapp.profile"
    const val SIGNAL = "vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.contact"
    const val TELEGRAM = "vnd.android.cursor.item/vnd.org.telegram.messenger.android.profile"
}

// Interne Keys für System-Labels (werden in der UI übersetzt)
private const val SYSTEM_LABEL_ALL = "My Contacts"
private const val SYSTEM_LABEL_STARRED = "Starred in Android"
private const val LABEL_NONE = "Unlabeled"

fun fetchBirthdays(context: Context): List<BirthdayContact> {
    val contactList = mutableListOf<BirthdayContact>()
    val contactIds = mutableSetOf<String>()
    
    // 1. Alle verfügbaren Gruppen (Labels) laden
    val groupMap = mutableMapOf<String, String>()
    context.contentResolver.query(
        ContactsContract.Groups.CONTENT_URI,
        arrayOf(ContactsContract.Groups._ID, ContactsContract.Groups.TITLE, ContactsContract.Groups.SYSTEM_ID),
        "${ContactsContract.Groups.DELETED} = 0",
        null,
        null
    )?.use { cursor ->
        val idIdx = cursor.getColumnIndex(ContactsContract.Groups._ID)
        val titleIdx = cursor.getColumnIndex(ContactsContract.Groups.TITLE)
        val systemIdIdx = cursor.getColumnIndex(ContactsContract.Groups.SYSTEM_ID)
        
        while (cursor.moveToNext()) {
            val id = cursor.getString(idIdx)
            val title = cursor.getString(titleIdx)
            val systemId = cursor.getString(systemIdIdx)
            
            val finalTitle = when {
                !title.isNullOrBlank() -> title.replace("System Group: ", "")
                !systemId.isNullOrBlank() -> systemId
                else -> null
            }
            if (id != null && finalTitle != null) groupMap[id] = finalTitle
        }
    }

    // 2. Alle Kontakte mit Geburtstag laden
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Event.CONTACT_ID,
        ContactsContract.CommonDataKinds.Event.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Event.START_DATE,
        ContactsContract.Data.STARRED,
        ContactsContract.Data.IN_VISIBLE_GROUP
    )
    val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ${ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY}"
    val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)

    val tempContacts = mutableMapOf<String, Triple<String, String, Set<String>>>()

    context.contentResolver.query(
        ContactsContract.Data.CONTENT_URI,
        projection, selection, selectionArgs, null
    )?.use { cursor ->
        val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.CONTACT_ID)
        val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.DISPLAY_NAME)
        val bdayIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE)
        val starredIdx = cursor.getColumnIndex(ContactsContract.Data.STARRED)
        val visibleIdx = cursor.getColumnIndex(ContactsContract.Data.IN_VISIBLE_GROUP)

        while (cursor.moveToNext()) {
            val id = cursor.getString(idIdx) ?: continue
            val name = cursor.getString(nameIdx) ?: "Unknown"
            val bday = cursor.getString(bdayIdx) ?: ""
            val isStarred = cursor.getInt(starredIdx) == 1
            val isVisible = cursor.getInt(visibleIdx) == 1
            
            val systemLabels = mutableSetOf<String>()
            if (isStarred) systemLabels.add(SYSTEM_LABEL_STARRED)
            if (isVisible) systemLabels.add(SYSTEM_LABEL_ALL)

            // Wir speichern nur den ersten Geburtstag pro Kontakt ID
            if (!tempContacts.containsKey(id)) {
                tempContacts[id] = Triple(name, bday, systemLabels)
                contactIds.add(id)
            }
        }
    }

    if (contactIds.isEmpty()) return emptyList()

    // 3. Batch-Abfrage für ALLES andere (Gruppenmitgliedschaften, Telefon, Mail, Messenger)
    val labelsMap = mutableMapOf<String, MutableSet<String>>()
    val actionsMap = mutableMapOf<String, ContactActions>()
    val phoneMap = mutableMapOf<String, String>()
    val emailMap = mutableMapOf<String, String>()
    val waSet = mutableSetOf<String>()
    val sigSet = mutableSetOf<String>()
    val tgSet = mutableSetOf<String>()

    contactIds.chunked(500).forEach { chunk ->
        val idList = chunk.joinToString(",") { "?" }
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data.CONTACT_ID, 
                ContactsContract.Data.MIMETYPE, 
                ContactsContract.Data.DATA1,
                ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID
            ),
            "${ContactsContract.Data.CONTACT_ID} IN ($idList)",
            chunk.toTypedArray(),
            null
        )?.use { cursor ->
            val cIdIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val mimeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val data1Idx = cursor.getColumnIndex(ContactsContract.Data.DATA1)
            val groupIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)

            while (cursor.moveToNext()) {
                val cid = cursor.getString(cIdIdx) ?: continue
                val mime = cursor.getString(mimeIdx)
                val data1 = cursor.getString(data1Idx)

                when (mime) {
                    ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE -> {
                        val groupId = cursor.getString(groupIdIdx)
                        groupMap[groupId]?.let { labelsMap.getOrPut(cid) { mutableSetOf() }.add(it) }
                    }
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> if (phoneMap[cid] == null) phoneMap[cid] = data1
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> if (emailMap[cid] == null) emailMap[cid] = data1
                    ContactMimes.WHATSAPP -> waSet.add(cid)
                    ContactMimes.SIGNAL -> sigSet.add(cid)
                    ContactMimes.TELEGRAM -> tgSet.add(cid)
                }
            }
        }
    }

    // 4. Daten zusammenführen
    for ((id, data) in tempContacts) {
        val (name, bday, systemLabels) = data
        val photoUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id.toLong()).let {
            Uri.withAppendedPath(it, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY).toString()
        }

        val (age, remainingDays) = calculateAgeAndDays(bday)
        
        // Labels kombinieren (System + Custom)
        val finalLabels = mutableSetOf<String>().apply {
            addAll(systemLabels)
            labelsMap[id]?.let { addAll(it) }
            // Falls gar keine Labels vorhanden sind, als "Ohne Label" markieren
            if (isEmpty()) add(LABEL_NONE)
        }

        contactList.add(BirthdayContact(
            id = id,
            name = name,
            birthday = bday,
            labels = finalLabels.toList().sorted(),
            remainingDays = remainingDays,
            age = age,
            actions = ContactActions(
                phoneNumber = phoneMap[id],
                email = emailMap[id],
                hasWhatsApp = waSet.contains(id),
                hasSignal = sigSet.contains(id),
                hasTelegram = tgSet.contains(id)
            ),
            photoUri = photoUri
        ))
    }

    return contactList
}
