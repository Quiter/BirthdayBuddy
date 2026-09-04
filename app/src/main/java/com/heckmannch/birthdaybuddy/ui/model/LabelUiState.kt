package com.heckmannch.birthdaybuddy.ui.model

import androidx.compose.runtime.Immutable

/**
 * Coherent UI state for the label settings screen.
 *
 * @property labelsEnabled Whether label management and filtering are enabled globally.
 * @property labels The list of configurable labels in use.
 */
@Immutable
data class LabelUiState(
    val labelsEnabled: Boolean = true,
    val labels: List<LabelManagementModel> = emptyList(),
)
