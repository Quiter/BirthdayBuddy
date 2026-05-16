package com.heckmannch.birthdaybuddy.viewmodel

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.heckmannch.birthdaybuddy.data.local.AppSettings
import com.heckmannch.birthdaybuddy.data.local.LabelConfig
import com.heckmannch.birthdaybuddy.data.local.NotificationRule
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.data.repository.TimeRepository
import com.heckmannch.birthdaybuddy.data.mapper.ContactMapper
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.model.LabelManagementModel
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

    // Zentraler Flow für App-Einstellungen zur Vermeidung mehrfacher DB-Abos
    private val settings = notificationRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val notificationsEnabled: StateFlow<Boolean> = settings
        .map { it.notificationsEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val persistentNotifications: StateFlow<Boolean> = settings
        .map { it.persistentNotifications }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationRules: StateFlow<List<NotificationRule>?> = notificationRepository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val onboardingCompleted: StateFlow<Boolean> = settings
        .map { it.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val swipeHintShown: StateFlow<Boolean> = settings
        .map { it.swipeHintShown }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        if (enabled) {
            val rules = notificationRepository.getAllRulesImmediate()
            if (rules.isEmpty()) {
                addNotificationRule(daysBefore = 0, hour = 9, minute = 0)
            }
        }
        notificationRepository.updateSettings(notificationsEnabled = enabled)
    }

    fun completeOnboarding(notificationsEnabled: Boolean) = viewModelScope.launch {
        if (notificationsEnabled) {
            val rules = notificationRepository.getAllRulesImmediate()
            if (rules.isEmpty()) {
                addNotificationRule(daysBefore = 0, hour = 9, minute = 0)
            }
        }
        notificationRepository.updateSettings(
            notificationsEnabled = notificationsEnabled,
            onboardingCompleted = true,
        )
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

    fun setSwipeHintShown() = viewModelScope.launch {
        notificationRepository.updateSettings(swipeHintShown = true)
    }

    // --- Search & Filter State ---

    private val _searchQuery = MutableStateFlow("")
    private val _selectedLabel = MutableStateFlow<String?>(null)
    private val _isResettingFilter = MutableStateFlow(value = false)
    private val _isSyncing = MutableStateFlow(value = false)
    private val _searchFocusRequested = MutableStateFlow(value = false)

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(replay = 0)
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    fun setIsResettingFilter(isResetting: Boolean) { _isResettingFilter.value = isResetting }

    // --- Data Processing ---

    private val ignoredLabels: Flow<Set<String>> = contactRepository.labelConfigs
        .map { configs -> 
            configs.asSequence()
                .filter { it.isIgnored }
                .map { it.name }
                .toSet() 
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

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
        // Automatische Worker-Synchronisation
        viewModelScope.launch {
            combine(notificationsEnabled, notificationRules) { enabled, rules ->
                enabled to rules
            }.collect { (enabled, rules) ->
                if (rules == null) return@collect 
                
                if (enabled && rules.isNotEmpty()) {
                    NotificationWorker.scheduleNext(context, rules)
                } else {
                    NotificationWorker.cancelNotification(context)
                }
            }
        }

        // Pre-fetch der ersten Kontaktbilder
        viewModelScope.launch {
            allUiContacts.filter { it.isNotEmpty() }.first().take(20).forEach { contact ->
                if (contact.imageUri != null) {
                    val request = ImageRequest.Builder(context)
                        .data(contact.imageUri)
                        .size(150)
                        .build()
                    context.imageLoader.enqueue(request)
                }
            }
        }
    }

    /**
     * Filtern der fertigen UI-Modelle basierend auf Suche und Labels.
     * Optimierung: Such-Keywords werden nur berechnet, wenn sich die Suche ändert.
     */
    private val searchKeywords = _searchQuery
        .map { it.trim() }
        .distinctUntilChanged()
        .map { if (it.isEmpty()) emptyList() else it.split("\\s+".toRegex()) }
        .flowOn(Dispatchers.Default)

    private val filteredContacts: Flow<List<ContactUiModel>?> = combine(
        allUiContacts,
        searchKeywords,
        _selectedLabel,
        ignoredLabels,
    ) { uiList, keywords, label, ignoredLabels ->
        val isSearching = keywords.isNotEmpty()
        val startTime = System.currentTimeMillis()

        val result = uiList.asSequence()
            .filter { contact ->
                val isIgnored = contact.labels.any { it in ignoredLabels }
                if (isIgnored && !isSearching) return@filter false

                val matchesQuery = !isSearching || keywords.all { keyword ->
                    contact.fullName.contains(keyword, ignoreCase = true)
                }
                val matchesLabel = (label == null) || contact.labels.contains(label)
                matchesQuery && matchesLabel
            }
            .toList()
        
        if (uiList.size > 1000) {
            Log.d("BirthdayViewModel", "Filtering ${uiList.size} contacts took ${System.currentTimeMillis() - startTime}ms")
        }
        result
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

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<HomeUiState> = combine(
        filteredContacts,
        _searchQuery,
        _selectedLabel,
        _isResettingFilter,
        _isSyncing,
        availableLabels,
        swipeHintShown,
        _searchFocusRequested,
    ) { flows ->
        HomeUiState(
            contacts = flows[0] as List<ContactUiModel>?,
            searchQuery = flows[1] as String,
            selectedLabel = flows[2] as String?,
            isResettingFilter = flows[3] as Boolean,
            isSyncing = flows[4] as Boolean,
            availableLabels = flows[5] as List<String>,
            swipeHintShown = flows[6] as Boolean,
            searchFocusRequested = flows[7] as Boolean,
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

    fun syncContacts(showLoading: Boolean = false) = viewModelScope.launch {
        if (showLoading) _isSyncing.value = true
        val startTime = System.currentTimeMillis()
        
        contactRepository.syncContacts()
        updateWidget()
        
        if (showLoading) {
            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime < 800) {
                delay(800 - elapsedTime)
            }
            _isSyncing.value = false
        }
    }

    fun triggerScrollToTop() = viewModelScope.launch {
        _scrollToTopEvent.emit(Unit)
    }

    fun triggerSearchFocus() {
        _searchFocusRequested.value = true
    }

    fun consumeSearchFocus() {
        _searchFocusRequested.value = false
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
