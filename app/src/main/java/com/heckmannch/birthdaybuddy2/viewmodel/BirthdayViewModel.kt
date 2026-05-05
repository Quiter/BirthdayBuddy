package com.heckmannch.birthdaybuddy2.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy2.database.AppDatabase
import com.heckmannch.birthdaybuddy2.database.Contact
import com.heckmannch.birthdaybuddy2.widget.BirthdayWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

/**
 * UI-Modell für einen Kontakt. 
 */
@Immutable
data class ContactUiModel(
    val id: String,
    val fullName: String,
    val dateText: String,
    val monthName: String,
    val imageUri: String?,
    val initials: String,
    val nextAge: Int?,
    val nextAgeText: String?,
    val daysLeftText: String,
    val isToday: Boolean,
)

class BirthdayViewModel(application: Application) : AndroidViewModel(application) {
    private val contactDao = AppDatabase.getDatabase(application).contactDao()
    
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    private val dayMonthFormatter = DateTimeFormatter.ofPattern("d. MMMM")
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM")

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedLabel = MutableStateFlow<String?>(null)
    val selectedLabel: StateFlow<String?> = _selectedLabel.asStateFlow()

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(replay = 0)
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    val availableLabels: StateFlow<List<String>> = contactDao.getAllContacts()
        .map { list ->
            try {
                list.asSequence()
                    .flatMap { it.labels }
                    .distinct()
                    .sorted()
                    .toList()
            } catch (_: Exception) {
                emptyList()
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val contacts: StateFlow<List<ContactUiModel>> = combine(
        contactDao.getAllContacts(),
        _searchQuery,
        _selectedLabel,
    ) { list, query, label ->
        try {
            list.asSequence()
                .filter { contact ->
                    val matchesQuery = query.isEmpty() || contact.fullName.contains(query, ignoreCase = true)
                    val matchesLabel = (label == null) || contact.labels.contains(label)
                    matchesQuery && matchesLabel
                }
                .sortedBy { it.birthday.safeDaysUntilNext() } // Sortierung auf Original-Daten
                .mapNotNull { contact -> 
                    try {
                        contact.toUiModel()
                    } catch (_: Exception) {
                        null
                    }
                }
                .toList()
        } catch (_: Exception) {
            emptyList()
        }
    }
    .flowOn(Dispatchers.Default)
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

    fun triggerScrollToTop() {
        viewModelScope.launch {
            _scrollToTopEvent.emit(Unit)
        }
    }

    fun syncContacts() {
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.READ_CONTACTS) 
            != PackageManager.PERMISSION_GRANTED
        ) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val systemContacts = fetchBirthdaysFromSystem()
                    val contactGroups = fetchContactGroups()
                    
                    systemContacts.forEach { contact ->
                        try {
                            val labels = fetchLabelsForContact(contact.id, contactGroups)
                            contactDao.insertContact(contact.copy(labels = labels))
                        } catch (_: Exception) {
                            // Einzelner Kontakt-Sync Fehler ignorieren
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            try {
                BirthdayWidget().updateAll(getApplication())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun Contact.toUiModel(): ContactUiModel {
        val daysLeft = birthday.safeDaysUntilNext()
        val hasYear = birthday.year != 1900
        val nextAgeValue = if (hasYear) birthday.safeNextAge() else null

        return ContactUiModel(
            id = id,
            fullName = fullName,
            dateText = if (!hasYear) birthday.format(dayMonthFormatter) else birthday.format(dateFormatter),
            monthName = birthday.format(monthFormatter),
            imageUri = imageUri,
            initials = fullName.take(1).uppercase(),
            nextAge = nextAgeValue,
            nextAgeText = nextAgeValue?.let { "wird $it" },
            daysLeftText = if (daysLeft == 0L) "Heute!" else "In $daysLeft T.",
            isToday = daysLeft == 0L,
        )
    }

    private suspend fun fetchContactGroups(): Map<Long, String> = withContext(Dispatchers.IO) {
        val groupsMap = mutableMapOf<Long, String>()
        val projection = arrayOf(
            ContactsContract.Groups._ID,
            ContactsContract.Groups.TITLE,
            ContactsContract.Groups.SYSTEM_ID,
        )
        
        try {
            getApplication<Application>().contentResolver.query(
                ContactsContract.Groups.CONTENT_URI,
                projection,
                null,
                null,
                null,
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Groups._ID)
                val titleIdx = cursor.getColumnIndex(ContactsContract.Groups.TITLE)
                val systemIdIdx = cursor.getColumnIndex(ContactsContract.Groups.SYSTEM_ID)
                
                if ((idIdx == -1) || (titleIdx == -1) || (systemIdIdx == -1)) return@withContext emptyMap()

                while (cursor.moveToNext()) {
                    val systemId = cursor.getString(systemIdIdx)
                    if ((systemId != "Contacts") && (systemId != "Favorites")) {
                        val title = cursor.getString(titleIdx)
                        if (!title.isNullOrBlank()) {
                            groupsMap[cursor.getLong(idIdx)] = title
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        groupsMap
    }

    private suspend fun fetchLabelsForContact(contactId: String, groupsMap: Map<Long, String>): List<String> = withContext(Dispatchers.IO) {
        val labels = mutableListOf<String>()
        val projection = arrayOf(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)
        val selection = "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(
            contactId,
            ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
        )
        
        try {
            getApplication<Application>().contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null,
            )?.use { cursor ->
                val groupRowIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)
                if (groupRowIdIdx == -1) return@withContext emptyList()
                
                while (cursor.moveToNext()) {
                    groupsMap[cursor.getLong(groupRowIdIdx)]?.let { labels.add(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        labels.distinct()
    }

    private suspend fun fetchBirthdaysFromSystem(): List<Contact> = withContext(Dispatchers.IO) {
        val contactsMap = mutableMapOf<String, Contact>()
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.CommonDataKinds.Event.START_DATE,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.PHOTO_URI,
        )

        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ?"
        val selectionArgs = arrayOf(
            ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString(),
        )

        try {
            getApplication<Application>().contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null,
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val dateIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE)
                val photoIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

                if ((idIdx == -1) || (nameIdx == -1) || (dateIdx == -1) || (photoIdx == -1)) return@withContext emptyList()

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIdx) ?: continue
                    parseDate(cursor.getString(dateIdx))?.let { birthday ->
                        contactsMap[id] = Contact(
                            id = id,
                            fullName = cursor.getString(nameIdx) ?: "Unbekannt",
                            birthday = birthday,
                            imageUri = cursor.getString(photoIdx),
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        } catch (_: Exception) {
            null
        }
    }
}

// --- Robuste Extensions für Datumsberechnungen ---

fun LocalDate.safeDaysUntilNext(): Long {
    return try {
        val today = LocalDate.now()
        var nextBirthday = this.toCurrentYear(today.year)
        
        if (nextBirthday.isBefore(today)) {
            nextBirthday = this.toCurrentYear(today.year + 1)
        }
        ChronoUnit.DAYS.between(today, nextBirthday)
    } catch (_: Exception) {
        Long.MAX_VALUE
    }
}

fun LocalDate.safeNextAge(): Int {
    return try {
        val today = LocalDate.now()
        var nextBirthday = this.toCurrentYear(today.year)
        
        if (nextBirthday.isBefore(today)) {
            nextBirthday = this.toCurrentYear(today.year + 1)
        }
        nextBirthday.year - this.year
    } catch (_: Exception) {
        0
    }
}

/**
 * Hilfsfunktion um ein Datum sicher in ein Zieljahr zu überführen (Handling für 29. Feb).
 */
private fun LocalDate.toCurrentYear(targetYear: Int): LocalDate {
    return try {
        this.withYear(targetYear)
    } catch (_: Exception) {
        // Falls targetYear kein Schaltjahr ist, nehmen wir den 28. Februar
        LocalDate.of(targetYear, 2, 28)
    }
}
