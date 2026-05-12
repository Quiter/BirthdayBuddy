package com.heckmannch.birthdaybuddy.viewmodel

import androidx.compose.runtime.Immutable

/**
 * UI-Modell für einen Kontakt. 
 * Alle Daten sind bereits für die Anzeige vorformatiert.
 */
@Immutable
data class ContactUiModel(
    val id: String, 
    val contactId: String,
    val lookupKey: String,
    val fullName: String,
    val dateText: String,
    val monthName: String,
    val imageUri: String?,
    val initials: String,
    val nextAge: Int?,
    val nextAgeText: String?,
    val daysUntilNext: Long,
    val daysLeftText: String,
    val isToday: Boolean,
    val labels: List<String>,
    val giftIdeas: List<GiftIdea>,
) {
    val hasGiftIdeas: Boolean get() = giftIdeas.any { it.text.isNotBlank() }
}

@Immutable
data class LabelManagementModel(
    val name: String,
    val isHiddenFromFilter: Boolean,
    val isIgnored: Boolean,
    val isSystem: Boolean,
)
