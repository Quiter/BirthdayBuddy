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

    val notificationsEnabled: StateFlow<Boolean> = preferenceRepository.notificationsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    val notificationHour: StateFlow<Int> = preferenceRepository.notificationHour
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 9,
        )

    val notificationMinute: StateFlow<Int> = preferenceRepository.notificationMinute
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
                    NotificationWorker.enqueueDailyNotification(context, hour, minute)
                } else {
                    NotificationWorker.cancelNotification(context)
                }
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.setNotificationsEnabled(enabled)
        }
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferenceRepository.setNotificationTime(hour, minute)
        }
    }

    val swipeHintShown: StateFlow<Boolean> = preferenceRepository.swipeHintShown
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true,
        )

    fun setSwipeHintShown() {
        viewModelScope.launch {
            preferenceRepository.setSwipeHintShown(shown = true)
        }
    }

    fun updateGiftIdeas(lookupKey: String, ideas: String) {
        viewModelScope.launch {
            contactRepository.updateGiftIdeas(lookupKey, ideas)
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

    private val _isFastScrolling = MutableStateFlow(value = false)
    val isFastScrolling: StateFlow<Boolean> = _isFastScrolling.asStateFlow()

    fun setFastScrolling(isScrolling: Boolean) {
        _isFastScrolling.value = isScrolling
    }

    val availableLabels: StateFlow<List<String>> = combine(
        contactRepository.allContacts,
        contactRepository.labelConfigs,
    ) { contacts, configs ->
        val inUseLabels = contacts.asSequence().flatMap { it.labels }.toSet()
        val configMap = configs.associateBy { it.name }
        
        val hasUserLabels = inUseLabels.any { name ->
            configMap[name]?.isSystem == false
        }

        if (!hasUserLabels) return@combine emptyList()

        inUseLabels.asSequence()
            .filter { name ->
                val config = configMap[name]
                !(config?.isHiddenFromFilter ?: false) && !(config?.isIgnored ?: false)
            }
            .sorted()
            .toList()
    }
    .flowOn(Dispatchers.Default)
    .distinctUntilChanged()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    val contacts: StateFlow<List<ContactUiModel>?> = combine(
        contactRepository.allContacts,
        _searchQuery,
        _selectedLabel,
        contactRepository.labelConfigs,
        timeRepository.currentDate,
    ) { list, query, label, configs, today ->
        val ignoredLabels = configs.asSequence()
            .filter { it.isIgnored }
            .map { it.name }
            .toSet()
        val isSearching = query.isNotEmpty()
        
        try {
            list.asSequence()
                .filter { contact ->
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
    .distinctUntilChanged()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null,
    )

    fun onSearchQueryChange(newQuery: String) {
        val wasEmpty = _searchQuery.value.isEmpty()
        _searchQuery.value = newQuery
        if (newQuery.isEmpty() && !wasEmpty) {
            triggerScrollToTop()
        }
    }

    fun onLabelSelected(label: String?) {
        _selectedLabel.value = if (_selectedLabel.value == label) null else label
        triggerScrollToTop()
    }

    val labelManagementList: StateFlow<List<LabelManagementModel>> = contactRepository.labelConfigs.map { configs ->
        configs.asSequence().map { config ->
            LabelManagementModel(
                name = config.name,
                isHiddenFromFilter = config.isHiddenFromFilter,
                isIgnored = config.isIgnored,
                isSystem = config.isSystem,
            )
        }.sortedBy { it.name }.toList()
    }
    .flowOn(Dispatchers.Default)
    .distinctUntilChanged()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    fun updateLabelConfig(name: String, isHiddenFromFilter: Boolean, isIgnored: Boolean, isSystem: Boolean) {
        viewModelScope.launch {
            contactRepository.updateLabelConfig(
                LabelConfig(name, isHiddenFromFilter, isIgnored, isSystem),
            )
            try {
                BirthdayWidget().updateAll(context)
            } catch (e: Exception) {
                Log.e("BirthdayViewModel", "Widget update failed", e)
            }
        }
    }

    fun triggerScrollToTop() {
        viewModelScope.launch {
            _scrollToTopEvent.emit(Unit)
        }
    }

    fun syncContacts() {
        viewModelScope.launch {
            contactRepository.syncContacts()
            try {
                BirthdayWidget().updateAll(context)
            } catch (e: Exception) {
                Log.e("BirthdayViewModel", "Widget update failed", e)
            }
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
            giftIdeas = GiftIdea.fromString(giftIdeas),
        )
    }
}
