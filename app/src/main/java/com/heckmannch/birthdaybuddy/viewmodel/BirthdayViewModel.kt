package com.heckmannch.birthdaybuddy.viewmodel

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.heckmannch.birthdaybuddy.database.LabelConfig
import com.heckmannch.birthdaybuddy.database.NotificationRule
import com.heckmannch.birthdaybuddy.repository.ContactRepository
import com.heckmannch.birthdaybuddy.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.repository.TimeRepository
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.components.NotificationWorker
import com.heckmannch.birthdaybuddy.widget.BirthdayWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BirthdayViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository,
    private val notificationRepository: NotificationRepository,
    private val mapper: ContactMapper,
    timeRepository: TimeRepository,
) : ViewModel() {

    // --- Settings & Preferences ---

    val notificationsEnabled: StateFlow<Boolean> = notificationRepository.settings
        .map { it.notificationsEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val persistentNotifications: StateFlow<Boolean> = notificationRepository.settings
        .map { it.persistentNotifications }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = true)

    val notificationRules: StateFlow<List<NotificationRule>?> = notificationRepository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val onboardingCompleted: StateFlow<Boolean> = notificationRepository.settings
        .map { it.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = true)

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        // Wir fügen eine Standard-Regel hinzu, falls noch keine existiert und eingeschaltet wird
        val rules = notificationRules.value
        if (enabled && (rules != null) && rules.isEmpty()) {
            addNotificationRule(daysBefore = 0, hour = 9, minute = 0)
        }
        notificationRepository.updateSettings(notificationsEnabled = enabled)
    }

    fun setOnboardingCompleted(completed: Boolean) = viewModelScope.launch {
        notificationRepository.updateSettings(onboardingCompleted = completed)
    }

    fun setPersistentNotifications(persistent: Boolean) = viewModelScope.launch {
        notificationRepository.updateSettings(persistentNotifications = persistent)
    }

    fun addNotificationRule(daysBefore: Int, hour: Int, minute: Int) = viewModelScope.launch {
        notificationRepository.insertRule(NotificationRule(daysBefore = daysBefore, hour = hour, minute = minute))
    }

    fun updateNotificationRule(rule: NotificationRule) = viewModelScope.launch {
        notificationRepository.updateRule(rule)
    }

    fun deleteNotificationRule(rule: NotificationRule) = viewModelScope.launch {
        notificationRepository.deleteRule(rule)
    }

    val swipeHintShown: StateFlow<Boolean> = notificationRepository.settings
        .map { it.swipeHintShown }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = true)

    fun setSwipeHintShown() = viewModelScope.launch {
        notificationRepository.updateSettings(swipeHintShown = true)
    }

    // --- Search & Filter State ---

    private val _searchQuery = MutableStateFlow("")
    private val _selectedLabel = MutableStateFlow<String?>(null)
    private val _isResettingFilter = MutableStateFlow(value = false)
    private val _isSyncing = MutableStateFlow(value = false)

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(replay = 0)
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    fun setIsResettingFilter(isResetting: Boolean) { _isResettingFilter.value = isResetting }

    // --- Data Processing ---

    /**
     * Basale Umwandlung der DB-Modelle in UI-Modelle.
     */
    private val allUiContacts: Flow<List<ContactUiModel>> = combine(
        contactRepository.allContacts,
        timeRepository.currentDate,
    ) { list, today ->
        list.asSequence()
            .map { mapper.toUiModel(it, today) }
            .sortedBy { it.daysUntilNext }
            .toList()
    }.flowOn(Dispatchers.Default)

    init {
        // Automatische Worker-Synchronisation bei Einstellungsänderungen oder Regeländerungen
        viewModelScope.launch {
            combine(notificationsEnabled, notificationRules) { enabled, rules ->
                enabled to rules
            }.collect { (enabled, rules) ->
                if (rules == null) return@collect // Noch am Laden
                
                if (enabled && rules.isNotEmpty()) {
                    NotificationWorker.scheduleNext(context, rules)
                } else {
                    NotificationWorker.cancelNotification(context)
                }
            }
        }

        // Pre-fetch der ersten Kontaktbilder zur Vermeidung von Rucklern beim Start
        viewModelScope.launch {
            allUiContacts.filter { it.isNotEmpty() }.first().take(20).forEach { contact ->
                if (contact.imageUri != null) {
                    val request = ImageRequest.Builder(context)
                        .data(contact.imageUri)
                        .size(150) // Daumenwert für Thumbnails
                        .build()
                    context.imageLoader.enqueue(request)
                }
            }
        }
    }

    /**
     * Filtern der fertigen UI-Modelle basierend auf Suche und Labels.
     */
    private val filteredContacts: Flow<List<ContactUiModel>?> = combine(
        allUiContacts,
        _searchQuery,
        _selectedLabel,
        contactRepository.labelConfigs,
    ) { uiList, query, label, configs ->
        val ignoredLabels = configs.asSequence().filter { it.isIgnored }.map { it.name }.toSet()
        val trimmedQuery = query.trim()
        val isSearching = trimmedQuery.isNotEmpty()
        val searchKeywords = if (isSearching) trimmedQuery.split("\\s+".toRegex()) else emptyList()

        uiList.asSequence()
            .filter { contact ->
                val isIgnored = contact.labels.any { it in ignoredLabels }
                if (isIgnored && !isSearching) return@filter false

                val matchesQuery = !isSearching || searchKeywords.all { keyword ->
                    contact.fullName.contains(keyword, ignoreCase = true)
                }
                val matchesLabel = (label == null) || contact.labels.contains(label)
                matchesQuery && matchesLabel
            }
            .toList()
    }

    val availableLabels: Flow<List<String>> = combine(
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
    }

    /**
     * Gebündelter UI-State für den Home-Bildschirm.
     */
    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<HomeUiState> = combine(
        filteredContacts,
        _searchQuery,
        _selectedLabel,
        _isResettingFilter,
        _isSyncing,
        availableLabels,
        swipeHintShown,
    ) { flows ->
        HomeUiState(
            contacts = flows[0] as List<ContactUiModel>?,
            searchQuery = flows[1] as String,
            selectedLabel = flows[2] as String?,
            isResettingFilter = flows[3] as Boolean,
            isSyncing = flows[4] as Boolean,
            availableLabels = flows[5] as List<String>,
            swipeHintShown = flows[6] as Boolean,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    val labelManagementList: StateFlow<List<LabelManagementModel>> = combine(
        contactRepository.labelConfigs,
        contactRepository.allContacts,
    ) { configs, contacts ->
        val labelsInUse = contacts.asSequence().flatMap { it.labels }.toSet()
        configs.asSequence()
            .filter { it.name in labelsInUse }
            .map { config ->
                LabelManagementModel(
                    config.name,
                    config.isHiddenFromFilter,
                    config.isIgnored,
                    config.isSystem,
                )
            }.sortedBy { it.name }.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Actions ---

    fun onSearchQueryChange(newQuery: String) {
        val wasEmpty = _searchQuery.value.isEmpty()
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
        _isSyncing.value = true
        val startTime = System.currentTimeMillis()
        
        contactRepository.syncContacts()
        updateWidget()
        
        // Sicherstellen, dass der Ladekreis mindestens 800ms sichtbar ist (UX)
        val elapsedTime = System.currentTimeMillis() - startTime
        if (elapsedTime < 800) {
            delay(800 - elapsedTime)
        }
        _isSyncing.value = false
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
}
