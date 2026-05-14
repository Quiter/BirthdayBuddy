package com.heckmannch.birthdaybuddy.viewmodel

import androidx.compose.runtime.Immutable

/**
 * UI-Modell für die Verwaltung von Labels in den Einstellungen.
 */
@Immutable
data class LabelManagementModel(
    val name: String,
    val isHiddenFromFilter: Boolean,
    val isIgnored: Boolean,
    val isSystem: Boolean,
)
