package com.heckmannch.birthdaybuddy.ui.model

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
    val notificationsEnabled: Boolean = true,
    val showInWidget: Boolean = true,
)
