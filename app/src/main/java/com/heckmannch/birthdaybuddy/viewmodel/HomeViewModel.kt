package com.heckmannch.birthdaybuddy.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.data.mapper.ContactMapper
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.data.repository.TimeRepository
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.CoupleSuggestionUiModel
import com.heckmannch.birthdaybuddy.ui.model.EventType
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


    private data class LabelSettingsState(
        val ignoredLabels: Set<String>,
        val labelsEnabled: Boolean,
        val otherEventsEnabled: Boolean
    )

    private val labelSettingsState: Flow<LabelSettingsState> = combine(
        contactRepository.labelConfigs,
        contactRepository.labelsEnabled,
        contactRepository.otherEventsEnabled
    ) { configs, labelsEnabled, otherEventsEnabled ->
        val ignored = if (!labelsEnabled) emptySet()
        else configs.asSequence()
            .filter { it.isIgnored }
            .map { it.name }
            .toSet()
        LabelSettingsState(ignored, labelsEnabled, otherEventsEnabled)
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
        labelSettingsState,
    ) { rawContacts, today, keywords, label, settingsState ->
        val startTime = System.currentTimeMillis()
        val isSearching = keywords.isNotEmpty()
        val ignoredLabels = settingsState.ignoredLabels
        val labelsEnabled = settingsState.labelsEnabled
        val otherEventsEnabled = settingsState.otherEventsEnabled

        val uiList = if (!labelsEnabled) {
            // Wenn Label-Management deaktiviert ist, alle drei Ereignistypen in die Liste einfügen

            // 1. Geburtstage
            val birthdays = rawContacts.asSequence()
                .filter { it.birthday != null }
                .filter { contact ->
                    !isSearching || keywords.all { keyword ->
                        contact.fullName.contains(keyword, ignoreCase = true)
                    }
                }
                .map { mapper.toUiModelForEvent(it, today, EventType.BIRTHDAY).copy(labels = emptyList()) }
                .toList()

            // 2. Namenstage (nur wenn weitere Ereignisse aktiviert sind)
            val nameDays = if (otherEventsEnabled) {
                rawContacts.asSequence()
                    .filter { it.nameDay != null }
                    .filter { contact ->
                        !isSearching || keywords.all { keyword ->
                            contact.fullName.contains(keyword, ignoreCase = true)
                        }
                    }
                    .map { mapper.toUiModelForEvent(it, today, EventType.NAME_DAY).copy(labels = emptyList()) }
                    .toList()
            } else emptyList()

            // 3. Hochzeitstage (mit Pairing-Logik analog zum Standard, nur wenn weitere Ereignisse aktiviert sind)
            val pairedAnniversaries = if (otherEventsEnabled) {
                val processedKeys = mutableSetOf<String>()
                val list = mutableListOf<ContactUiModel>()
                val contactMap = rawContacts.associateBy { it.lookupKey }

                for (contact in rawContacts) {
                    if (contact.anniversary == null) continue
                    if (processedKeys.contains(contact.lookupKey)) continue

                    // Suchfilter anwenden
                    if (isSearching && !keywords.all { keyword ->
                            contact.fullName.contains(keyword, ignoreCase = true) ||
                            (contact.spouseLookupKey?.let { contactMap[it]?.fullName }?.contains(keyword, ignoreCase = true) ?: false)
                        }
                    ) {
                        continue
                    }

                    val spouseKey = contact.spouseLookupKey
                    val spouse = if (spouseKey != null) contactMap[spouseKey] else null

                    if (spouse != null && spouse.anniversary != null) {
                        processedKeys.add(contact.lookupKey)
                        processedKeys.add(spouse.lookupKey)

                        val uiModelA = mapper.toUiModelForEvent(contact, today, EventType.ANNIVERSARY)
                        val uiModelB = mapper.toUiModelForEvent(spouse, today, EventType.ANNIVERSARY)

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
                            labels = emptyList(),
                            giftIdeas = uiModelA.giftIdeas + uiModelB.giftIdeas,
                            birthday = contact.birthday,
                            secondImageUri = spouse.imageUri,
                            secondInitials = uiModelB.initials,
                            secondFullName = spouse.fullName,
                            isCouple = true
                        )
                        list.add(mergedUiModel)
                    } else {
                        processedKeys.add(contact.lookupKey)
                        list.add(
                            mapper.toUiModelForEvent(contact, today, EventType.ANNIVERSARY)
                                .copy(labels = emptyList())
                        )
                    }
                }
                list
            } else emptyList()

            // 4. Ereignislose Kontakte nur bei aktiver Suche anzeigen
            val contactsWithNoEvent = if (isSearching) {
                rawContacts.asSequence()
                    .filter { contact ->
                        val hasNoAnniversary = !otherEventsEnabled || contact.anniversary == null
                        val hasNoNameDay = !otherEventsEnabled || contact.nameDay == null
                        contact.birthday == null && hasNoAnniversary && hasNoNameDay
                    }
                    .filter { contact ->
                        keywords.all { keyword ->
                            contact.fullName.contains(keyword, ignoreCase = true)
                        }
                    }
                    .map { mapper.toUiModelForEvent(it, today, EventType.BIRTHDAY).copy(labels = emptyList()) }
                    .toList()
            } else emptyList()

            birthdays + nameDays + pairedAnniversaries + contactsWithNoEvent
        } else {
            val displayEventType: EventType = when (label) {
                LABEL_ANNIVERSARY -> EventType.ANNIVERSARY
                LABEL_NAME_DAY -> EventType.NAME_DAY
                else -> EventType.BIRTHDAY
            }

            val preFilteredRaw = if (displayEventType != EventType.ANNIVERSARY) {
                rawContacts.asSequence().filter { contact ->
                    if (isSearching && !keywords.all { keyword ->
                            contact.fullName.contains(keyword, ignoreCase = true)
                        }) {
                        return@filter false
                    }
                    if (label != null && label != LABEL_NO_BIRTHDAY && label != LABEL_NAME_DAY && !contact.labels.contains(label)) {
                        return@filter false
                    }
                    if (!isSearching && contact.labels.any { it in ignoredLabels }) {
                        return@filter false
                    }
                    val hasEvent = if (displayEventType == EventType.NAME_DAY) contact.nameDay != null else contact.birthday != null
                    if (!hasEvent) {
                        if (displayEventType != EventType.BIRTHDAY) return@filter false
                        if (!isSearching && label != LABEL_NO_BIRTHDAY) return@filter false
                    } else if (label == LABEL_NO_BIRTHDAY) {
                        return@filter false
                    }
                    true
                }.toList()
            } else {
                rawContacts
            }

            val uiListTemp = if (displayEventType == EventType.ANNIVERSARY) {
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

                        val uiModelA = mapper.toUiModelForEvent(contact, today, EventType.ANNIVERSARY)
                        val uiModelB = mapper.toUiModelForEvent(spouse, today, EventType.ANNIVERSARY)

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
                        mergedList.add(mapper.toUiModelForEvent(contact, today, EventType.ANNIVERSARY))
                    }
                }
                mergedList
            } else {
                preFilteredRaw.map { mapper.toUiModelForEvent(it, today, displayEventType) }
            }

            uiListTemp.filter { shouldShowContact(it, keywords, label, ignoredLabels, displayEventType) }
        }

        val result = uiList.sortedWith(
            compareBy<ContactUiModel, Long?>(nullsLast(naturalOrder())) { it.daysUntilNext }
                .thenBy { it.fullName }
        )

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
        displayEventType: EventType
    ): Boolean {
        val isSearching = keywords.isNotEmpty()

        // 1. Sichtbarkeit prüfen (Ereignis vorhanden & Ignoriert-Status)
        val isMissingEvent = contact.dateText == "-"
        val isNoBirthdayFilter = label == LABEL_NO_BIRTHDAY

        // Ereignislose Kontakte ausblenden (außer bei Suche, sofern es sich um Geburtstage handelt.
        // Für Hochzeitstag und Namenstag blenden wir Kontakte ohne dieses Ereignis IMMER aus!)
        if (isMissingEvent) {
            if (displayEventType != EventType.BIRTHDAY) return false
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
        contactRepository.labelsEnabled,
    ) { contacts, configs, otherEventsEnabled, labelsEnabled ->
        if (!labelsEnabled) return@combine emptyList()
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

        // "Ohne Datum" immer als Letztes von Geburtstagen, falls aktiv
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
        coupleSuggestion,
        contactRepository.labelsEnabled
    ) { contacts, labels, userState, suggestion, labelsEnabled ->
        val finalContacts = if (!labelsEnabled && contacts != null) {
            contacts.map { it.copy(labels = emptyList()) }
        } else {
            contacts
        }
        HomeUiState(
            contacts = finalContacts,
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
                        val updatedLabel =
                            if (newQuery.isNotEmpty() && state.searchQuery.isEmpty()) {
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
                    contactRepository.toggleGiftIdea(
                        intent.lookupKey,
                        intent.idea,
                        intent.isChecked
                    )
                }
            }

            is HomeIntent.DeleteGiftIdea -> {
                viewModelScope.launch {
                    contactRepository.deleteGiftIdea(intent.lookupKey, intent.ideaId)
                }
            }

            is HomeIntent.UpdateGiftIdeaText -> {
                viewModelScope.launch {
                    contactRepository.updateGiftIdeaText(
                        intent.lookupKey,
                        intent.ideaId,
                        intent.newText
                    )
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

    fun syncContacts(showLoading: Boolean = false) {
        onIntent(HomeIntent.SyncContacts(showLoading))
    }

    fun triggerScrollToTop() {
        onIntent(HomeIntent.TriggerScrollToTop)
    }

    fun triggerSearchFocus() {
        onIntent(HomeIntent.TriggerSearchFocus)
    }
}

// --- Home MVI Intent Definition ---
sealed interface HomeIntent {
    data class SearchQueryChanged(val query: String) : HomeIntent
    data class LabelSelected(val label: String?) : HomeIntent
    object ResetFilters : HomeIntent
    data class AddGiftIdea(val lookupKey: String) : HomeIntent
    data class ToggleGiftIdea(val lookupKey: String, val idea: GiftIdea, val isChecked: Boolean) :
        HomeIntent

    data class DeleteGiftIdea(val lookupKey: String, val ideaId: String) : HomeIntent
    data class UpdateGiftIdeaText(val lookupKey: String, val ideaId: String, val newText: String) :
        HomeIntent

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
