package com.heckmannch.birthdaybuddy.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Represents a contact with birthday and event details in the database.
 *
 * Indices:
 * - `lookupKey` (unique): Unique and fast identification for re-sync and associations.
 * - `birthday`: Accelerates chronological sorting and filtering of all contacts by birth date.
 * - `anniversary`: Optimizes retrieval of potential couples ([ContactDao.getPotentialCouples]) through fast
 *   access to anniversary and wedding dates without performing a full-table scan.
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
    val localId: Long = 0,      // Internal key for Room relations
    val contactId: String,       // Current _ID from Android system (for fast access)
    val lookupKey: String,       // Stable key from Android system (for re-sync)
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
    @ColumnInfo(name = "giftIdeas")
    val giftIdeasJson: String = "[]",
    val spouseLookupKey: String? = null
)
