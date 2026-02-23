package com.heckmannch.birthdaybuddy.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Repräsentiert einen Geburtstagskontakt in der lokalen Datenbank.
 * 
 * Diese Klasse dient gleichzeitig als Room-Entität für die Persistenz 
 * und als serialisierbares Modell für den Datenaustausch (z.B. mit dem Widget).
 * 
 * @param id Eindeutige ID (entspricht der Android Contact-ID).
 * @param name Der Anzeigename des Kontakts.
 * @param birthday Das Geburtsdatum als String (Format: YYYY-MM-DD).
 * @param labels Liste der zugeordneten Gruppen/Label-Namen.
 * @param remainingDays Tage bis zum nächsten Geburtstag (0 = heute).
 * @param age Das Alter, das die Person am nächsten Geburtstag erreicht.
 * @param actions Verfügbare Kommunikationswege (Messenger, Mail etc.).
 * @param photoUri Lokaler Pfad zum Kontaktbild.
 */
@Entity(tableName = "birthdays")
@Serializable
data class BirthdayContact(
    @PrimaryKey val id: String,
    val name: String,
    val birthday: String,
    val labels: List<String>,
    val remainingDays: Int,
    val age: Int,
    val actions: ContactActions = ContactActions(),
    val photoUri: String? = null
)

/**
 * Hilfsklasse zur Kapselung der verfügbaren Messenger-Dienste und Kontaktwege.
 * 
 * @param hasWhatsApp True, wenn eine WhatsApp-ID für diesen Kontakt gefunden wurde.
 */
@Serializable
data class ContactActions(
    val phoneNumber: String? = null,
    val email: String? = null,
    val hasWhatsApp: Boolean = false,
    val hasSignal: Boolean = false,
    val hasTelegram: Boolean = false
)
