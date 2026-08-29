package com.heckmannch.birthdaybuddy.ui.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/**
 * Hält ausstehende Bearbeitungsanfragen für Geburtstage (z. B. via AppFunctions Deep-Link).
 */
@Immutable
data class PendingBirthdayEdit(
    val contactId: String,
    val initialDate: LocalDate,
)

/**
 * Gebündelter UI-State für den Home-Bildschirm.
 */
@Immutable
data class HomeUiState(
    val contacts: List<ContactUiModel>? = null,
    val searchQuery: String = "",
    val availableLabels: List<String> = emptyList(),
    val selectedLabel: String? = null,
    val isResettingFilter: Boolean = false,
    val isSyncing: Boolean = false,
    val searchFocusRequested: Boolean = false,
    val newlyAddedIdeaId: String? = null,
    val coupleSuggestion: CoupleSuggestionUiModel? = null,
    val hasContactPermission: Boolean = false,
    val pendingBirthdayEdit: PendingBirthdayEdit? = null,
)
