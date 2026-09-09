package com.heckmannch.birthdaybuddy.ui.screens.home

/**
 * Sealed interface representing one-shot UI events dispatched from [HomeViewModel] to the UI layer.
 *
 * Extracting transient events (such as search focus requests and newly added gift ideas) from
 * [com.heckmannch.birthdaybuddy.ui.model.HomeUiState] into a dedicated event stream prevents
 * unnecessary full-screen recompositions across state-consuming Composables.
 */
sealed interface HomeUiEvent {
    /**
     * Commands the UI to request focus on the search text field and show the soft keyboard.
     */
    data object RequestSearchFocus : HomeUiEvent

    /**
     * Commands the UI to focus a newly added gift idea item.
     *
     * @property ideaId Unique identifier of the newly created gift idea item.
     */
    data class FocusNewlyAddedIdea(val ideaId: String) : HomeUiEvent

    /**
     * Commands the UI to scroll the contact list back to the top item.
     */
    data object ScrollToTop : HomeUiEvent
}
