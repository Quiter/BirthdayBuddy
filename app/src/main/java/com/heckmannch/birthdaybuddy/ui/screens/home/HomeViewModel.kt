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
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeViewModel.Companion.SEARCH_DEBOUNCE_DURATION
import com.heckmannch.birthdaybuddy.util.Clock
import com.heckmannch.birthdaybuddy.util.NO_YEAR_MARKER
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.Year
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * [HomeViewModel] is the architectural core and state engine for the main contacts and birthdays dashboard.
 *
 * ### Architectural & Design Patterns
 * - **MVI / UDF (Uni-Directional Data Flow)**:
 *   Employs strict Unidirectional Data Flow where the UI emits discrete events and commands ([HomeIntent])
 *   via [onIntent], and observes a single, immutable, read-only state representation ([HomeUiState]) via [uiState].
 * - **State Consolidation & Reactive Pipeline**:
 *   Combines multiple reactive streams (contact data from Room, label filters, search keyword transformations,
 *   couple suggestions, and transient user UI mutations encapsulated in internal [UserUiState]) into a unified
 *   [HomeUiState] using Kotlin Coroutines and Flows.
 * - **Debouncing & Transformation Optimization**:
 *   Search inputs are debounced ([SEARCH_DEBOUNCE_DURATION]) and processed on [Dispatchers.Default] with
 *   `distinctUntilChanged` to avoid redundant database querying and contact filtering computations during fast typing.
 * - **Lifecycle & Resource Awareness**:
 *   Exposes state via `stateIn` with `SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS)`, preventing
 *   unnecessary background processing and memory retention when UI subscriptions cease.
 *
 * @property contactRepository Repository managing contact persistence, labels, synchronization, and gift ideas.
 * @property contactUiMapper Mapper converting domain contact entities to presentation-ready [ContactUiModel]s.
 * @property coupleSuggestionUiMapper Mapper transforming domain couple suggestions to [CoupleSuggestionUiModel]s.
 * @property linkAsCoupleUseCase Use case linking two independent contacts into a single couple entity.
 * @property unlinkCoupleUseCase Use case decoupling a previously linked couple back into individual contacts.
 * @property ignoreCoupleSuggestionUseCase Use case marking a suggested couple pairing as ignored/dismissed.
 * @property permissionChecker Interface verifying system-level contact permissions.
 * @property clock Abstraction for system time facilitating deterministic testing and time manipulation.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
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

    /**
     * Tracks the timestamp (in milliseconds) of the most recent user interaction or resumed state,
     * used to calculate inactivity timeouts upon returning to the app.
     */
    private var lastInteractionTime: Long = clock.currentTimeMillis()

    // --- Search & Filter State (MVI Consolidated UI State) ---
    /**
     * Internal data holder encapsulating user-driven transient and local UI state mutations.
     *
     * @property searchQuery Current text query entered into the search input.
     * @property selectedLabel Currently active label chip filter, or `null` if none selected.
     * @property isResettingFilter Flag signalling UI components to coordinate scroll or animation reset.
     * @property isSyncing Flag indicating active background contact synchronization (shows refresh indicator).
     * @property searchFocusRequested One-shot flag commanding the UI to focus the search field and display soft keyboard.
     * @property newlyAddedIdeaId Unique identifier of a freshly created gift idea awaiting UI focus, or `null`.
     * @property hasContactPermission Cached status of system contact read/write permissions.
     * @property pendingBirthdayEdit Data for active birthday picker dialog, or `null` if closed.
     */
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

    /**
     * Evaluates whether the application currently holds system contact permissions.
     */
    private fun checkContactPermission(): Boolean {
        return permissionChecker.hasContactsPermission()
    }

    /**
     * Backing StateFlow for internal user UI state mutations.
     */
    private val _userUiState = MutableStateFlow(
        UserUiState(
            hasContactPermission = checkContactPermission()
        )
    )

    companion object {
        /** Delay applied to search queries to debounce rapid keystrokes before triggering list filtering. */
        private val SEARCH_DEBOUNCE_DURATION = 300.milliseconds

        /** Inactivity threshold (5 minutes). Exceeding this resets active search/label filters when app is resumed. */
        private val AUTO_RESET_INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000L

        /** Minimum visible duration for the manual pull-to-refresh spinner to prevent jarring UI flicker. */
        private const val MIN_SYNC_SPINNER_DURATION_MS = 800L

        /** Flow subscription stop timeout preventing upstream cancellation during brief configuration changes. */
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        /** Regex used to split search query strings into distinct whitespace-delimited keyword tokens. */
        private val WHITESPACE_REGEX = "\\s+".toRegex()
    }

    /**
     * Internal event channel dispatching one-shot scroll-to-top commands to the UI.
     */
    private val _scrollToTopEvent = MutableSharedFlow<Unit>(replay = 0)

    /**
     * Public stream of one-shot scroll-to-top events observed by [HomeScreen].
     */
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    /**
     * Internal event channel notifying when manual contact synchronization has completed.
     */
    private val _syncCompletedEvent = MutableSharedFlow<Unit>(replay = 0)

    /**
     * Public stream of synchronization completion events.
     */
    val syncCompletedEvent: SharedFlow<Unit> = _syncCompletedEvent.asSharedFlow()


    /**
     * Reactive stream combining label configurations and user preferences to determine ignored labels
     * and event filtering rules for contacts retrieval.
     */
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
        // Automatically trigger initial contact sync on ViewModel creation
        onIntent(HomeIntent.SyncContacts())
    }

    /**
     * Reactive stream of debounced search keyword lists derived from user input.
     * Emits immediately if the query is blank, or delays by [SEARCH_DEBOUNCE_DURATION] when typing.
     */
    private val searchKeywords = _userUiState
        .map { it.searchQuery.trim() }
        .distinctUntilChanged()
        .transformLatest { query ->
            if (query.isEmpty()) {
                emit(query)
            } else {
                delay(SEARCH_DEBOUNCE_DURATION)
                emit(query)
            }
        }
        .map { if (it.isEmpty()) emptyList() else it.split(WHITESPACE_REGEX) }
        .flowOn(Dispatchers.Default)

    /**
     * Reactive domain stream of filtered contact entities matching current search, label, and event settings.
     */
    private val filteredContacts = getContactsUseCase(
        contacts = contactRepository.allContacts,
        currentDate = timeRepository.currentDate,
        searchKeywords = searchKeywords,
        selectedLabel = _userUiState.map { it.selectedLabel }.distinctUntilChanged(),
        labelSettings = labelSettingsState,
    )

    /**
     * Reactive presentation stream transforming domain contacts into UI-ready [ContactUiModel]s,
     * computing remaining days, age calculations, relationship states, and visual formatting.
     */
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

    /**
     * Reactive stream of available label names for filter chip presentation.
     */
    private val availableLabels: Flow<List<String>> = getAvailableLabelsUseCase(
        contacts = contactRepository.allContacts,
        configs = contactRepository.labelConfigs,
        otherEventsEnabled = contactRepository.otherEventsEnabled,
        labelsEnabled = contactRepository.labelsEnabled,
    )

    /**
     * Reactive stream providing the top couple merge suggestion banner when applicable.
     */
    private val coupleSuggestion: Flow<CoupleSuggestionUiModel?> = getCoupleSuggestionUseCase(
        selectedLabel = _userUiState.map { it.selectedLabel }.distinctUntilChanged()
    ).map { suggestion ->
        suggestion?.let { coupleSuggestionUiMapper.toUiModel(it) }
    }.flowOn(Dispatchers.Default)

    /**
     * The consolidated read-only [HomeUiState] stream consumed by the UI layer.
     * Merges contacts list, filter labels, user input state, and active couple suggestions.
     */
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    /**
     * Dispatches an asynchronous one-shot event notifying the UI to scroll the contact list to the top.
     */
    private fun triggerScrollToTop() {
        viewModelScope.launch {
            _scrollToTopEvent.emit(Unit)
        }
    }

    /**
     * Clears both the search query and selected label filter in a single atomic update,
     * setting [UserUiState.isResettingFilter] to true and requesting a list scroll reset.
     */
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
    /**
     * Central MVI intent dispatcher processing incoming [HomeIntent] user actions and system events.
     *
     * Updates [lastInteractionTime] on all user-driven actions to maintain accurate inactivity tracking.
     *
     * @param intent The [HomeIntent] action to process.
     */
    fun onIntent(intent: HomeIntent) {
        if (intent !is HomeIntent.AppResumed) {
            lastInteractionTime = clock.currentTimeMillis()
        }
        when (intent) {
            // Re-evaluates contact permissions and resets active search/label filters if inactive for > 5 minutes.
            is HomeIntent.AppResumed -> {
                _userUiState.update { it.copy(hasContactPermission = checkContactPermission()) }
                if ((clock.currentTimeMillis() - lastInteractionTime) > AUTO_RESET_INACTIVITY_TIMEOUT_MS) {
                    resetFiltersInternal()
                }
                lastInteractionTime = clock.currentTimeMillis()
            }

            // Updates the search query text, clears label selection if transitioning from blank to typed, and scrolls to top.
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

            // Toggles or selects a category label filter chip and scrolls list to top.
            is HomeIntent.LabelSelected -> {
                _userUiState.update { state ->
                    val newLabel = if (state.selectedLabel == intent.label) null else intent.label
                    if (state.selectedLabel == newLabel) state
                    else state.copy(selectedLabel = newLabel, isResettingFilter = true)
                }
                triggerScrollToTop()
            }

            // Explicitly resets all active search queries and label filters.
            is HomeIntent.ResetFilters -> {
                resetFiltersInternal()
            }

            // Creates a new empty gift idea, sets newlyAddedIdeaId to trigger UI focus, and persists to repository.
            is HomeIntent.AddGiftIdea -> {
                val newIdea = GiftIdea(text = "")
                _userUiState.update { it.copy(newlyAddedIdeaId = newIdea.id) }
                viewModelScope.launch {
                    contactRepository.addGiftIdea(intent.lookupKey, newIdea)
                }
            }

            // Toggles the checked/purchased status of an existing gift idea item.
            is HomeIntent.ToggleGiftIdea -> {
                viewModelScope.launch {
                    contactRepository.toggleGiftIdea(
                        intent.lookupKey,
                        intent.idea,
                        intent.isChecked
                    )
                }
            }

            // Deletes a gift idea item by its unique ID.
            is HomeIntent.DeleteGiftIdea -> {
                viewModelScope.launch {
                    contactRepository.deleteGiftIdea(intent.lookupKey, intent.ideaId)
                }
            }

            // Updates the description text of an existing gift idea item.
            is HomeIntent.UpdateGiftIdeaText -> {
                viewModelScope.launch {
                    contactRepository.updateGiftIdeaText(
                        intent.lookupKey,
                        intent.ideaId,
                        intent.newText
                    )
                }
            }

            // Persists an updated birthday date for a specific contact.
            is HomeIntent.UpdateBirthday -> {
                viewModelScope.launch {
                    contactRepository.updateContactBirthday(intent.contactId, intent.birthday)
                }
            }

            // Synchronizes contacts with system provider; enforces an 800ms minimum spinner duration on manual pull-to-refresh.
            is HomeIntent.SyncContacts -> {
                _userUiState.update { it.copy(hasContactPermission = checkContactPermission()) }
                viewModelScope.launch {
                    val startTime = clock.currentTimeMillis()
                    if (intent.showLoading) {
                        _userUiState.update { it.copy(isSyncing = true) }
                        contactRepository.clearIgnoredCouplePairs()
                    }
                    try {
                        contactRepository.syncContacts()
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        // Re-throw CancellationException to ensure proper coroutine cancellation mechanics
                        throw e
                    } catch (e: Exception) {
                        // Prevent UI freezing/crashing on sync errors; further error handling/logging can be attached here
                    } finally {
                        if (intent.showLoading) {
                            val elapsedTime = clock.currentTimeMillis() - startTime
                            if (elapsedTime < MIN_SYNC_SPINNER_DURATION_MS) {
                                delay((MIN_SYNC_SPINNER_DURATION_MS - elapsedTime).milliseconds)
                            }
                            _userUiState.update { it.copy(isSyncing = false) }
                            _syncCompletedEvent.emit(Unit)
                        }
                    }
                }
            }

            // Dispatches a scroll-to-top request to the UI.
            is HomeIntent.TriggerScrollToTop -> {
                triggerScrollToTop()
            }

            // Signals the UI to request focus on the search text field and show keyboard.
            is HomeIntent.TriggerSearchFocus -> {
                _userUiState.update { it.copy(searchFocusRequested = true) }
            }

            // Consumes the search focus request after the UI has handled it.
            is HomeIntent.ConsumeSearchFocus -> {
                _userUiState.update { it.copy(searchFocusRequested = false) }
            }

            // Consumes the newly added gift idea ID after focus has been applied by the UI.
            is HomeIntent.ConsumeNewlyAddedIdeaId -> {
                _userUiState.update { it.copy(newlyAddedIdeaId = null) }
            }

            // Merges two independent contacts into a single couple entity.
            is HomeIntent.LinkAsCouple -> {
                viewModelScope.launch {
                    linkAsCoupleUseCase(intent.lookupKey1, intent.lookupKey2)
                }
            }

            // Unlinks a linked couple entity back into two separate individual contacts.
            is HomeIntent.UnlinkCouple -> {
                viewModelScope.launch {
                    unlinkCoupleUseCase(intent.lookupKey)
                }
            }

            // Dismisses and ignores a couple pairing suggestion.
            is HomeIntent.IgnoreCoupleSuggestion -> {
                viewModelScope.launch {
                    ignoreCoupleSuggestionUseCase(intent.lookupKey1, intent.lookupKey2)
                }
            }

            // Updates the filter resetting state flag used for animation synchronization.
            is HomeIntent.SetIsResettingFilter -> {
                _userUiState.update { it.copy(isResettingFilter = intent.isResetting) }
            }

            // Resolves the contact and safely normalizes leap day (Feb 29) & day bounds before opening the birthday picker dialog.
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
                        val targetYear =
                            if (intent.year != null && intent.year > 0 && intent.year != NO_YEAR_MARKER) intent.year else NO_YEAR_MARKER
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

            // Dismisses the active birthday picker dialog and clears pending edit state.
            is HomeIntent.DismissBirthdayPicker -> {
                _userUiState.update { it.copy(pendingBirthdayEdit = null) }
            }
        }
    }


}

