package com.heckmannch.birthdaybuddy.utils

import android.content.Context
import android.provider.ContactsContract
import com.heckmannch.birthdaybuddy.model.BirthdayContact

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

            // Labels holen
            val labels = getContactLabels(context, contactId)

            // Die Berechnung findet jetzt automatisch im DateHelper statt!
            val (age, remainingDays) = calculateAgeAndDays(bday)

            contactList.add(BirthdayContact(name, bday, labels, remainingDays, age))
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