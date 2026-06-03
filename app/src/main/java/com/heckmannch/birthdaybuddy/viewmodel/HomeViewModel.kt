package com.heckmannch.birthdaybuddy.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.data.mapper.ContactMapper
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.data.repository.TimeRepository
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.GiftIdea
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val mapper: ContactMapper,
    timeRepository: TimeRepository,
) : ViewModel() {

    // --- Search & Filter State ---
    private val _searchQuery = MutableStateFlow("")
    private val _selectedLabel = MutableStateFlow<String?>(null)
    private val _isResettingFilter = MutableStateFlow(value = false)
    private val _isSyncing = MutableStateFlow(value = false)
    private val _searchFocusRequested = MutableStateFlow(value = false)
    private val _newlyAddedIdeaId = MutableStateFlow<String?>(null)

    companion object {
        const val LABEL_NO_BIRTHDAY = "special:no_birthday"
        const val LABEL_ANNIVERSARY = "special:anniversary"
        const val LABEL_NAME_DAY = "special:name_day"
    }

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(replay = 0)
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    private val _syncCompletedEvent = MutableSharedFlow<Unit>(replay = 0)
    val syncCompletedEvent: SharedFlow<Unit> = _syncCompletedEvent.asSharedFlow()

    fun setIsResettingFilter(isResetting: Boolean) {
        _isResettingFilter.value = isResetting
    }

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

    init {
        syncContacts()
    }

    private val searchKeywords = _searchQuery
        .map { it.trim() }
        .distinctUntilChanged()
        .map { if (it.isEmpty()) emptyList() else it.split("\\s+".toRegex()) }
        .flowOn(Dispatchers.Default)

    private val filteredContacts: Flow<List<ContactUiModel>?> = combine(
        contactRepository.allContacts,
        timeRepository.currentDate,
        searchKeywords,
        _selectedLabel,
        ignoredLabels,
    ) { rawContacts, today, keywords, label, ignoredLabels ->
        val startTime = System.currentTimeMillis()

        val displayEventType = when (label) {
            LABEL_ANNIVERSARY -> "anniversary"
            LABEL_NAME_DAY -> "name_day"
            else -> "birthday"
        }

        val uiList = rawContacts.asSequence()
            .map { mapper.toUiModelForEvent(it, today, displayEventType) }
            .toList()

        val result = uiList.asSequence()
            .filter { shouldShowContact(it, keywords, label, ignoredLabels, displayEventType) }
            .sortedWith(
                compareBy<ContactUiModel> { it.daysUntilNext }
                    .thenBy { it.fullName },
            )
            .toList()

        if (uiList.size > 1000) {
            Log.d(
                "HomeViewModel",
                "Filtering ${uiList.size} contacts took ${System.currentTimeMillis() - startTime}ms"
            )
        }
        result
    }.flowOn(Dispatchers.Default)

    /**
     * Zentrale Filter-Logik für Kontakte.
     */
    private fun shouldShowContact(
        contact: ContactUiModel,
        keywords: List<String>,
        label: String?,
        ignoredLabels: Set<String>,
        displayEventType: String
    ): Boolean {
        val isSearching = keywords.isNotEmpty()

        // 1. Sichtbarkeit prüfen (Ereignis vorhanden & Ignoriert-Status)
        val isMissingEvent = contact.dateText == "-"
        val isNoBirthdayFilter = label == LABEL_NO_BIRTHDAY

        // Ereignislose Kontakte ausblenden (außer bei Suche, sofern es sich um Geburtstage handelt.
        // Für Hochzeitstag und Namenstag blenden wir Kontakte ohne dieses Ereignis IMMER aus!)
        if (isMissingEvent) {
            if (displayEventType != "birthday") return false
            if (!isSearching && !isNoBirthdayFilter) return false
        }

        // Ignorierte Labels ausblenden (außer bei aktiver Suche)
        val isIgnored = contact.labels.any { it in ignoredLabels }
        if (isIgnored && !isSearching) return false

        // 2. Suche (Keywords)
        val matchesQuery = !isSearching || keywords.all { keyword ->
            contact.fullName.contains(keyword, ignoreCase = true)
        }
        if (!matchesQuery) return false

        // 3. Label-Filter
        return when (label) {
            null -> true
            LABEL_NO_BIRTHDAY -> isMissingEvent
            LABEL_ANNIVERSARY -> true // bereits oben über isMissingEvent und displayEventType gefiltert
            LABEL_NAME_DAY -> true    // bereits oben über isMissingEvent und displayEventType gefiltert
            else -> contact.labels.contains(label)
        }
    }

    val availableLabels: Flow<List<String>> = combine(
        contactRepository.allContacts,
        contactRepository.labelConfigs,
        contactRepository.otherEventsEnabled,
    ) { contacts, configs, otherEventsEnabled ->
        val inUseLabels = contacts.asSequence().flatMap { it.labels }.toSet()
        val configMap = configs.associateBy { it.name }

        // Pseudo-Label "Ohne Datum" Konfiguration laden und Sichtbarkeit prüfen
        val pseudoConfig = configMap[LABEL_NO_BIRTHDAY]
        val showPseudo = contacts.any { it.birthday == null } &&
                pseudoConfig?.isHiddenFromFilter != true &&
                pseudoConfig?.isIgnored != true

        // Prüfen, ob aktive, nicht-versteckte User-Labels vorhanden sind
        val hasActiveUserLabels = inUseLabels.any { name ->
            val config = configMap[name]
            config?.isSystem == false && !(config.isHiddenFromFilter) && !(config.isIgnored) && name != LABEL_NO_BIRTHDAY
        }

        val showAnniversary = otherEventsEnabled && contacts.any { it.anniversary != null }
        val showNameDay = otherEventsEnabled && contacts.any { it.nameDay != null }

        // Wenn weder aktive User-Labels noch das Pseudo-Label noch andere Events aktiv sind -> Bar verstecken
        if (!hasActiveUserLabels && !showPseudo && !showAnniversary && !showNameDay) return@combine emptyList()

        val labels = mutableListOf<String>()

        // Zuerst die User-Label
        if (hasActiveUserLabels) {
            inUseLabels.asSequence()
                .filter { name ->
                    val config = configMap[name]
                    (config?.isSystem == false) && !(config.isHiddenFromFilter) && !(config.isIgnored) && name != LABEL_NO_BIRTHDAY
                }
                .sorted()
                .forEach { labels.add(it) }
        }

        // "Ohne Datum" immer als letztes von Geburtstagen, falls aktiv
        if (showPseudo) {
            labels.add(LABEL_NO_BIRTHDAY)
        }

        // Weitere Ereignisse ganz rechts
        if (showAnniversary) {
            labels.add(LABEL_ANNIVERSARY)
        }
        if (showNameDay) {
            labels.add(LABEL_NAME_DAY)
        }

        labels
    }

    private data class FilterCriteria(
        val searchQuery: String = "",
        val selectedLabel: String? = null,
        val isResettingFilter: Boolean = false
    )

    private val filterCriteria: Flow<FilterCriteria> = combine(
        _searchQuery,
        _selectedLabel,
        _isResettingFilter,
    ) { query, label, resetting ->
        FilterCriteria(query, label, resetting)
    }

    private data class FilterUiFlags(
        val isSyncing: Boolean = false,
        val searchFocusRequested: Boolean = false,
        val newlyAddedIdeaId: String? = null
    )

    private val filterUiFlags: Flow<FilterUiFlags> = combine(
        _isSyncing,
        _searchFocusRequested,
        _newlyAddedIdeaId,
    ) { syncing, focus, newlyAdded ->
        FilterUiFlags(syncing, focus, newlyAdded)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        filteredContacts,
        availableLabels,
        filterCriteria,
        filterUiFlags
    ) { contacts, labels, criteria, flags ->
        HomeUiState(
            contacts = contacts,
            availableLabels = labels,
            searchQuery = criteria.searchQuery,
            selectedLabel = criteria.selectedLabel,
            isResettingFilter = criteria.isResettingFilter,
            isSyncing = flags.isSyncing,
            searchFocusRequested = flags.searchFocusRequested,
            newlyAddedIdeaId = flags.newlyAddedIdeaId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    // --- Actions ---
    fun onSearchQueryChange(newQuery: String) {
        if (_searchQuery.value == newQuery) return

        if (newQuery.isNotEmpty() && _searchQuery.value.isEmpty()) {
            _selectedLabel.value = null
        }

        _searchQuery.value = newQuery
        requestFilterReset()
    }

    fun onLabelSelected(label: String?) {
        val newLabel = if (_selectedLabel.value == label) null else label
        if (_selectedLabel.value != newLabel) {
            _selectedLabel.value = newLabel
            requestFilterReset()
        }
    }

    fun resetFilters() {
        if (_searchQuery.value.isNotEmpty() || _selectedLabel.value != null) {
            _searchQuery.value = ""
            _selectedLabel.value = null
            requestFilterReset()
        }
    }

    /**
     * Triggert einen Scroll-Reset und setzt den Filter-Status zurück.
     */
    private fun requestFilterReset() {
        _isResettingFilter.value = true
        triggerScrollToTop()
    }

    fun addGiftIdea(lookupKey: String) = viewModelScope.launch {
        val newIdea = GiftIdea(text = "")
        _newlyAddedIdeaId.value = newIdea.id
        contactRepository.addGiftIdea(lookupKey, newIdea)
    }

    fun toggleGiftIdea(lookupKey: String, idea: GiftIdea, isChecked: Boolean) =
        viewModelScope.launch {
            contactRepository.toggleGiftIdea(lookupKey, idea, isChecked)
        }

    fun deleteGiftIdea(lookupKey: String, ideaId: String) = viewModelScope.launch {
        contactRepository.deleteGiftIdea(lookupKey, ideaId)
    }

    fun updateGiftIdeaText(lookupKey: String, ideaId: String, newText: String) =
        viewModelScope.launch {
            contactRepository.updateGiftIdeaText(lookupKey, ideaId, newText)
        }

    fun updateBirthday(contactId: String, birthday: java.time.LocalDate) = viewModelScope.launch {
        contactRepository.updateContactBirthday(contactId, birthday)
    }

    fun syncContacts(showLoading: Boolean = false) = viewModelScope.launch {
        if (showLoading) _isSyncing.value = true
        val startTime = System.currentTimeMillis()

        contactRepository.syncContacts()

        if (showLoading) {
            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime < 800) {
                delay((800 - elapsedTime).milliseconds)
            }
            _isSyncing.value = false
            _syncCompletedEvent.emit(Unit)
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
}