// --- Home MVI Intent Definition ---
/**
 * Sealed interface defining all possible user interactions, lifecycle events, and commands
 * dispatched to [HomeViewModel] in accordance with the MVI / UDF architecture.
 */
sealed interface HomeIntent {
    /**
     * Dispatched when the user updates text in the search input field.
     *
     * @property query The updated search text query.
     */
    data class SearchQueryChanged(val query: String) : HomeIntent

    /**
     * Dispatched when the user taps on a category label filter chip.
     *
     * @property label The selected label name, or `null` to clear label filtering.
     */
    data class LabelSelected(val label: String?) : HomeIntent

    /**
     * Dispatched to reset all active search queries and label filters to their default state.
     */
    data object ResetFilters : HomeIntent

    /**
     * Dispatched when the user initiates adding a new gift idea to a contact.
     *
     * @property lookupKey Unique lookup key of the target contact.
     */
    data class AddGiftIdea(val lookupKey: String) : HomeIntent

    /**
     * Dispatched when the user checks or unchecks a gift idea item.
     *
     * @property lookupKey Unique lookup key of the contact owning the gift idea.
     * @property idea The [GiftIdea] model instance being toggled.
     * @property isChecked New completion/purchased state.
     */
    data class ToggleGiftIdea(val lookupKey: String, val idea: GiftIdea, val isChecked: Boolean) :
        HomeIntent

