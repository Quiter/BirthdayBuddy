package com.heckmannch.birthdaybuddy.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.model.ContactActions

fun fetchBirthdays(context: Context): List<BirthdayContact> {
    val contactList = mutableListOf<BirthdayContact>()

    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Event.CONTACT_ID,
        ContactsContract.CommonDataKinds.Event.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Event.START_DATE
    )

    val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ${ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY}"
    val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)

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

            val photoUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong()).let {
                Uri.withAppendedPath(it, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY).toString()
            }

            val labels = getContactLabels(context, contactId)
            val (age, remainingDays) = calculateAgeAndDays(bday)
            val actions = getContactActions(context, contactId)

            contactList.add(BirthdayContact(name, bday, labels, remainingDays, age, actions, photoUri))
        }
    }
    return contactList
}

private fun getContactLabels(context: Context, contactId: String): List<String> {
    val groupIds = mutableListOf<String>()

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
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> if (phone == null) phone = data1
                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> if (email == null) email = data1
                "vnd.android.cursor.item/vnd.com.whatsapp.profile" -> hasWA = true
                "vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.contact" -> hasSig = true
                "vnd.android.cursor.item/vnd.org.telegram.messenger.android.profile" -> hasTG = true
            }
        }
    }
    return ContactActions(phone, email, hasWA, hasSig, hasTG)
}