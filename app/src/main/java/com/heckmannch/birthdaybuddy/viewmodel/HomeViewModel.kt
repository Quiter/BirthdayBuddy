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
import com.heckmannch.birthdaybuddy.util.ImagePrefetcher
import com.heckmannch.birthdaybuddy.util.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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
    private val widgetUpdater: WidgetUpdater,
    private val imagePrefetcher: ImagePrefetcher,
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
    }

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(replay = 0)
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

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

    private val allUiContacts: Flow<List<ContactUiModel>> = combine(
        contactRepository.allContacts,
        timeRepository.currentDate,
    ) { list, today ->
        list.asSequence()
            .map { mapper.toUiModel(it, today) }
            .sortedWith(
                compareBy<ContactUiModel> { it.daysUntilNext }
                    .thenBy { it.fullName },
            )
            .toList()
    }.flowOn(Dispatchers.Default)

    init {
        syncContacts()
        // Pre-fetch der ersten Kontaktbilder
        viewModelScope.launch {
            val contactUris = allUiContacts.filter { it.isNotEmpty() }
                .first()
                .take(20)
                .mapNotNull { it.imageUri }
            imagePrefetcher.prefetch(contactUris)
        }
    }

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
        val startTime = System.currentTimeMillis()

        val result = uiList.asSequence()
            .filter { shouldShowContact(it, keywords, label, ignoredLabels) }
            .toList()

        if (uiList.size > 1000) {
            Log.d(
                "HomeViewModel",
                "Filtering ${uiList.size} contacts took ${System.currentTimeMillis() - startTime}ms"
            )
        }
        result
    }

    /**
     * Zentrale Filter-Logik für Kontakte.
     */
    private fun shouldShowContact(
        contact: ContactUiModel,
        keywords: List<String>,
        label: String?,
        ignoredLabels: Set<String>
    ): Boolean {
        val isSearching = keywords.isNotEmpty()

        // 1. Sichtbarkeit prüfen (Geburtstag vorhanden & Ignoriert-Status)
        val isMissingBirthday = contact.dateText == "-"
        val isNoBirthdayFilter = label == LABEL_NO_BIRTHDAY

        // Kontakte ohne Geburtstag ausblenden (außer bei Suche oder speziellem Filter)
        if (isMissingBirthday && !isSearching && !isNoBirthdayFilter) return false

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
            LABEL_NO_BIRTHDAY -> isMissingBirthday
            else -> contact.labels.contains(label)
        }
    }

    val availableLabels: Flow<List<String>> = combine(
        contactRepository.allContacts,
        contactRepository.labelConfigs,
    ) { contacts, configs ->
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

        // Wenn weder aktive User-Labels noch das Pseudo-Label aktiv sind -> Bar verstecken
        if (!hasActiveUserLabels && !showPseudo) return@combine emptyList()

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

        // "Ohne Datum" immer als letztes, falls aktiv
        if (showPseudo) {
            labels.add(LABEL_NO_BIRTHDAY)
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

    private suspend fun updateContactGiftIdeas(
        lookupKey: String,
        transform: (List<GiftIdea>) -> List<GiftIdea>
    ) {
        val contact = allUiContacts.first().find { it.lookupKey == lookupKey } ?: return
        val newIdeas = transform(contact.giftIdeas)
        contactRepository.updateGiftIdeas(lookupKey, newIdeas)
    }

    fun addGiftIdea(lookupKey: String) = viewModelScope.launch {
        val newIdea = GiftIdea(text = "")
        _newlyAddedIdeaId.value = newIdea.id
        updateContactGiftIdeas(lookupKey) { current -> GiftIdea.withNewIdea(current, newIdea) }
    }

    fun toggleGiftIdea(lookupKey: String, idea: GiftIdea, isChecked: Boolean) =
        viewModelScope.launch {
            updateContactGiftIdeas(lookupKey) { current ->
                GiftIdea.withToggledIdea(
                    current,
                    idea,
                    isChecked
                )
            }
        }

    fun deleteGiftIdea(lookupKey: String, ideaId: String) = viewModelScope.launch {
        updateContactGiftIdeas(lookupKey) { currentIdeas ->
            currentIdeas.filter { it.id != ideaId }
        }
    }

    fun updateGiftIdeaText(lookupKey: String, ideaId: String, newText: String) =
        viewModelScope.launch {
            updateContactGiftIdeas(lookupKey) { currentIdeas ->
                currentIdeas.map {
                    if (it.id == ideaId) it.copy(text = newText) else it
                }
            }
        }

    fun updateBirthday(contactId: String, birthday: java.time.LocalDate) = viewModelScope.launch {
        contactRepository.updateContactBirthday(contactId, birthday)
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

    private fun updateWidget() = viewModelScope.launch {
        widgetUpdater.updateWidget()
    }
}