    /**
     * Dispatched when the user deletes a gift idea from a contact.
     *
     * @property lookupKey Unique lookup key of the contact owning the gift idea.
     * @property ideaId Unique identifier of the gift idea to remove.
     */
    data class DeleteGiftIdea(val lookupKey: String, val ideaId: String) : HomeIntent

    /**
     * Dispatched when the user modifies the text content of a gift idea.
     *
     * @property lookupKey Unique lookup key of the contact owning the gift idea.
     * @property ideaId Unique identifier of the gift idea being edited.
     * @property newText Updated description text.
     */
    data class UpdateGiftIdeaText(val lookupKey: String, val ideaId: String, val newText: String) :
        HomeIntent

    /**
     * Dispatched when the user confirms an updated birthday date for a contact.
     *
     * @property contactId Raw Android contact provider ID.
     * @property birthday New [LocalDate] birthday representation.
     */
    data class UpdateBirthday(val contactId: String, val birthday: LocalDate) : HomeIntent

    /**
     * Dispatched to trigger contact synchronization with the system Contacts Provider.
     *
     * @property showLoading When `true`, displays the loading spinner and enforces minimum spinner duration.
     */
    data class SyncContacts(val showLoading: Boolean = false) : HomeIntent

    /**
     * Dispatched to programmatically request the contact list to scroll back to the first item.
     */
    data object TriggerScrollToTop : HomeIntent

