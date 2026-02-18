package com.heckmannch.birthdaybuddy.utils

import android.content.Context
import android.provider.ContactsContract
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.model.ContactActions

// Holt alle Kontakte mit Geburtsdatum aus der Datenbank
fun fetchBirthdays(context: Context): List<BirthdayContact> {
    val contactList = mutableListOf<BirthdayContact>()

    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Event.CONTACT_ID,
        ContactsContract.CommonDataKinds.Event.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Event.START_DATE,
        ContactsContract.CommonDataKinds.Event.LABEL,
        ContactsContract.CommonDataKinds.Event.TYPE
    )

    val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ${ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY}"
    val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)

    val cursor = context.contentResolver.query(
        ContactsContract.Data.CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
    )

    cursor?.use {
        val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Event.CONTACT_ID)
        val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Event.DISPLAY_NAME)
        val birthdayIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE)

        while (it.moveToNext()) {
            val contactId = it.getString(idIndex) ?: ""
            val name = it.getString(nameIndex) ?: "Unbekannt"
            val bday = it.getString(birthdayIndex) ?: ""

            // NEU: Den Pfad zum Kontaktbild im Android-System zusammenbauen
            val photoUri = if (contactId.isNotEmpty()) {
                val contactUri = android.content.ContentUris.withAppendedId(android.provider.ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
                android.net.Uri.withAppendedPath(contactUri, android.provider.ContactsContract.Contacts.Photo.CONTENT_DIRECTORY).toString()
            } else null

            // Labels holen
            val labels = getContactLabels(context, contactId)

            // Die Berechnung findet jetzt automatisch im DateHelper statt!
            val (age, remainingDays) = calculateAgeAndDays(bday)

            // NEU: Wir laden die verknüpften Aktionen
            val actions = getContactActions(context, contactId)

            // NEU: Aktionen an den Kontakt übergeben
            contactList.add(BirthdayContact(name, bday, labels, remainingDays, age, actions, photoUri))
        }
    }
    return contactList
}

// Holt die Gruppen/Labels für einen einzelnen Kontakt
private fun getContactLabels(context: Context, contactId: String): List<String> {
    val groupIds = mutableListOf<String>()

    val groupCursor = context.contentResolver.query(
        ContactsContract.Data.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID),
        "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
        arrayOf(contactId, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE),
        null
    )

    groupCursor?.use {
        val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)
        while (it.moveToNext()) {
            val groupId = it.getString(idIndex)
            if (groupId != null) groupIds.add(groupId)
        }
    }

    if (groupIds.isEmpty()) return listOf("Ohne Label")

    val labels = mutableListOf<String>()
    val placeholders = groupIds.joinToString(",") { "?" }

    val titleCursor = context.contentResolver.query(
        ContactsContract.Groups.CONTENT_URI,
        arrayOf(ContactsContract.Groups.TITLE),
        "${ContactsContract.Groups._ID} IN ($placeholders)",
        groupIds.toTypedArray(),
        null
    )

    titleCursor?.use {
        val titleIndex = it.getColumnIndex(ContactsContract.Groups.TITLE)
        while (it.moveToNext()) {
            val title = it.getString(titleIndex)?.replace("System Group: ", "")
            if (!title.isNullOrBlank()) {
                labels.add(title)
            }
        }
    }

    return if (labels.isEmpty()) listOf("Ohne Label") else labels
}

// Sucht nach Telefonnummern, E-Mails und Messenger-Profilen für einen Kontakt
private fun getContactActions(context: Context, contactId: String): ContactActions {
    var phone: String? = null
    var email: String? = null
    var hasWhatsApp = false
    var hasSignal = false
    var hasTelegram = false

    val cursor = context.contentResolver.query(
        ContactsContract.Data.CONTENT_URI,
        arrayOf(ContactsContract.Data.MIMETYPE, ContactsContract.Data.DATA1),
        "${ContactsContract.Data.CONTACT_ID} = ?",
        arrayOf(contactId),
        null
    )

    cursor?.use {
        val mimeTypeIndex = it.getColumnIndex(ContactsContract.Data.MIMETYPE)
        val data1Index = it.getColumnIndex(ContactsContract.Data.DATA1)

        while (it.moveToNext()) {
            val mimeType = it.getString(mimeTypeIndex)
            val data1 = it.getString(data1Index)

            when (mimeType) {
                // Telefonnummer (wir nehmen einfach die erste, die wir finden)
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> if (phone == null) phone = data1
                // E-Mail Adresse
                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> if (email == null) email = data1
                // Die geheimen System-Codes der Messenger:
                "vnd.android.cursor.item/vnd.com.whatsapp.profile" -> hasWhatsApp = true
                "vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.contact" -> hasSignal = true
                "vnd.android.cursor.item/vnd.org.telegram.messenger.android.profile" -> hasTelegram = true
            }
        }
    }

    return ContactActions(phone, email, hasWhatsApp, hasSignal, hasTelegram)
}