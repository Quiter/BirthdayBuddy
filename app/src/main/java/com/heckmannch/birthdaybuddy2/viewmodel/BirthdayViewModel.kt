package com.heckmannch.birthdaybuddy2.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy2.database.AppDatabase
import com.heckmannch.birthdaybuddy2.database.Contact
import com.heckmannch.birthdaybuddy2.database.LabelConfig
import com.heckmannch.birthdaybuddy2.ui.screens.settings.notifications.PreferenceManager
import com.heckmannch.birthdaybuddy2.ui.screens.settings.notifications.NotificationWorker
import com.heckmannch.birthdaybuddy2.widget.BirthdayWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

@Immutable
data class GiftIdea(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false,
) {
    companion object {
        fun fromString(encoded: String?): List<GiftIdea> {
            if (encoded.isNullOrBlank()) return emptyList()
            return encoded.split(";;").mapNotNull {
                val parts = it.split("|", limit = 2)
                if (parts.size == 2) {
                    GiftIdea(text = parts[1], isChecked = parts[0] == "1")
                } else null
            }
        }

        fun toString(ideas: List<GiftIdea>): String {
            return ideas.joinToString(";;") { "${if (it.isChecked) "1" else "0"}|${it.text}" }
        }
    }
}

/**
 * UI-Modell für einen Kontakt. 
 */
@Immutable
data class ContactUiModel(
    val id: String, // Interner Key oder lookupKey
    val contactId: String,
    val lookupKey: String,
    val fullName: String,
    val dateText: String,
    val monthName: String,
    val imageUri: String?,
    val initials: String,
    val nextAge: Int?,
    val nextAgeText: String?,
    val daysUntilNext: Long,
    val daysLeftText: String,
    val isToday: Boolean,
    val giftIdeas: String?,
)

@Immutable
data class LabelManagementModel(
    val name: String,
    val isHiddenFromFilter: Boolean,
    val isIgnored: Boolean,
    val isSystem: Boolean,
)

private data class GroupInfo(val title: String, val isSystem: Boolean)

class BirthdayViewModel(application: Application) : AndroidViewModel(application) {
    private val contactDao = AppDatabase.getDatabase(application).contactDao()
    private val labelConfigDao = AppDatabase.getDatabase(application).labelConfigDao()
    private val preferenceManager = PreferenceManager(application)