    /**
     * Dispatched to programmatically request focus and soft keyboard on the search bar (e.g. via keyboard shortcut).
     */
    data object TriggerSearchFocus : HomeIntent

    /**
     * Dispatched by the UI after successfully applying focus to the search bar to consume the one-shot state.
     */
    data object ConsumeSearchFocus : HomeIntent

    /**
     * Dispatched by the UI after successfully applying focus to a newly added gift idea item.
     */
    data object ConsumeNewlyAddedIdeaId : HomeIntent

    /**
     * Dispatched when the user confirms linking two contacts together into a couple entity.
     *
     * @property lookupKey1 Lookup key of the primary contact.
     * @property lookupKey2 Lookup key of the partner contact.
     */
    data class LinkAsCouple(val lookupKey1: String, val lookupKey2: String) : HomeIntent

    /**
     * Dispatched when the user unlinks an existing couple back into individual contacts.
     *
     * @property lookupKey Lookup key of the couple or member contact to unlink.
     */
    data class UnlinkCouple(val lookupKey: String) : HomeIntent

    /**
     * Dispatched when the user dismisses or ignores a suggested couple pairing.
     *
     * @property lookupKey1 Lookup key of the first contact.
     * @property lookupKey2 Lookup key of the second contact.
     */
    data class IgnoreCoupleSuggestion(val lookupKey1: String, val lookupKey2: String) : HomeIntent

    /**
     * Dispatched to notify the ViewModel whether a filter reset animation is currently in progress.
     *
     * @property isResetting `true` while the reset animation/transition is active, `false` once settled.
     */
    data class SetIsResettingFilter(val isResetting: Boolean) : HomeIntent

    /**
     * Dispatched when the application lifecycle returns to the foreground (resumed).
     * Triggers permission re-check and checks 5-minute inactivity timeout.
     */
    data object AppResumed : HomeIntent

    /**
     * Dispatched to open the birthday date picker dialog for a specific contact.
     *
     * Performs leap-year normalization and bounds checking prior to showing the dialog.
     *
     * @property contactLookupKey Lookup key or ID of the contact whose birthday is being edited.
     * @property year Selected birth year, or `null` / [NO_YEAR_MARKER] if unknown.
     * @property month Month of birth (1-12).
     * @property day Day of birth (1-31).
     */
    data class OpenBirthdayPicker(
        val contactLookupKey: String,
        val year: Int?,
        val month: Int,
        val day: Int,
    ) : HomeIntent

    /**
     * Dispatched when the birthday date picker dialog is dismissed or cancelled without saving.
     */
    data object DismissBirthdayPicker : HomeIntent
}
