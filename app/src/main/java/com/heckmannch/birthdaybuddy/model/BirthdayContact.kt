package com.heckmannch.birthdaybuddy.model

/**
 * Hält Informationen über verfügbare Kontaktmöglichkeiten eines Nutzers bereit.
 * 
 * @param phoneNumber Die primäre Telefonnummer (falls vorhanden).
 * @param email Die primäre E-Mail-Adresse.
 * @param hasWhatsApp Gibt an, ob der Kontakt über WhatsApp erreichbar ist.
 * @param hasSignal Gibt an, ob der Kontakt über Signal erreichbar ist.
 * @param hasTelegram Gibt an, ob der Kontakt über Telegram erreichbar ist.
 */
data class ContactActions(
    val phoneNumber: String? = null,
    val email: String? = null,
    val hasWhatsApp: Boolean = false,
    val hasSignal: Boolean = false,
    val hasTelegram: Boolean = false
)

/**
 * Repräsentiert einen Geburtstagskontakt mit allen für die App relevanten Daten.
 * 
 * @param name Der vollständige Name des Kontakts.
 * @param birthday Das Geburtsdatum im Format "YYYY-MM-DD" oder "--MM-DD".
 * @param labels Liste der Gruppen/Labels, denen der Kontakt in Android zugeordnet ist.
 * @param remainingDays Tage bis zum nächsten Geburtstag.
 * @param age Das Alter, das die Person am nächsten Geburtstag erreicht.
 * @param actions Verfügbare Kommunikationswege.
 * @param photoUri URI zum Profilbild des Kontakts.
 */
data class BirthdayContact(
    val name: String,
    val birthday: String,
    val labels: List<String>,
    val remainingDays: Int,
    val age: Int,
    val actions: ContactActions = ContactActions(),
    val photoUri: String? = null
)
