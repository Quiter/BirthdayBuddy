package com.heckmannch.birthdaybuddy2

import android.app.Application
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy2.database.AppDatabase
import com.heckmannch.birthdaybuddy2.database.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class BirthdayViewModel(application: Application) : AndroidViewModel(application) {
    private val contactDao = AppDatabase.getDatabase(application).contactDao()

    val contacts: StateFlow<List<Contact>> = contactDao.getAllContacts()
        .map { list ->
            list.sortedBy { it.birthday.daysUntilNext() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun syncContacts() {
        viewModelScope.launch {
            val systemContacts = fetchBirthdaysFromSystem()
            // Erst alle bestehenden IDs holen, um zu prüfen, ob wir alte Kontakte löschen müssen,
            // die im System nicht mehr existieren (optional, aber sauber).
            // Hier konzentrieren wir uns auf das Vermeiden von Duplikaten durch Eindeutigkeit:
            systemContacts.forEach { contact ->
                // Da id der PrimaryKey ist, überschreibt insertContact mit REPLACE Strategie den alten Eintrag.
                contactDao.insertContact(contact)
            }
        }
    }

    private suspend fun fetchBirthdaysFromSystem(): List<Contact> = withContext(Dispatchers.IO) {
        val contactsMap = mutableMapOf<String, Contact>() // Map nutzen, um Duplikate im Cursor-Resultat zu vermeiden
        val contentResolver = getApplication<Application>().contentResolver

        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.CommonDataKinds.Event.START_DATE,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.PHOTO_URI
        )

        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ?"
        val selectionArgs = arrayOf(
            ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString()
        )

        val cursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val dateIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE)
            val photoIndex = it.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

            while (it.moveToNext()) {
                val contactId = it.getString(idIndex) ?: continue
                val name = it.getString(nameIndex) ?: "Unbekannt"
                val dateStr = it.getString(dateIndex)
                val photoUri = it.getString(photoIndex)

                val birthday = parseDate(dateStr)
                if (birthday != null) {
                    contactsMap[contactId] = Contact(
                        id = contactId,
                        fullName = name,
                        birthday = birthday,
                        imageUri = photoUri
                    )
                }
            }
        }
        contactsMap.values.toList()
    }

    private fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr == null) return null
        return try {
            // Kontakte speichern Geburtstage oft in verschiedenen Formaten: YYYY-MM-DD oder --MM-DD
            if (dateStr.startsWith("--")) {
                // Jahr fehlt, wir nehmen das aktuelle Jahr als Platzhalter (wird bei daysUntilNext ignoriert)
                LocalDate.parse("1900-${dateStr.substring(2)}")
            } else {
                LocalDate.parse(dateStr)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun addTestContact(name: String, date: LocalDate) {
        viewModelScope.launch {
            contactDao.insertContact(
                Contact(
                    id = "test_${System.currentTimeMillis()}",
                    fullName = name,
                    birthday = date,
                    imageUri = null
                )
            )
        }
    }
}

fun LocalDate.daysUntilNext(): Long {
    val today = LocalDate.now()
    var nextBirthday = this.withYear(today.year)
    if (nextBirthday.isBefore(today) || nextBirthday.isEqual(today)) {
        nextBirthday = nextBirthday.plusYears(1)
    }
    return ChronoUnit.DAYS.between(today, nextBirthday)
}

fun LocalDate.nextAge(): Int {
    val today = LocalDate.now()
    var nextBirthday = this.withYear(today.year)
    if (nextBirthday.isBefore(today) || nextBirthday.isEqual(today)) {
        nextBirthday = nextBirthday.plusYears(1)
    }
    return nextBirthday.year - this.year
}
