package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea

/**
 * Speichert benutzerdefinierte Daten zu einem Kontakt, die unabhängig vom System-Cache sind.
 * Diese Tabelle wird in der SettingsDatabase gespeichert und somit in der Cloud gesichert.
 */
@Entity(tableName = "contact_user_data")
data class ContactUserData(
    @PrimaryKey
    val lookupKey: String,
    val giftIdeas: List<GiftIdea> = emptyList(),
    val spouseLookupKey: String? = null
)
