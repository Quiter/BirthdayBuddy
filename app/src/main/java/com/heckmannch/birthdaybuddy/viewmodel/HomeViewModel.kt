package com.heckmannch.birthdaybuddy.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.data.mapper.ContactMapper
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.data.repository.TimeRepository
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.CoupleSuggestionUiModel
import com.heckmannch.birthdaybuddy.ui.model.GiftIdea
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.util.getInitials
import com.heckmannch.birthdaybuddy.util.mergeNames
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val mapper: ContactMapper,
    timeRepository: TimeRepository,
) : ViewModel() {

    // --- Search & Filter State (MVI Consolidated UI State) ---
    private data class UserUiState(
        val searchQuery: String = "",
        val selectedLabel: String? = null,
        val isResettingFilter: Boolean = false,
        val isSyncing: Boolean = false,
        val searchFocusRequested: Boolean = false,
        val newlyAddedIdeaId: String? = null
    )

    private val _userUiState = MutableStateFlow(UserUiState())

    companion object {
        const val LABEL_NO_BIRTHDAY = "special:no_birthday"
        const val LABEL_ANNIVERSARY = "special:anniversary"
        const val LABEL_NAME_DAY = "special:name_day"
        private val WHITESPACE_REGEX = "\\s+".toRegex()
    }

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(replay = 0)
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    private val _syncCompletedEvent = MutableSharedFlow<Unit>(replay = 0)
    val syncCompletedEvent: SharedFlow<Unit> = _syncCompletedEvent.asSharedFlow()

    fun setIsResettingFilter(isResetting: Boolean) {
        onIntent(HomeIntent.SetIsResettingFilter(isResetting))
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

    private val searchKeywords = _userUiState
        .map { it.searchQuery }
        .distinctUntilChanged()
        .debounce(300.milliseconds)
        .map { it.trim() }
        .distinctUntilChanged()
        .map { if (it.isEmpty()) emptyList() else it.split(WHITESPACE_REGEX) }
        .flowOn(Dispatchers.Default)

    private val filteredContacts: Flow<List<ContactUiModel>?> = combine(
        contactRepository.allContacts,
        timeRepository.currentDate,
        searchKeywords,
        _userUiState.map { it.selectedLabel }.distinctUntilChanged(),
        ignoredLabels,
    ) { rawContacts, today, keywords, label, ignoredLabels ->
        val startTime = System.currentTimeMillis()
        val isSearching = keywords.isNotEmpty()

        val displayEventType = when (label) {
            LABEL_ANNIVERSARY -> "anniversary"
            LABEL_NAME_DAY -> "name_day"
            else -> "birthday"
        }

        // --- OPTIMIERUNG: Vor-Filterung der Rohdaten ---
        // Dies reduziert die Anzahl der teuren Mapping-Vorgänge erheblich.
        val preFilteredRaw = if (displayEventType != "anniversary") {
            rawContacts.asSequence().filter { contact ->
                // 1. Suche (Rohname)
                if (isSearching && !keywords.all { keyword -> contact.fullName.contains(keyword, ignoreCase = true) }) {
                    return@filter false
                }
                // 2. Label-Filter (außer Spezial-Labels)
                if (label != null && label != LABEL_NO_BIRTHDAY && label != LABEL_NAME_DAY && !contact.labels.contains(label)) {
                    return@filter false
                }
                // 3. Ignorierte Labels (nur wenn nicht gesucht wird)
                if (!isSearching && contact.labels.any { it in ignoredLabels }) {
                    return@filter false
                }
                // 4. Event-Verfügbarkeit
                val hasEvent = if (displayEventType == "name_day") contact.nameDay != null else contact.birthday != null
                if (!hasEvent) {
                    // Wenn kein Event vorhanden ist:
                    // - Bei Namenstagen/Hochzeitstagen immer ausblenden
                    if (displayEventType != "birthday") return@filter false
                    // - Bei Geburtstagen nur einblenden, wenn gesucht wird oder der "Ohne Datum"-Filter aktiv ist
                    if (!isSearching && label != LABEL_NO_BIRTHDAY) return@filter false
                } else if (label == LABEL_NO_BIRTHDAY) {
                    // Wenn ein Geburtsdatum vorhanden ist, aber der "Ohne Datum"-Filter aktiv ist -> ausblenden
                    return@filter false
                }
                true
            }.toList()
        } else {
            rawContacts // Bei Hochzeitstagen brauchen wir alle Kontakte für das Pairing
        }

        val uiList = if (displayEventType == "anniversary") {
            val processedKeys = mutableSetOf<String>()
            val mergedList = mutableListOf<ContactUiModel>()
            val contactMap = rawContacts.associateBy { it.lookupKey }

            for (contact in rawContacts) {
                if (processedKeys.contains(contact.lookupKey)) continue

                val spouseKey = contact.spouseLookupKey
                val spouse = if (spouseKey != null) contactMap[spouseKey] else null

                if (spouse != null && contact.anniversary != null && spouse.anniversary != null) {
                    processedKeys.add(contact.lookupKey)
                    processedKeys.add(spouse.lookupKey)

                    val uiModelA = mapper.toUiModelForEvent(contact, today, "anniversary")
                    val uiModelB = mapper.toUiModelForEvent(spouse, today, "anniversary")

                    val mergedUiModel = ContactUiModel(
                        id = "${contact.lookupKey}_${spouse.lookupKey}",
                        contactId = contact.contactId,
                        lookupKey = contact.lookupKey,
                        fullName = mergeNames(contact.fullName, spouse.fullName),
                        dateText = uiModelA.dateText,
                        monthName = uiModelA.monthName,
                        imageUri = contact.imageUri,
                        phoneNumber = contact.phoneNumber,
                        initials = uiModelA.initials,
                        nextAge = uiModelA.nextAge,
                        daysUntilNext = uiModelA.daysUntilNext,
                        isToday = uiModelA.isToday,
                        hasWhatsApp = contact.hasWhatsApp || spouse.hasWhatsApp,
                        hasSignal = contact.hasSignal || spouse.hasSignal,
                        labels = (contact.labels + spouse.labels).distinct(),
                        giftIdeas = uiModelA.giftIdeas + uiModelB.giftIdeas,
                        birthday = contact.birthday,
                        secondImageUri = spouse.imageUri,
                        secondInitials = uiModelB.initials,
                        secondFullName = spouse.fullName,
                        isCouple = true
                    )
                    mergedList.add(mergedUiModel)
                } else {
                    processedKeys.add(contact.lookupKey)
                    mergedList.add(mapper.toUiModelForEvent(contact, today, "anniversary"))
                }
            }
            mergedList
        } else {
            preFilteredRaw.map { mapper.toUiModelForEvent(it, today, displayEventType) }
        }

        val result = uiList.asSequence()
            .filter { shouldShowContact(it, keywords, label, ignoredLabels, displayEventType) }
            .sortedWith(
                compareBy<ContactUiModel> { it.daysUntilNext }
                    .thenBy { it.fullName },
            )
            .toList()

        if (rawContacts.size > 1000) {
            Log.d(
                "HomeViewModel",
                "Filtering ${rawContacts.size} -> ${result.size} contacts took ${System.currentTimeMillis() - startTime}ms"
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

    val coupleSuggestion: Flow<CoupleSuggestionUiModel?> = combine(
        contactRepository.potentialCouples,
        contactRepository.ignoredCouplePairs,
        _userUiState.map { it.selectedLabel }.distinctUntilChanged(),
    ) { potentials, ignoredPairs, label ->
        if (label != LABEL_ANNIVERSARY || potentials.isEmpty()) return@combine null

        potentials.firstOrNull { couple ->
            val pairKey = if (couple.firstLookupKey < couple.secondLookupKey) {
                "${couple.firstLookupKey}:${couple.secondLookupKey}"
            } else {
                "${couple.secondLookupKey}:${couple.firstLookupKey}"
            }
            !ignoredPairs.contains(pairKey)
        }?.let { couple ->
            CoupleSuggestionUiModel(
                firstLookupKey = couple.firstLookupKey,
                firstName = couple.firstName,
                firstImageUri = couple.firstImageUri,
                firstInitials = couple.firstName.getInitials(),
                secondLookupKey = couple.secondLookupKey,
                secondName = couple.secondName,
                secondImageUri = couple.secondImageUri,
                secondInitials = couple.secondName.getInitials()
            )
        }
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<HomeUiState> = combine(
        filteredContacts,
        availableLabels,
        _userUiState,
        coupleSuggestion
    ) { contacts, labels, userState, suggestion ->
        HomeUiState(
            contacts = contacts,
            availableLabels = labels,
            searchQuery = userState.searchQuery,
            selectedLabel = userState.selectedLabel,
            isResettingFilter = userState.isResettingFilter,
            isSyncing = userState.isSyncing,
            searchFocusRequested = userState.searchFocusRequested,
            newlyAddedIdeaId = userState.newlyAddedIdeaId,
            coupleSuggestion = suggestion
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    // --- MVI Intent Processing ---
    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SearchQueryChanged -> {
                val newQuery = intent.query
                _userUiState.update { state ->
                    if (state.searchQuery == newQuery) state
                    else {
                        val updatedLabel = if (newQuery.isNotEmpty() && state.searchQuery.isEmpty()) {
                            null
                        } else {
                            state.selectedLabel
                        }
                        state.copy(
                            searchQuery = newQuery,
                            selectedLabel = updatedLabel,
                            isResettingFilter = true
                        )
                    }
                }
                triggerScrollToTop()
            }
            is HomeIntent.LabelSelected -> {
                _userUiState.update { state ->
                    val newLabel = if (state.selectedLabel == intent.label) null else intent.label
                    if (state.selectedLabel == newLabel) state
                    else state.copy(selectedLabel = newLabel, isResettingFilter = true)
                }
                triggerScrollToTop()
            }
            is HomeIntent.ResetFilters -> {
                _userUiState.update { state ->
                    if (state.searchQuery.isNotEmpty() || state.selectedLabel != null) {
                        state.copy(searchQuery = "", selectedLabel = null, isResettingFilter = true)
                    } else {
                        state
                    }
                }
                triggerScrollToTop()
            }
            is HomeIntent.AddGiftIdea -> {
                val newIdea = GiftIdea(text = "")
                _userUiState.update { it.copy(newlyAddedIdeaId = newIdea.id) }
                viewModelScope.launch {
                    contactRepository.addGiftIdea(intent.lookupKey, newIdea)
                }
            }
            is HomeIntent.ToggleGiftIdea -> {
                viewModelScope.launch {
                    contactRepository.toggleGiftIdea(intent.lookupKey, intent.idea, intent.isChecked)
                }
            }
            is HomeIntent.DeleteGiftIdea -> {
                viewModelScope.launch {
                    contactRepository.deleteGiftIdea(intent.lookupKey, intent.ideaId)
                }
            }
            is HomeIntent.UpdateGiftIdeaText -> {
                viewModelScope.launch {
                    contactRepository.updateGiftIdeaText(intent.lookupKey, intent.ideaId, intent.newText)
                }
            }
            is HomeIntent.UpdateBirthday -> {
                viewModelScope.launch {
                    contactRepository.updateContactBirthday(intent.contactId, intent.birthday)
                }
            }
            is HomeIntent.SyncContacts -> {
                viewModelScope.launch {
                    if (intent.showLoading) {
                        _userUiState.update { it.copy(isSyncing = true) }
                        contactRepository.clearIgnoredCouplePairs()
                    }
                    val startTime = System.currentTimeMillis()
                    contactRepository.syncContacts()
                    if (intent.showLoading) {
                        val elapsedTime = System.currentTimeMillis() - startTime
                        if (elapsedTime < 800) {
                            delay((800 - elapsedTime).milliseconds)
                        }
                        _userUiState.update { it.copy(isSyncing = false) }
                        _syncCompletedEvent.emit(Unit)
                    }
                }
            }
            is HomeIntent.TriggerScrollToTop -> {
                viewModelScope.launch {
                    _scrollToTopEvent.emit(Unit)
                }
            }
            is HomeIntent.TriggerSearchFocus -> {
                _userUiState.update { it.copy(searchFocusRequested = true) }
            }
            is HomeIntent.ConsumeSearchFocus -> {
                _userUiState.update { it.copy(searchFocusRequested = false) }
            }
            is HomeIntent.ConsumeNewlyAddedIdeaId -> {
                _userUiState.update { it.copy(newlyAddedIdeaId = null) }
            }
            is HomeIntent.LinkAsCouple -> {
                viewModelScope.launch {
                    contactRepository.linkAsCouple(intent.lookupKey1, intent.lookupKey2)
                }
            }
            is HomeIntent.UnlinkCouple -> {
                viewModelScope.launch {
                    contactRepository.unlinkCouple(intent.lookupKey)
                }
            }
            is HomeIntent.IgnoreCoupleSuggestion -> {
                viewModelScope.launch {
                    contactRepository.ignoreCoupleSuggestion(intent.lookupKey1, intent.lookupKey2)
                }
            }
            is HomeIntent.SetIsResettingFilter -> {
                _userUiState.update { it.copy(isResettingFilter = intent.isResetting) }
            }
        }
    }

    // --- Legacy / Compatibility Actions ---
    fun onSearchQueryChange(newQuery: String) {
        onIntent(HomeIntent.SearchQueryChanged(newQuery))
    }

    fun onLabelSelected(label: String?) {
        onIntent(HomeIntent.LabelSelected(label))
    }

    fun resetFilters() {
        onIntent(HomeIntent.ResetFilters)
    }

    fun addGiftIdea(lookupKey: String) {
        onIntent(HomeIntent.AddGiftIdea(lookupKey))
    }

    fun toggleGiftIdea(lookupKey: String, idea: GiftIdea, isChecked: Boolean) {
        onIntent(HomeIntent.ToggleGiftIdea(lookupKey, idea, isChecked))
    }

    fun deleteGiftIdea(lookupKey: String, ideaId: String) {
        onIntent(HomeIntent.DeleteGiftIdea(lookupKey, ideaId))
    }

    fun updateGiftIdeaText(lookupKey: String, ideaId: String, newText: String) {
        onIntent(HomeIntent.UpdateGiftIdeaText(lookupKey, ideaId, newText))
    }

    fun updateBirthday(contactId: String, birthday: java.time.LocalDate) {
        onIntent(HomeIntent.UpdateBirthday(contactId, birthday))
    }

    fun syncContacts(showLoading: Boolean = false) {
        onIntent(HomeIntent.SyncContacts(showLoading))
    }

    fun triggerScrollToTop() {
        onIntent(HomeIntent.TriggerScrollToTop)
    }

    fun triggerSearchFocus() {
        onIntent(HomeIntent.TriggerSearchFocus)
    }

    fun consumeSearchFocus() {
        onIntent(HomeIntent.ConsumeSearchFocus)
    }

    fun consumeNewlyAddedIdeaId() {
        onIntent(HomeIntent.ConsumeNewlyAddedIdeaId)
    }

    fun linkAsCouple(lookupKey1: String, lookupKey2: String) {
        onIntent(HomeIntent.LinkAsCouple(lookupKey1, lookupKey2))
    }

    fun unlinkCouple(lookupKey: String) {
        onIntent(HomeIntent.UnlinkCouple(lookupKey))
    }

    fun ignoreCoupleSuggestion(lookupKey1: String, lookupKey2: String) {
        onIntent(HomeIntent.IgnoreCoupleSuggestion(lookupKey1, lookupKey2))
    }
}

// --- Home MVI Intent Definition ---
sealed interface HomeIntent {
    data class SearchQueryChanged(val query: String) : HomeIntent
    data class LabelSelected(val label: String?) : HomeIntent
    object ResetFilters : HomeIntent
    data class AddGiftIdea(val lookupKey: String) : HomeIntent
    data class ToggleGiftIdea(val lookupKey: String, val idea: GiftIdea, val isChecked: Boolean) : HomeIntent
    data class DeleteGiftIdea(val lookupKey: String, val ideaId: String) : HomeIntent
    data class UpdateGiftIdeaText(val lookupKey: String, val ideaId: String, val newText: String) : HomeIntent
    data class UpdateBirthday(val contactId: String, val birthday: java.time.LocalDate) : HomeIntent
    data class SyncContacts(val showLoading: Boolean = false) : HomeIntent
    object TriggerScrollToTop : HomeIntent
    object TriggerSearchFocus : HomeIntent
    object ConsumeSearchFocus : HomeIntent
    object ConsumeNewlyAddedIdeaId : HomeIntent
    data class LinkAsCouple(val lookupKey1: String, val lookupKey2: String) : HomeIntent
    data class UnlinkCouple(val lookupKey: String) : HomeIntent
    data class IgnoreCoupleSuggestion(val lookupKey1: String, val lookupKey2: String) : HomeIntent
    data class SetIsResettingFilter(val isResetting: Boolean) : HomeIntent
}
