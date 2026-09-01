package com.heckmannch.birthdaybuddy.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import java.time.LocalDate

/**
 * Repräsentiert einen Kontakt mit Geburtstag in der Datenbank.
 *
 * Indizes:
 * - `lookupKey` (unique): Eindeutige und schnelle Identifikation für Re-Sync und Verknüpfungen.
 * - `birthday`: Beschleunigt die chronologische Sortierung und Filterung aller Kontakte nach Geburtsdatum.
 * - `anniversary`: Optimiert die Ermittlung potenzieller Paare ([ContactDao.getPotentialCouples]) durch schnellen
 *   Zugriff auf Jubiläums- und Hochzeitstagsdaten ohne Full-Table-Scan.
 */
@Entity(
    tableName = "contacts",
    indices = [
        Index(value = ["lookupKey"], unique = true),
        Index(value = ["birthday"]),
        Index(value = ["anniversary"])
    ]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,      // Interner Key für Room-Relationen
    val contactId: String,       // Aktuelle _ID vom Android-System (für schnellen Zugriff)
    val lookupKey: String,       // Stabiler Key vom Android-System (für Re-Sync)
    val fullName: String,
    val birthday: LocalDate? = null,
    val anniversary: LocalDate? = null,
    val nameDay: LocalDate? = null,
    val imageUri: String? = null,
    val phoneNumber: String? = null,
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val hasWhatsApp: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val hasSignal: Boolean = false,
    val labels: List<String> = emptyList(),
    val giftIdeas: List<GiftIdea> = emptyList(),
    val spouseLookupKey: String? = null
)
