package com.heckmannch.birthdaybuddy.ui.model

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
    val phoneNumber: String?,
    val initials: String,
    val nextAge: Int?,
    val daysUntilNext: Long,
    val isToday: Boolean,
    val hasWhatsApp: Boolean,
    val hasSignal: Boolean,
    val labels: List<String>,
    val giftIdeas: List<GiftIdea>,
) {
    val hasGiftIdeas: Boolean get() = giftIdeas.any { it.text.isNotBlank() }
}
