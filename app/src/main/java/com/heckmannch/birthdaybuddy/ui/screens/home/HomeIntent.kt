package com.heckmannch.birthdaybuddy.ui.screens.home

import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import com.heckmannch.birthdaybuddy.util.NO_YEAR_MARKER
import java.time.LocalDate

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
