package com.heckmannch.birthdaybuddy.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.TimeRepository
import com.heckmannch.birthdaybuddy.domain.usecase.GetAvailableLabelsUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.GetContactsUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.GetCoupleSuggestionUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.IgnoreCoupleSuggestionUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.LinkAsCoupleUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.UnlinkCoupleUseCase
import com.heckmannch.birthdaybuddy.ui.mapper.ContactUiMapper
import com.heckmannch.birthdaybuddy.ui.mapper.CoupleSuggestionUiMapper
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.CoupleSuggestionUiModel
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.model.PendingBirthdayEdit
import com.heckmannch.birthdaybuddy.util.Clock
import com.heckmannch.birthdaybuddy.util.NO_YEAR_MARKER
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
import java.time.LocalDate
import java.time.Month
import java.time.Year
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    getContactsUseCase: GetContactsUseCase,
    private val contactUiMapper: ContactUiMapper,
    private val coupleSuggestionUiMapper: CoupleSuggestionUiMapper,
    getAvailableLabelsUseCase: GetAvailableLabelsUseCase,
    getCoupleSuggestionUseCase: GetCoupleSuggestionUseCase,
    private val linkAsCoupleUseCase: LinkAsCoupleUseCase,
    private val unlinkCoupleUseCase: UnlinkCoupleUseCase,
    private val ignoreCoupleSuggestionUseCase: IgnoreCoupleSuggestionUseCase,
    timeRepository: TimeRepository,
    private val permissionChecker: PermissionChecker,
    private val clock: Clock,
) : ViewModel() {

    private var lastInteractionTime: Long = clock.currentTimeMillis()

    // --- Search & Filter State (MVI Consolidated UI State) ---
    private data class UserUiState(
        val searchQuery: String = "",
        val selectedLabel: String? = null,
        val isResettingFilter: Boolean = false,
        val isSyncing: Boolean = false,
        val searchFocusRequested: Boolean = false,
        val newlyAddedIdeaId: String? = null,
        val hasContactPermission: Boolean = false,
        val pendingBirthdayEdit: PendingBirthdayEdit? = null,
    )

    private fun checkContactPermission(): Boolean {
        return permissionChecker.hasContactsPermission()
    }

    private val _userUiState = MutableStateFlow(
        UserUiState(
            hasContactPermission = checkContactPermission()
        )
    )

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
        onIntent(HomeIntent.SyncContacts())
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
    )

    private val uiContacts: Flow<List<ContactUiModel>> = combine(
        filteredContacts,
        timeRepository.currentDate,
        _userUiState.map { it.selectedLabel }.distinctUntilChanged(),
        contactRepository.labelsEnabled,
        contactRepository.otherEventsEnabled
    ) { contacts, today, selectedLabel, labelsEnabled, otherEventsEnabled ->
        contactUiMapper.mapToUiModels(
            contacts = contacts,
            today = today,
            selectedLabel = selectedLabel,
            labelsEnabled = labelsEnabled,
            otherEventsEnabled = otherEventsEnabled
        )
    }.flowOn(Dispatchers.Default)

    private val availableLabels: Flow<List<String>> = getAvailableLabelsUseCase(
        contacts = contactRepository.allContacts,
        configs = contactRepository.labelConfigs,
        otherEventsEnabled = contactRepository.otherEventsEnabled,
        labelsEnabled = contactRepository.labelsEnabled,
    )

    private val coupleSuggestion: Flow<CoupleSuggestionUiModel?> = getCoupleSuggestionUseCase(
        selectedLabel = _userUiState.map { it.selectedLabel }.distinctUntilChanged()
    ).map { suggestion ->
        suggestion?.let { coupleSuggestionUiMapper.toUiModel(it) }
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<HomeUiState> = combine(
        uiContacts,
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
            coupleSuggestion = suggestion,
            hasContactPermission = userState.hasContactPermission,
            pendingBirthdayEdit = userState.pendingBirthdayEdit,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private fun triggerScrollToTop() {
        viewModelScope.launch {
            _scrollToTopEvent.emit(Unit)
        }
    }

    private fun resetFiltersInternal() {
        _userUiState.update { state ->
            if (state.searchQuery.isNotEmpty() || state.selectedLabel != null) {
                state.copy(searchQuery = "", selectedLabel = null, isResettingFilter = true)
            } else {
                state
            }
        }
        triggerScrollToTop()
    }

    // --- MVI Intent Processing ---
    fun onIntent(intent: HomeIntent) {
        if (intent !is HomeIntent.AppResumed) {
            lastInteractionTime = clock.currentTimeMillis()
        }
        when (intent) {
            is HomeIntent.AppResumed -> {
                _userUiState.update { it.copy(hasContactPermission = checkContactPermission()) }
                if ((clock.currentTimeMillis() - lastInteractionTime) > (5 * 60 * 1000)) {
                    resetFiltersInternal()
                }
                lastInteractionTime = clock.currentTimeMillis()
            }
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
                resetFiltersInternal()
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
                _userUiState.update { it.copy(hasContactPermission = checkContactPermission()) }
                viewModelScope.launch {
                    if (intent.showLoading) {
                        _userUiState.update { it.copy(isSyncing = true) }
                        contactRepository.clearIgnoredCouplePairs()
                    }
                    val startTime = clock.currentTimeMillis()
                    contactRepository.syncContacts()
                    if (intent.showLoading) {
                        val elapsedTime = clock.currentTimeMillis() - startTime
                        if (elapsedTime < 800) {
                            delay((800 - elapsedTime).milliseconds)
                        }
                        _userUiState.update { it.copy(isSyncing = false) }
                        _syncCompletedEvent.emit(Unit)
                    }
                }
            }

            is HomeIntent.TriggerScrollToTop -> {
                triggerScrollToTop()
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

            is HomeIntent.OpenBirthdayPicker -> {
                viewModelScope.launch {
                    var contact = contactRepository.getAllContactsImmediate()
                        .find { it.lookupKey == intent.contactLookupKey || it.contactId == intent.contactLookupKey }
                    if (contact == null) {
                        contactRepository.syncContacts()
                        contact = contactRepository.getAllContactsImmediate()
                            .find { it.lookupKey == intent.contactLookupKey || it.contactId == intent.contactLookupKey }
                    }
                    if (contact != null) {
                        val targetYear = if (intent.year != null && intent.year > 0 && intent.year != NO_YEAR_MARKER) intent.year else NO_YEAR_MARKER
                        val safeMonth = intent.month.coerceIn(1, 12)
                        val maxDays = Month.of(safeMonth).length(Year.isLeap(targetYear.toLong()))
                        val safeDay = intent.day.coerceIn(1, maxDays)
                        val initialDate = LocalDate.of(targetYear, safeMonth, safeDay)

                        _userUiState.update {
                            it.copy(
                                pendingBirthdayEdit = PendingBirthdayEdit(
                                    contactId = contact.contactId,
                                    initialDate = initialDate
                                )
                            )
                        }
                    }
                }
            }

            is HomeIntent.DismissBirthdayPicker -> {
                _userUiState.update { it.copy(pendingBirthdayEdit = null) }
            }
        }
    }


}

// --- Home MVI Intent Definition ---
sealed interface HomeIntent {
    data class SearchQueryChanged(val query: String) : HomeIntent
    data class LabelSelected(val label: String?) : HomeIntent
    data object ResetFilters : HomeIntent
    data class AddGiftIdea(val lookupKey: String) : HomeIntent
    data class ToggleGiftIdea(val lookupKey: String, val idea: GiftIdea, val isChecked: Boolean) :
        HomeIntent

    data class DeleteGiftIdea(val lookupKey: String, val ideaId: String) : HomeIntent
    data class UpdateGiftIdeaText(val lookupKey: String, val ideaId: String, val newText: String) :
        HomeIntent

    data class UpdateBirthday(val contactId: String, val birthday: LocalDate) : HomeIntent
    data class SyncContacts(val showLoading: Boolean = false) : HomeIntent
    data object TriggerScrollToTop : HomeIntent
    data object TriggerSearchFocus : HomeIntent
    data object ConsumeSearchFocus : HomeIntent
    data object ConsumeNewlyAddedIdeaId : HomeIntent
    data class LinkAsCouple(val lookupKey1: String, val lookupKey2: String) : HomeIntent
    data class UnlinkCouple(val lookupKey: String) : HomeIntent
    data class IgnoreCoupleSuggestion(val lookupKey1: String, val lookupKey2: String) : HomeIntent
    data class SetIsResettingFilter(val isResetting: Boolean) : HomeIntent
    data object AppResumed : HomeIntent
    data class OpenBirthdayPicker(
        val contactLookupKey: String,
        val year: Int?,
        val month: Int,
        val day: Int,
    ) : HomeIntent
    data object DismissBirthdayPicker : HomeIntent
}