    val notificationsEnabled: StateFlow<Boolean> = preferenceManager.notificationsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    val notificationHour: StateFlow<Int> = preferenceManager.notificationHour
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 9,
        )

    val notificationMinute: StateFlow<Int> = preferenceManager.notificationMinute
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0,
        )

    init {
        viewModelScope.launch {
            combine(
                notificationsEnabled,
                notificationHour,
                notificationMinute,
            ) { enabled, hour, minute ->
                Triple(enabled, hour, minute)
            }.collect { (enabled, hour, minute) ->
                if (enabled) {
                    NotificationWorker.enqueueDailyNotification(getApplication(), hour, minute)
                } else {
                    NotificationWorker.cancelNotification(getApplication())
                }
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferenceManager.setNotificationsEnabled(enabled)
        }
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferenceManager.setNotificationTime(hour, minute)
        }
    }

    val swipeHintShown: StateFlow<Boolean> = preferenceManager.swipeHintShown
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true, // Default true to avoid flash before load
        )

    fun setSwipeHintShown() {
        viewModelScope.launch {
            preferenceManager.setSwipeHintShown(shown = true)
        }
    }

    fun updateGiftIdeas(lookupKey: String, ideas: String) {
        viewModelScope.launch {
            val contact = contactDao.getContactByLookupKey(lookupKey) ?: return@launch
            contactDao.insertContact(contact.copy(giftIdeas = ideas))
        }
    }
    
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    private val dayMonthFormatter = DateTimeFormatter.ofPattern("d. MMMM")
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM")

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedLabel = MutableStateFlow<String?>(null)
    val selectedLabel: StateFlow<String?> = _selectedLabel.asStateFlow()

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(replay = 0)
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    private val _isFastScrolling = MutableStateFlow(false)
    val isFastScrolling: StateFlow<Boolean> = _isFastScrolling.asStateFlow()

    fun setFastScrolling(isScrolling: Boolean) {
        _isFastScrolling.value = isScrolling
    }

    val availableLabels: StateFlow<List<String>> = combine(
        contactDao.getAllContacts(),
        labelConfigDao.getAllConfigs(),
    ) { contacts, configs ->
        val configMap = configs.associateBy { it.name }
        
        val allLabelsInContacts = contacts.asSequence()
            .flatMap { it.labels }
            .distinct()
            .toList()
        
        // Prüfung: Gibt es mindestens ein Label, das KEIN System-Label ist?
        val hasUserLabels = allLabelsInContacts.any { name ->
            configMap[name]?.isSystem == false
        }

        // Wenn NUR System-Labels vorhanden sind, blenden wir die Filterbar komplett aus
        if (!hasUserLabels) return@combine emptyList()

        // Sobald ein User-Label existiert, zeigen wir ALLE an (auch System-Labels wie "Family"),
        // sofern sie nicht manuell in den Settings verborgen wurden.
        allLabelsInContacts.asSequence()
            .filter { name ->
                val config = configMap[name]
                !(config?.isHiddenFromFilter ?: false) && !(config?.isIgnored ?: false)
            }
            .sorted()
            .toList()
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    /**
     * Die Haupt-Liste der Kontakte. 
     * Kombiniert Datenbank-Inhalt mit Suchanfrage und Label-Filter.
     * Initialwert ist null, um "Laden" von "Leer" zu unterscheiden.
     */
    val contacts: StateFlow<List<ContactUiModel>?> = combine(
        contactDao.getAllContacts(),
        _searchQuery,
        _selectedLabel,
        labelConfigDao.getAllConfigs(),
    ) { list, query, label, configs ->
        val ignoredLabels = configs.asSequence()
            .filter { it.isIgnored }
            .map { it.name }
            .toSet()
        val isSearching = query.isNotEmpty()
        val today = LocalDate.now()
        
        try {
            list.asSequence()
                .filter { contact ->
                    // Verbergen, wenn eines der Labels auf "Ignorieren" steht
                    // ABER: Wenn gesucht wird, zeigen wir auch ignorierte Kontakte an
                    val isIgnored = contact.labels.any { it in ignoredLabels }
                    if (isIgnored && !isSearching) return@filter false

                    val matchesQuery = query.isEmpty() || contact.fullName.contains(query, ignoreCase = true)
                    val matchesLabel = (label == null) || contact.labels.contains(label)
                    matchesQuery && matchesLabel
                }
                .mapNotNull { contact -> 
                    try {
                        contact.toUiModel(today)
                    } catch (e: Exception) {
                        Log.e("BirthdayViewModel", "Error mapping contact ${contact.contactId}", e)
                        null
                    }
                }
                .sortedBy { it.daysUntilNext }
                .toList()
        } catch (e: Exception) {
            Log.e("BirthdayViewModel", "Error in contacts flow", e)
            emptyList()
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null,
    )

    fun onSearchQueryChange(newQuery: String) {
        val wasEmpty = _searchQuery.value.isEmpty()
        _searchQuery.value = newQuery
        // Scroll nach oben, wenn die Suche gelöscht wurde
        if (newQuery.isEmpty() && !wasEmpty) {
            triggerScrollToTop()
        }
    }

    fun onLabelSelected(label: String?) {
        _selectedLabel.value = if (_selectedLabel.value == label) null else label
        triggerScrollToTop()
    }

    /**
     * Alle Labels inkl. ihrer Konfiguration für die Einstellungen.
     */
    val labelManagementList: StateFlow<List<LabelManagementModel>> = combine(
        contactDao.getAllContacts(),
        labelConfigDao.getAllConfigs(),
    ) { contacts, configs ->
        val allLabelNames = contacts.asSequence()
            .flatMap { it.labels }
            .distinct()
            .sorted()
            .toList()
        val configMap = configs.associateBy { it.name }
        
        allLabelNames.map { name ->
            val config = configMap[name]
            LabelManagementModel(
                name = name,
                isHiddenFromFilter = config?.isHiddenFromFilter ?: false,
                isIgnored = config?.isIgnored ?: false,
                isSystem = config?.isSystem ?: false,
            )
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    fun updateLabelConfig(name: String, isHiddenFromFilter: Boolean, isIgnored: Boolean, isSystem: Boolean) {
        viewModelScope.launch {
            labelConfigDao.insertConfig(
                LabelConfig(name, isHiddenFromFilter, isIgnored, isSystem)
            )
            try {
                BirthdayWidget().updateAll(getApplication())
            } catch (e: Exception) {
                Log.e("BirthdayViewModel", "Widget update failed in updateLabelConfig", e)
            }
        }
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
                    if (systemContacts.isEmpty()) {
                        contactDao.deleteAllContacts()
                        return@withContext
                    }

                    val contactGroups = fetchContactGroups()
                    
                    // Label-Konfigurationen in der DB aktualisieren (isSystem Status)
                    val allConfigs = labelConfigDao.getAllConfigsImmediate().associateBy { it.name }
                    contactGroups.values.distinctBy { it.title }.forEach { group ->
                        val existing = allConfigs[group.title]
                        if ((existing == null) || (existing.isSystem != group.isSystem)) {
                            labelConfigDao.insertConfig(
                                LabelConfig(
                                    name = group.title,
                                    isHiddenFromFilter = existing?.isHiddenFromFilter ?: false,
                                    isIgnored = existing?.isIgnored ?: false,
                                    isSystem = group.isSystem
                                )
                            )
                        }
                    }

                    val labelsMap = fetchLabelsForContacts(
                        systemContacts.asSequence().map { it.contactId }.toSet(),
                        contactGroups
                    )
                    
                    // Bestehende Geschenkideen erhalten
                    val existingGiftIdeas: Map<String, String?> = contactDao.getAllContactsImmediate()
                        .associateBy({ it.lookupKey }, { it.giftIdeas })

                    val updatedContacts = systemContacts.map { contact ->
                        contact.copy(
                            labels = labelsMap[contact.contactId] ?: emptyList(),
                            giftIdeas = existingGiftIdeas[contact.lookupKey]
                        )
                    }
                    
                    // Transaktionaler Sync zur Vermeidung von UI-Flimmern
                    contactDao.refreshContacts(updatedContacts)
                } catch (e: Exception) {
                    Log.e("BirthdayViewModel", "Sync failed", e)
                }
            }
            try {
                BirthdayWidget().updateAll(getApplication())
            } catch (e: Exception) {
                Log.e("BirthdayViewModel", "Widget update failed", e)
            }
        }
    }

    private fun Contact.toUiModel(today: LocalDate): ContactUiModel {
        val hasYear = birthday.year != 1900
        val nextBirthday = birthday.toNextOccurrence(today)
        val daysLeft = ChronoUnit.DAYS.between(today, nextBirthday)
        val nextAgeValue = if (hasYear) nextBirthday.year - birthday.year else null

        return ContactUiModel(
            id = lookupKey, 
            contactId = contactId,
            lookupKey = lookupKey,
            fullName = fullName,
            dateText = if (!hasYear) birthday.format(dayMonthFormatter) else birthday.format(dateFormatter),
            monthName = birthday.format(monthFormatter),
            imageUri = imageUri,
            initials = fullName.take(1).uppercase(),
            nextAge = nextAgeValue,
            nextAgeText = nextAgeValue?.let { "wird $it" },
            daysUntilNext = daysLeft,
            daysLeftText = if (daysLeft == 0L) "Heute!" else "In $daysLeft T.",
            isToday = daysLeft == 0L,
            giftIdeas = giftIdeas,
        )
    }

    private suspend fun fetchLabelsForContacts(contactIds: Set<String>, groupsMap: Map<Long, GroupInfo>): Map<String, List<String>> = withContext(Dispatchers.IO) {
        val labelsMap = mutableMapOf<String, MutableList<String>>()
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID
        )
        val selection = "${ContactsContract.Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
        
        try {
            getApplication<Application>().contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null,
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                val groupRowIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)
                
                if ((idIdx != -1) && (groupRowIdIdx != -1)) {
                    while (cursor.moveToNext()) {
                        val contactId = cursor.getString(idIdx)
                        if (contactId in contactIds) {
                            val groupId = cursor.getLong(groupRowIdIdx)
                            groupsMap[groupId]?.let { groupInfo ->
                                labelsMap.getOrPut(contactId) { mutableListOf() }.add(groupInfo.title)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BirthdayViewModel", "Error fetching labels", e)
        }
        labelsMap.mapValues { it.value.distinct() }
    }

    private suspend fun fetchContactGroups(): Map<Long, GroupInfo> = withContext(Dispatchers.IO) {
        val groupsMap = mutableMapOf<Long, GroupInfo>()
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
                    val title = cursor.getString(titleIdx)
                    
                    if (!title.isNullOrBlank()) {
                        val lowerTitle = title.lowercase()
                        val isRedundant = (lowerTitle == "my contacts") || 
                                         (lowerTitle == "contacts") ||
                                         (lowerTitle == "ich") ||
                                         (lowerTitle == "me")
                                     
                        if (!isRedundant) {
                            groupsMap[cursor.getLong(idIdx)] = GroupInfo(
                                title = title,
                                isSystem = systemId != null
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BirthdayViewModel", "Error in fetchContactGroups", e)
        }
        groupsMap
    }

    private suspend fun fetchBirthdaysFromSystem(): List<Contact> = withContext(Dispatchers.IO) {
        val contactsMap = mutableMapOf<String, Contact>()
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.LOOKUP_KEY,
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
                val lookupIdx = cursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY)
                val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val dateIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE)
                val photoIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

                if ((idIdx == -1) || (lookupIdx == -1) || (nameIdx == -1) || (dateIdx == -1) || (photoIdx == -1)) return@withContext emptyList()

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIdx) ?: continue
                    val lookupKey = cursor.getString(lookupIdx) ?: ""
                    parseDate(cursor.getString(dateIdx))?.let { birthday ->
                        contactsMap[id] = Contact(
                            contactId = id,
                            lookupKey = lookupKey,
                            fullName = cursor.getString(nameIdx) ?: "Unbekannt",
                            birthday = birthday,
                            imageUri = cursor.getString(photoIdx),
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BirthdayViewModel", "Error in fetchBirthdaysFromSystem", e)
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
    val today = LocalDate.now()
    return ChronoUnit.DAYS.between(today, toNextOccurrence(today))
}

fun LocalDate.safeNextAge(): Int {
    val today = LocalDate.now()
    return toNextOccurrence(today).year - this.year
}

/**
 * Hilfsfunktion um das nächste Vorkommen eines Datums zu finden (Handling für 29. Feb).
 */
fun LocalDate.toNextOccurrence(today: LocalDate): LocalDate {
    var next = this.toYear(today.year)
    if (next.isBefore(today)) {
        next = this.toYear(today.year + 1)
    }
    return next
}

private fun LocalDate.toYear(targetYear: Int): LocalDate {
    return if ((this.monthValue == 2) && (this.dayOfMonth == 29) && !java.time.Year.isLeap(targetYear.toLong())) {
        LocalDate.of(targetYear, 2, 28)
    } else {
        this.withYear(targetYear)
    }
}
