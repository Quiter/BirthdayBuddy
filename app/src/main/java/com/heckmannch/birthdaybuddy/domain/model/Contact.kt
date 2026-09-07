package com.heckmannch.birthdaybuddy.domain.model

import java.time.LocalDate

/**
 * Represents a contact with birthday and event details in the domain layer.
 * Completely decoupled from database implementation details.
 */
data class Contact(
    val contactId: String,
    val lookupKey: String,
    val fullName: String,
    val birthday: LocalDate? = null,
    val anniversary: LocalDate? = null,
    val nameDay: LocalDate? = null,
    val imageUri: String? = null,
    val phoneNumber: String? = null,
    val isFavorite: Boolean = false,
    val hasWhatsApp: Boolean = false,
    val hasSignal: Boolean = false,
    val labels: List<String> = emptyList(),
    val giftIdeas: List<GiftIdea> = emptyList(),
    val spouseLookupKey: String? = null
)
