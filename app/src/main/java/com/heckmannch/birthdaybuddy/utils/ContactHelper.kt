package com.heckmannch.birthdaybuddy.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.model.ContactActions

/**
 * Durchsucht das Android-Telefonbuch nach allen Kontakten, die ein Geburtsdatum hinterlegt haben.
 *
 * @param context Der App-Kontext für den Zugriff auf den ContentResolver.
 * @return Eine Liste von [BirthdayContact] Objekten mit Namen, Datum, Labels und Kontaktmöglichkeiten.
 */
fun fetchBirthdays(context: Context): List<BirthdayContact> {
    val contactList = mutableListOf<BirthdayContact>()

    // Definition der Spalten, die wir aus der Kontaktdatenbank lesen wollen
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Event.CONTACT_ID,
        ContactsContract.CommonDataKinds.Event.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Event.START_DATE
    )

    // Filter: Wir suchen nach Daten vom Typ "Event", die speziell ein "Geburtstag" sind
    val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ${ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY}"
    val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)

    // Abfrage der Kontaktdatenbank
    context.contentResolver.query(
        ContactsContract.Data.CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
    )?.use { cursor ->
        val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.CONTACT_ID)
        val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.DISPLAY_NAME)
        val bdayIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE)

        while (cursor.moveToNext()) {
            val contactId = cursor.getString(idIdx) ?: continue
            val name = cursor.getString(nameIdx) ?: "Unbekannt"
            val bday = cursor.getString(bdayIdx) ?: ""

            // Konstruiert die URI zum Profilbild des Kontakts
            val photoUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong()).let {
                Uri.withAppendedPath(it, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY).toString()
            }

            // Holt zusätzliche Informationen wie Labels und Messenger-Accounts
            val labels = getContactLabels(context, contactId)
            val (age, remainingDays) = calculateAgeAndDays(bday)
            val actions = getContactActions(context, contactId)

            contactList.add(BirthdayContact(name, bday, labels, remainingDays, age, actions, photoUri))
        }
    }
    return contactList
}

/**
 * Ermittelt alle Labels (Kontaktgruppen), denen ein spezifischer Kontakt zugeordnet ist.
 */
private fun getContactLabels(context: Context, contactId: String): List<String> {
    val groupIds = mutableListOf<String>()

    // Schritt 1: IDs der Gruppen finden, in denen der Kontakt Mitglied ist
    context.contentResolver.query(
        ContactsContract.Data.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID),
        "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
        arrayOf(contactId, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE),
        null
    )?.use { cursor ->
        val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)
        while (cursor.moveToNext()) {
            cursor.getString(idIdx)?.let { groupIds.add(it) }
        }
    }

    if (groupIds.isEmpty()) return listOf("Ohne Label")

    val labels = mutableListOf<String>()
    val placeholders = groupIds.joinToString(",") { "?" }

    // Schritt 2: Die lesbaren Namen (Titel) zu den gefundenen Gruppen-IDs abfragen
    context.contentResolver.query(
        ContactsContract.Groups.CONTENT_URI,
        arrayOf(ContactsContract.Groups.TITLE),
        "${ContactsContract.Groups._ID} IN ($placeholders)",
        groupIds.toTypedArray(),
        null
    )?.use { cursor ->
        val titleIdx = cursor.getColumnIndex(ContactsContract.Groups.TITLE)
        while (cursor.moveToNext()) {
            cursor.getString(titleIdx)?.replace("System Group: ", "")?.let {
                if (it.isNotBlank()) labels.add(it)
            }
        }
    }

    return labels.ifEmpty { listOf("Ohne Label") }
}

/**
 * Durchsucht alle Datenfelder eines Kontakts nach Telefonnummern, E-Mails
 * und Profilen von Messengern (WhatsApp, Signal, Telegram).
 */
private fun getContactActions(context: Context, contactId: String): ContactActions {
    var phone: String? = null
    var email: String? = null
    var hasWA = false
    var hasSig = false
    var hasTG = false

    context.contentResolver.query(
        ContactsContract.Data.CONTENT_URI,
        arrayOf(ContactsContract.Data.MIMETYPE, ContactsContract.Data.DATA1),
        "${ContactsContract.Data.CONTACT_ID} = ?",
        arrayOf(contactId),
        null
    )?.use { cursor ->
        val mimeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
        val data1Idx = cursor.getColumnIndex(ContactsContract.Data.DATA1)

        while (cursor.moveToNext()) {
            val mime = cursor.getString(mimeIdx)
            val data1 = cursor.getString(data1Idx)

            when (mime) {
                // Standard Telefonnummer
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> if (phone == null) phone = data1
                // Standard E-Mail
                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> if (email == null) email = data1
                // Spezifische Mime-Types für Messenger-Integrationen
                "vnd.android.cursor.item/vnd.com.whatsapp.profile" -> hasWA = true
                "vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.contact" -> hasSig = true
                "vnd.android.cursor.item/vnd.org.telegram.messenger.android.profile" -> hasTG = true
            }
        }
    }
    return ContactActions(phone, email, hasWA, hasSig, hasTG)
}
