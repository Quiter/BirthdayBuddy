package com.heckmannch.birthdaybuddy.domain.model

import java.time.LocalDate

/**
 * Repräsentiert einen Kontakt mit Geburtstag im Domain-Layer.
 * Vollständig entkoppelt von der Datenbank-Implementierung.
 */
data class Contact(
    val localId: Long = 0,
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
