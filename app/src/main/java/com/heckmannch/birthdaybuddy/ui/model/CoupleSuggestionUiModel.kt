package com.heckmannch.birthdaybuddy.ui.model

import androidx.compose.runtime.Immutable

/**
 * UI-Modell für einen Paar-Vorschlag.
 * Kapselt nur die für die Anzeige relevanten Daten, um die Room-Entity Contact
 * nicht in die UI-Schicht durchzureichen und die Compose-Stabilität zu gewährleisten.
 */
@Immutable
data class CoupleSuggestionUiModel(
    val firstLookupKey: String,
    val firstName: String,
    val firstImageUri: String?,
    val firstInitials: String,
    val secondLookupKey: String,
    val secondName: String,
    val secondImageUri: String?,
    val secondInitials: String,
)
