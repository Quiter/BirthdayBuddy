package com.heckmannch.birthdaybuddy2.viewmodel

import android.app.Application
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy2.database.AppDatabase
import com.heckmannch.birthdaybuddy2.database.Contact
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import androidx.glance.appwidget.updateAll
import com.heckmannch.birthdaybuddy2.widget.BirthdayWidget

@Immutable
data class ContactUiModel(
    val id: String,
    val fullName: String,
    val dateText: String,
    val imageUri: String?,
    val initials: String,
    val nextAge: Int?,
    val nextAgeText: String?,
    val daysLeftText: String,
    val isToday: Boolean
)

class BirthdayViewModel(application: Application) : AndroidViewModel(application) {
    private val contactDao = AppDatabase.getDatabase(application).contactDao()
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    private val dayMonthFormatter = DateTimeFormatter.ofPattern("d. MMMM")

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedLabel = MutableStateFlow<String?>(null)
    val selectedLabel: StateFlow<String?> = _selectedLabel

    val availableLabels: StateFlow<List<String>> = contactDao.getAllContacts()
        .map { list ->
            list.flatMap { it.labels }
                .distinct()
                .sorted()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val contacts: StateFlow<List<ContactUiModel>> = combine(
        contactDao.getAllContacts(),
        _searchQuery,
        _selectedLabel
    ) { list, query, label ->
        val filteredList = list.filter { contact ->
            val matchesQuery = query.isEmpty() || contact.fullName.contains(query, ignoreCase = true)
            val matchesLabel = label == null || contact.labels.contains(label)
            matchesQuery && matchesLabel
        }
        
        filteredList.sortedBy { it.birthday.daysUntilNext() }
            .map { contact ->
                    val daysLeft = contact.birthday.daysUntilNext()
                    val nextAgeValue = if (contact.birthday.year != 1900) {
                        contact.birthday.nextAge()
                    } else null

                    ContactUiModel(
                        id = contact.id,
                        fullName = contact.fullName,
                        dateText = if (contact.birthday.year == 1900) {
                            contact.birthday.format(dayMonthFormatter)
                        } else {
                            contact.birthday.format(dateFormatter)
                        },
                        imageUri = contact.imageUri,
                        initials = contact.fullName.take(1).uppercase(),
                        nextAge = nextAgeValue,
                        nextAgeText = if (nextAgeValue != null) {
                            "wird $nextAgeValue"
                        } else null,
                        daysLeftText = if (daysLeft == 0L) "Heute!" else "In $daysLeft T.",
                        isToday = daysLeft == 0L
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onLabelSelected(label: String?) {
        _selectedLabel.value = if (_selectedLabel.value == label) null else label
    }

    fun syncContacts() {
        viewModelScope.launch {
            val systemContacts = fetchBirthdaysFromSystem()
            val contactGroups = fetchContactGroups()
            
            systemContacts.forEach { contact ->
                val labels = fetchLabelsForContact(contact.id, contactGroups)
                contactDao.insertContact(contact.copy(labels = labels))
            }
            // Widget nach dem Sync aktualisieren
            BirthdayWidget().updateAll(getApplication())
        }
    }

    private suspend fun fetchContactGroups(): Map<Long, String> = withContext(Dispatchers.IO) {
        val groupsMap = mutableMapOf<Long, String>()
        val contentResolver = getApplication<Application>().contentResolver
        
        val projection = arrayOf(
            ContactsContract.Groups._ID,
            ContactsContract.Groups.TITLE,
            ContactsContract.Groups.SYSTEM_ID
        )
        
        contentResolver.query(
            ContactsContract.Groups.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.Groups._ID)
            val titleIndex = cursor.getColumnIndex(ContactsContract.Groups.TITLE)
            val systemIdIndex = cursor.getColumnIndex(ContactsContract.Groups.SYSTEM_ID)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val title = cursor.getString(titleIndex)
                val systemId = cursor.getString(systemIdIndex)
                
                // Schließe Systemgruppen aus, die keine echten Nutzer-Labels sind
                if (systemId == "Contacts" || systemId == "Favorites") {
                    continue
                }

                if (!title.isNullOrBlank()) {
                    groupsMap[id] = title
                }
            }
        }
        groupsMap
    }

    private suspend fun fetchLabelsForContact(contactId: String, groupsMap: Map<Long, String>): List<String> = withContext(Dispatchers.IO) {
        val labels = mutableListOf<String>()
        val contentResolver = getApplication<Application>().contentResolver
        
        val projection = arrayOf(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)
        val selection = "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(
            contactId,
            ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE
        )
        
        contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val groupRowIdIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)
            while (cursor.moveToNext()) {
                val groupId = cursor.getLong(groupRowIdIndex)
                groupsMap[groupId]?.let { labels.add(it) }
            }
        }
        labels.distinct()
    }

    private suspend fun fetchBirthdaysFromSystem(): List<Contact> = withContext(Dispatchers.IO) {
        val contactsMap = mutableMapOf<String, Contact>()
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
            if (dateStr.startsWith("--")) {
                LocalDate.parse("1900-${dateStr.substring(2)}")
            } else {
                LocalDate.parse(dateStr)
            }
        } catch (e: Exception) {
            null
        }
    }
}

fun LocalDate.daysUntilNext(): Long {
    val today = LocalDate.now()
    var nextBirthday = this.withYear(today.year)
    if (nextBirthday.isBefore(today)) {
        nextBirthday = nextBirthday.plusYears(1)
    }
    return ChronoUnit.DAYS.between(today, nextBirthday)
}

fun LocalDate.nextAge(): Int {
    val today = LocalDate.now()
    var nextBirthday = this.withYear(today.year)
    if (nextBirthday.isBefore(today)) {
        nextBirthday = nextBirthday.plusYears(1)
    }
    return nextBirthday.year - this.year
}
