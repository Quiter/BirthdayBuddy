package com.heckmannch.birthdaybuddy.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Repräsentiert einen Kontakt mit Geburtstag.
 */
@Entity(
    tableName = "contacts",
    indices = [Index(value = ["lookupKey"], unique = true)]
)
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,      // Interner Key für Room-Relationen
    val contactId: String,       // Aktuelle _ID vom Android-System (für schnellen Zugriff)
    val lookupKey: String,       // Stabiler Key vom Android-System (für Re-Sync)
    val fullName: String,
    val birthday: LocalDate,
    val imageUri: String? = null,
    val labels: List<String> = emptyList(),
    val giftIdeas: String? = null
)
