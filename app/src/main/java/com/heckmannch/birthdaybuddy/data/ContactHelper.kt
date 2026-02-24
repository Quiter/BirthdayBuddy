package com.heckmannch.birthdaybuddy.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.GroupMembership
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.Groups
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.model.ContactActions
import com.heckmannch.birthdaybuddy.utils.calculateAgeAndDays

object ContactMimes {
    const val WHATSAPP = "vnd.android.cursor.item/vnd.com.whatsapp.profile"
    const val SIGNAL = "vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.contact"
    const val TELEGRAM = "vnd.android.cursor.item/vnd.org.telegram.messenger.android.profile"
}

private const val SYSTEM_LABEL_ALL = "My Contacts"
private const val SYSTEM_LABEL_STARRED = "Starred in Android"
private const val LABEL_NONE = "Unlabeled"

/**
 * Hochgradig robuste Implementierung zum Abrufen von Geburtstagen und Labels.
 * Berücksichtigt TITLE, TITLE_RES und SYSTEM_ID, um sicherzustellen, dass 
 * Emoji-Labels und Google-Gruppen über alle Konten hinweg gefunden werden.
 */
fun fetchBirthdays(context: Context): List<BirthdayContact> {
    val cr = context.contentResolver
    
    // 1. SCHRITT: Alle Gruppen-Namen auflösen
    val groupMap = mutableMapOf<Long, String>()
    cr.query(
        Groups.CONTENT_URI,
        arrayOf(Groups._ID, Groups.TITLE, Groups.TITLE_RES, Groups.RES_PACKAGE, Groups.SYSTEM_ID),
        null, null, null
    )?.use { cursor ->
        val idIdx = cursor.getColumnIndex(Groups._ID)
        val titleIdx = cursor.getColumnIndex(Groups.TITLE)
        val titleResIdx = cursor.getColumnIndex(Groups.TITLE_RES)
        val resPackageIdx = cursor.getColumnIndex(Groups.RES_PACKAGE)
        val systemIdIdx = cursor.getColumnIndex(Groups.SYSTEM_ID)
        
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idIdx)
            val title = cursor.getString(titleIdx)
            val titleRes = cursor.getInt(titleResIdx)
            val resPackage = cursor.getString(resPackageIdx)
            val systemId = cursor.getString(systemIdIdx)
            
            // Versuche den Namen zu bestimmen: 1. Titel (Emojis sind hier), 2. Lokalisierte Ressource, 3. System-ID
            var finalName: String? = null
            
            if (!title.isNullOrBlank()) {
                finalName = title
            } else if (titleRes != 0 && !resPackage.isNullOrBlank()) {
                try {
                    val packageRes = context.packageManager.getResourcesForApplication(resPackage)
                    finalName = packageRes.getString(titleRes)
                } catch (_: Exception) {}
            }
            
            if (finalName == null && !systemId.isNullOrBlank()) {
                finalName = systemId
            }

            if (finalName != null) {
                groupMap[id] = finalName.replace("System Group: ", "")
            }
        }
    }

    // 2. SCHRITT: Alle Kontakte mit Geburtstag finden
    val birthdayContacts = mutableMapOf<Long, TempContactBuilder>()
    val birthdaySelection = "${Data.MIMETYPE} = ? AND ${Event.TYPE} = ?"
    val birthdayArgs = arrayOf(Event.CONTENT_ITEM_TYPE, Event.TYPE_BIRTHDAY.toString())
    
    cr.query(
        Data.CONTENT_URI,
        arrayOf(Data.CONTACT_ID, Data.DISPLAY_NAME, Event.START_DATE, Data.STARRED, Data.IN_VISIBLE_GROUP, Data.PHOTO_THUMBNAIL_URI),
        birthdaySelection,
        birthdayArgs,
        null
    )?.use { cursor ->
        val idCol = cursor.getColumnIndex(Data.CONTACT_ID)
        val nameCol = cursor.getColumnIndex(Data.DISPLAY_NAME)
        val dateCol = cursor.getColumnIndex(Event.START_DATE)
        val starredCol = cursor.getColumnIndex(Data.STARRED)
        val visibleCol = cursor.getColumnIndex(Data.IN_VISIBLE_GROUP)
        val photoCol = cursor.getColumnIndex(Data.PHOTO_THUMBNAIL_URI)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val name = cursor.getString(nameCol) ?: "Unknown"
            val bday = cursor.getString(dateCol) ?: continue
            val isStarred = cursor.getInt(starredCol) != 0
            val isVisible = cursor.getInt(visibleCol) != 0
            val photo = cursor.getString(photoCol)
            
            if (!birthdayContacts.containsKey(id)) {
                birthdayContacts[id] = TempContactBuilder(id, name, bday, isStarred, isVisible, photo)
            }
        }
    }

    if (birthdayContacts.isEmpty()) return emptyList()

    // 3. SCHRITT: Batch-Abfrage für ALLES andere (Labels, Telefon, Messenger)
    val allIds = birthdayContacts.keys.toList()
    allIds.chunked(400).forEach { chunk ->
        val selection = "${Data.CONTACT_ID} IN (${chunk.joinToString(",")})"
        cr.query(
            Data.CONTENT_URI,
            arrayOf(Data.CONTACT_ID, Data.MIMETYPE, Data.DATA1, GroupMembership.GROUP_ROW_ID),
            selection,
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(Data.CONTACT_ID)
            val mimeIdx = cursor.getColumnIndex(Data.MIMETYPE)
            val data1Idx = cursor.getColumnIndex(Data.DATA1)
            val groupIdx = cursor.getColumnIndex(GroupMembership.GROUP_ROW_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val contact = birthdayContacts[id] ?: continue
                val mime = cursor.getString(mimeIdx)
                val data1 = cursor.getString(data1Idx)

                when (mime) {
                    GroupMembership.CONTENT_ITEM_TYPE -> {
                        // DATA1 (alias GROUP_ROW_ID) enthält die Gruppen-ID
                        val groupId = cursor.getLong(groupIdx)
                        groupMap[groupId]?.let { contact.labels.add(it) }
                    }
                    Phone.CONTENT_ITEM_TYPE -> if (contact.phone == null) contact.phone = data1
                    Email.CONTENT_ITEM_TYPE -> if (contact.email == null) contact.email = data1
                    ContactMimes.WHATSAPP -> contact.hasWA = true
                    ContactMimes.SIGNAL -> contact.hasSig = true
                    ContactMimes.TELEGRAM -> contact.hasTG = true
                }
            }
        }
    }

    // 4. SCHRITT: Finale Transformation
    return birthdayContacts.values.map { builder ->
        val (age, remainingDays) = calculateAgeAndDays(builder.birthday)
        val photoUri = builder.photoUri ?: ContentUris.withAppendedId(Contacts.CONTENT_URI, builder.id).let {
            Uri.withAppendedPath(it, Contacts.Photo.CONTENT_DIRECTORY).toString()
        }

        val labels = mutableSetOf<String>()
        if (builder.isVisible) labels.add(SYSTEM_LABEL_ALL)
        if (builder.isStarred) labels.add(SYSTEM_LABEL_STARRED)
        labels.addAll(builder.labels)
        
        if (labels.isEmpty()) labels.add(LABEL_NONE)

        BirthdayContact(
            id = builder.id.toString(),
            name = builder.name,
            birthday = builder.birthday,
            labels = labels.toList().sorted(),
            remainingDays = remainingDays,
            age = age,
            actions = ContactActions(
                phoneNumber = builder.phone,
                email = builder.email,
                hasWhatsApp = builder.hasWA,
                hasSignal = builder.hasSig,
                hasTelegram = builder.hasTG
            ),
            photoUri = photoUri
        )
    }
}

private class TempContactBuilder(
    val id: Long,
    val name: String,
    val birthday: String,
    val isStarred: Boolean,
    val isVisible: Boolean,
    val photoUri: String?
) {
    val labels = mutableSetOf<String>()
    var phone: String? = null
    var email: String? = null
    var hasWA: Boolean = false
    var hasSig: Boolean = false
    var hasTG: Boolean = false
}
