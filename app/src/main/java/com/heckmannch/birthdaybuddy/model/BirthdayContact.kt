package com.heckmannch.birthdaybuddy.model

/**
 * Hält Informationen über verfügbare Kontaktmöglichkeiten eines Nutzers bereit.
 */
data class ContactActions(
    val phoneNumber: String? = null,
    val email: String? = null,
    val hasWhatsApp: Boolean = false,
    val hasSignal: Boolean = false,
    val hasTelegram: Boolean = false
)

/**
 * Repräsentiert einen Geburtstagskontakt.
 * @param id Die eindeutige Android-Kontakt-ID.
 */
data class BirthdayContact(
    val id: String,
    val name: String,
    val birthday: String,
    val labels: List<String>,
    val remainingDays: Int,
    val age: Int,
    val actions: ContactActions = ContactActions(),
    val photoUri: String? = null
)
