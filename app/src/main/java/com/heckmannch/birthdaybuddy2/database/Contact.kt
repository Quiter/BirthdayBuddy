package com.heckmannch.birthdaybuddy2.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Repräsentiert einen Kontakt mit Geburtstag.
 * Diese Klasse kann später um Felder wie Geschenkideen oder Synchronisations-IDs erweitert werden.
 */
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey
    val id: String, // Hier nutzen wir die Android Contact ID
    val fullName: String,
    val birthday: LocalDate,
    val imageUri: String? = null,
    // Platzhalter für zukünftige Erweiterungen
    val giftIdeas: String? = null,
    val remoteId: String? = null
)
