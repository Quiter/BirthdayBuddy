package com.heckmannch.birthdaybuddy.viewmodel

import androidx.compose.runtime.Immutable

/**
 * Gebündelter UI-State für den Home-Bildschirm.
 */
@Immutable
data class HomeUiState(
    val contacts: List<ContactUiModel>? = null,
    val searchQuery: String = "",
    val availableLabels: List<String> = emptyList(),
    val selectedLabel: String? = null,
    val swipeHintShown: Boolean = true,
    val isResettingFilter: Boolean = false,
    val isSyncing: Boolean = false,
)
