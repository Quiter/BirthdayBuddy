package com.heckmannch.birthdaybuddy.model

// NEU: Ein kleiner Container für alle verfügbaren Kontakt-Aktionen
data class ContactActions(
    val phoneNumber: String? = null,
    val email: String? = null,
    val hasWhatsApp: Boolean = false,
    val hasSignal: Boolean = false,
    val hasTelegram: Boolean = false
)

data class BirthdayContact(
    val name: String,
    val birthday: String,
    val labels: List<String>,
    val remainingDays: Int,
    val age: Int,
    // NEU: Wir hängen die Aktionen an jeden Kontakt an
    val actions: ContactActions = ContactActions()
)