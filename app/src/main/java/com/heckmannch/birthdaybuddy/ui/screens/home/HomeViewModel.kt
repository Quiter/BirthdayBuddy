package com.heckmannch.birthdaybuddy.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.TimeRepository
import com.heckmannch.birthdaybuddy.domain.usecase.GetAvailableLabelsUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.GetContactsUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.GetCoupleSuggestionUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.IgnoreCoupleSuggestionUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.LinkAsCoupleUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.UnlinkCoupleUseCase
import com.heckmannch.birthdaybuddy.ui.model.CoupleSuggestionUiModel
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
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
    getContactsUseCase: GetContactsUseCase,
    getAvailableLabelsUseCase: GetAvailableLabelsUseCase,
    getCoupleSuggestionUseCase: GetCoupleSuggestionUseCase,
    private val linkAsCoupleUseCase: LinkAsCoupleUseCase,
    private val unlinkCoupleUseCase: UnlinkCoupleUseCase,
    private val ignoreCoupleSuggestionUseCase: IgnoreCoupleSuggestionUseCase,
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
        private val WHITESPACE_REGEX = "\\s+".toRegex()
    }

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(replay = 0)
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    private val _syncCompletedEvent = MutableSharedFlow<Unit>(replay = 0)
    val syncCompletedEvent: SharedFlow<Unit> = _syncCompletedEvent.asSharedFlow()


    private val labelSettingsState: Flow<GetContactsUseCase.LabelSettingsState> = combine(
        contactRepository.labelConfigs,
        contactRepository.labelsEnabled,
        contactRepository.otherEventsEnabled
    ) { configs, labelsEnabled, otherEventsEnabled ->
        val ignored = if (!labelsEnabled) emptySet()
        else configs.asSequence()
            .filter { it.isIgnored }
            .map { it.name }
            .toSet()
        GetContactsUseCase.LabelSettingsState(ignored, labelsEnabled, otherEventsEnabled)
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

    private val filteredContacts = getContactsUseCase(
        contacts = contactRepository.allContacts,
        currentDate = timeRepository.currentDate,
        searchKeywords = searchKeywords,
        selectedLabel = _userUiState.map { it.selectedLabel }.distinctUntilChanged(),
        labelSettings = labelSettingsState,
    ).flowOn(Dispatchers.Default)

    val availableLabels: Flow<List<String>> = getAvailableLabelsUseCase(
        contacts = contactRepository.allContacts,
        configs = contactRepository.labelConfigs,
        otherEventsEnabled = contactRepository.otherEventsEnabled,
        labelsEnabled = contactRepository.labelsEnabled,
    )

    val coupleSuggestion: Flow<CoupleSuggestionUiModel?> = getCoupleSuggestionUseCase(
        selectedLabel = _userUiState.map { it.selectedLabel }.distinctUntilChanged()
    ).flowOn(Dispatchers.Default)

    val uiState: StateFlow<HomeUiState> = combine(
        filteredContacts,
        availableLabels,
        _userUiState,
        coupleSuggestion,
        contactRepository.labelsEnabled
    ) { contacts, labels, userState, suggestion, labelsEnabled ->
        val finalContacts = if (!labelsEnabled) {
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
                    linkAsCoupleUseCase(intent.lookupKey1, intent.lookupKey2)
                }
            }

            is HomeIntent.UnlinkCouple -> {
                viewModelScope.launch {
                    unlinkCoupleUseCase(intent.lookupKey)
                }
            }

            is HomeIntent.IgnoreCoupleSuggestion -> {
                viewModelScope.launch {
                    ignoreCoupleSuggestionUseCase(intent.lookupKey1, intent.lookupKey2)
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
