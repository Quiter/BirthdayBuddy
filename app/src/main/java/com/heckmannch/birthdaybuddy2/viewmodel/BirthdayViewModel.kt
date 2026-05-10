package com.heckmannch.birthdaybuddy2.viewmodel

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy2.database.LabelConfig
import com.heckmannch.birthdaybuddy2.repository.ContactRepository
import com.heckmannch.birthdaybuddy2.repository.PreferenceRepository
import com.heckmannch.birthdaybuddy2.repository.TimeRepository
import com.heckmannch.birthdaybuddy2.ui.screens.settings.notifications.NotificationWorker
import com.heckmannch.birthdaybuddy2.util.toNextOccurrence
import com.heckmannch.birthdaybuddy2.widget.BirthdayWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class BirthdayViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository,
    private val preferenceRepository: PreferenceRepository,
    timeRepository: TimeRepository,
) : ViewModel() {

    // --- Settings & Preferences ---

    val notificationsEnabled: StateFlow<Boolean> = preferenceRepository.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val notificationHour: StateFlow<Int> = preferenceRepository.notificationHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 9)

    val notificationMinute: StateFlow<Int> = preferenceRepository.notificationMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Automatische Worker-Synchronisation bei Einstellungsänderungen
        viewModelScope.launch {
            combine(notificationsEnabled, notificationHour, notificationMinute) { enabled, hour, minute ->
                Triple(enabled, hour, minute)
            }.collect { (enabled, hour, minute) ->
                if (enabled) {
                    NotificationWorker.enqueueDailyNotification(context, hour, minute)
                } else {
                    NotificationWorker.cancelNotification(context)
                }
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        preferenceRepository.setNotificationsEnabled(enabled)
    }

    fun setNotificationTime(hour: Int, minute: Int) = viewModelScope.launch {
        preferenceRepository.setNotificationTime(hour, minute)
    }

    val swipeHintShown: StateFlow<Boolean> = preferenceRepository.swipeHintShown
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = true)

    fun setSwipeHintShown() = viewModelScope.launch {
        preferenceRepository.setSwipeHintShown(shown = true)
    }

    // --- Search & Filter State ---

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedLabel = MutableStateFlow<String?>(null)
    val selectedLabel: StateFlow<String?> = _selectedLabel.asStateFlow()

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(replay = 0)
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    private val _isFastScrolling = MutableStateFlow(false)
    val isFastScrolling: StateFlow<Boolean> = _isFastScrolling.asStateFlow()

    fun setFastScrolling(isScrolling: Boolean) { _isFastScrolling.value = isScrolling }

    // --- Data Processing (Optimized) ---

    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    private val dayMonthFormatter = DateTimeFormatter.ofPattern("d. MMMM")
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM")

    /**
     * Schritt 1: Basale Umwandlung der DB-Modelle in UI-Modelle (Reaktive Zeitquelle einbezogen).
     * Wird nur ausgeführt, wenn sich die DB-Daten oder das Datum ändern.
     */
    private val allUiContacts: Flow<List<ContactUiModel>> = combine(
        contactRepository.allContacts,
        timeRepository.currentDate,
    ) { list, today ->
        list.asSequence()
            .map { it.toUiModel(today) }
            .sortedBy { it.daysUntilNext }
            .toList()
    }.flowOn(Dispatchers.Default)

    /**
     * Schritt 2: Filtern der fertigen UI-Modelle basierend auf Suche und Labels.
     * Extrem performant beim Tippen, da keine Datumsberechnungen mehr stattfinden.
     */
    val contacts: StateFlow<List<ContactUiModel>?> = combine(
        allUiContacts,
        _searchQuery,
        _selectedLabel,
        contactRepository.labelConfigs,
    ) { uiList, query, label, configs ->
        val ignoredLabels = configs.asSequence().filter { it.isIgnored }.map { it.name }.toSet()
        val isSearching = query.isNotEmpty()

        uiList.asSequence()
            .filter { contact ->
                val isIgnored = contact.labels.any { it in ignoredLabels }
                if (isIgnored && !isSearching) return@filter false

                val matchesQuery = query.isEmpty() || contact.fullName.contains(query, ignoreCase = true)
                val matchesLabel = (label == null) || contact.labels.contains(label)
                matchesQuery && matchesLabel
            }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val availableLabels: StateFlow<List<String>> = combine(
        contactRepository.allContacts,
        contactRepository.labelConfigs,
    ) { contacts, configs ->
        val inUseLabels = contacts.asSequence().flatMap { it.labels }.toSet()
        val configMap = configs.associateBy { it.name }
        
        if (inUseLabels.none { configMap[it]?.isSystem == false }) return@combine emptyList()

        inUseLabels.asSequence()
            .filter { name ->
                val config = configMap[name]
                !(config?.isHiddenFromFilter ?: false) && !(config?.isIgnored ?: false)
            }
            .sorted()
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val labelManagementList: StateFlow<List<LabelManagementModel>> = combine(
        contactRepository.labelConfigs,
        contactRepository.allContacts
    ) { configs, contacts ->
        val labelsInUse = contacts.asSequence().flatMap { it.labels }.toSet()
        configs.asSequence()
            .filter { it.name in labelsInUse }
            .map { config ->
                LabelManagementModel(
                    config.name,
                    config.isHiddenFromFilter,
                    config.isIgnored,
                    config.isSystem
                )
            }.sortedBy { it.name }.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Actions ---

    fun onSearchQueryChange(newQuery: String) {
        val wasEmpty = _searchQuery.value.isEmpty()
        // Falls eine neue Suche gestartet wird, aktives Label zurücksetzen (Global Search)
        if (newQuery.isNotEmpty() && wasEmpty) {
            _selectedLabel.value = null
        }
        _searchQuery.value = newQuery
        if (newQuery.isEmpty() && !wasEmpty) triggerScrollToTop()
    }

    fun onLabelSelected(label: String?) {
        _selectedLabel.value = if (_selectedLabel.value == label) null else label
        triggerScrollToTop()
    }

    fun resetFilters() {
        if ((_searchQuery.value.isNotEmpty()) || (_selectedLabel.value != null)) {
            _searchQuery.value = ""
            _selectedLabel.value = null
            triggerScrollToTop()
        }
    }

    fun updateGiftIdeas(lookupKey: String, ideas: String) = viewModelScope.launch {
        contactRepository.updateGiftIdeas(lookupKey, ideas)
    }

    fun updateLabelConfig(name: String, hidden: Boolean, ignored: Boolean, isSystem: Boolean) = viewModelScope.launch {
        contactRepository.updateLabelConfig(LabelConfig(name, hidden, ignored, isSystem))
        updateWidget()
    }

    fun syncContacts() = viewModelScope.launch {
        contactRepository.syncContacts()
        updateWidget()
    }

    fun triggerScrollToTop() = viewModelScope.launch {
        _scrollToTopEvent.emit(Unit)
    }

    suspend fun exportGiftIdeas() = contactRepository.exportGiftIdeas()

    suspend fun importGiftIdeas(json: String): Int {
        val count = contactRepository.importGiftIdeas(json)
        if (count > 0) updateWidget()
        return count
    }

    private fun updateWidget() = viewModelScope.launch {
        try {
            BirthdayWidget().updateAll(context)
        } catch (e: Exception) {
            Log.e("BirthdayViewModel", "Widget update failed", e)
        }
    }

    private fun com.heckmannch.birthdaybuddy2.database.Contact.toUiModel(today: LocalDate): ContactUiModel {
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
            labels = labels,
            giftIdeas = GiftIdea.fromString(giftIdeas),
        )
    }
}
