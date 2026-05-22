package com.heckmannch.birthdaybuddy.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.heckmannch.birthdaybuddy.ui.model.GiftIdea
import java.time.LocalDate

/**
 * Repräsentiert einen Kontakt mit Geburtstag.
 */
@Entity(
    tableName = "contacts",
    indices = [
        Index(value = ["lookupKey"], unique = true),
        Index(value = ["birthday"])
    ]
)
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,      // Interner Key für Room-Relationen
    val contactId: String,       // Aktuelle _ID vom Android-System (für schnellen Zugriff)
    val lookupKey: String,       // Stabiler Key vom Android-System (für Re-Sync)
    val fullName: String,
    val birthday: LocalDate? = null,
    val imageUri: String? = null,
    val phoneNumber: String? = null,
    @ColumnInfo(defaultValue = "0")
    val hasWhatsApp: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val hasSignal: Boolean = false,
    val labels: List<String> = emptyList(),
    val giftIdeas: List<GiftIdea> = emptyList()
)
