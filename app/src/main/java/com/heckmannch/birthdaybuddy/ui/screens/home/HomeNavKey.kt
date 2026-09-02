package com.heckmannch.birthdaybuddy.ui.screens.home

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation keys for list-detail adaptive navigation in the home screen.
 */
@Serializable
sealed interface HomeNavKey : NavKey {
    /**
     * Represents the primary contact list pane.
     */
    @Serializable
    data object ContactList : HomeNavKey

    /**
     * Represents the contact detail pane for a specific contact.
     *
     * @property contactId Unique identifier of the selected contact.
     */
    @Serializable
    data class ContactDetail(val contactId: String) : HomeNavKey
}

/**
 * Safely updates the backstack with the selected contact detail pane,
 * replacing any existing detail entry to prevent backstack growth.
 */
fun MutableList<NavKey>.navigateToContactDetail(contactId: String) {
    if (lastOrNull() is HomeNavKey.ContactDetail) {
        removeLastOrNull()
    }
    add(HomeNavKey.ContactDetail(contactId))
}
