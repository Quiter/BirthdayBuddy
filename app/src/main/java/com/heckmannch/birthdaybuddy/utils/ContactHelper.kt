package com.heckmannch.birthdaybuddy.utils

import android.content.Context
import android.provider.ContactsContract
import com.heckmannch.birthdaybuddy.model.BirthdayContact

// Wir übergeben jetzt den "Context", damit die Funktion in der Datenbank lesen darf
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

    // WICHTIG: Hier nutzen wir jetzt context.contentResolver
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

            // Auch hier geben wir den Context weiter
            val label = getContactLabels(context, contactId)

            val (age, remainingDays) = calculateAgeAndDays(bday)
            contactList.add(BirthdayContact(name, bday, label, remainingDays, age))
        }
    }
    return contactList
}

private fun getContactLabels(context: Context, contactId: String): String {
    val groupIds = mutableListOf<String>()

    // WICHTIG: context.contentResolver
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

    if (groupIds.isEmpty()) return "Ohne Label"

    val labels = mutableListOf<String>()
    val placeholders = groupIds.joinToString(",") { "?" }

    // WICHTIG: context.contentResolver
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

    return if (labels.isEmpty()) "Ohne Label" else labels.joinToString(", ")
}

// Mathe braucht keinen Context, das funktioniert einfach so
private fun calculateAgeAndDays(birthDateString: String): Pair<Int, Int> {
    if (birthDateString.isEmpty()) return Pair(0, 0)

    return try {
        val today = java.time.LocalDate.now()

        if (birthDateString.startsWith("--")) {
            val month = birthDateString.substring(2, 4).toInt()
            val day = birthDateString.substring(5, 7).toInt()

            var nextBday = java.time.LocalDate.of(today.year, month, day)
            if (nextBday.isBefore(today)) {
                nextBday = nextBday.plusYears(1)
            }

            val days = java.time.temporal.ChronoUnit.DAYS.between(today, nextBday).toInt()
            return Pair(0, days)
        } else {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val birthDate = java.time.LocalDate.parse(birthDateString, formatter)

            var turnsAge = today.year - birthDate.year
            var nextBday = birthDate.withYear(today.year)

            if (nextBday.isBefore(today)) {
                nextBday = nextBday.plusYears(1)
                turnsAge += 1
            }

            val days = java.time.temporal.ChronoUnit.DAYS.between(today, nextBday).toInt()
            return Pair(turnsAge, days)
        }
    } catch (e: Exception) {
        Pair(0, 0)
    }
}