package com.heckmannch.birthdaybuddy.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores custom user data for a contact independent of the system contacts cache.
 * This table is stored in SettingsDatabase and is thus backed up to cloud backups.
 */
@Entity(tableName = "contact_user_data")
data class ContactUserData(
    @PrimaryKey
    val lookupKey: String,
    @ColumnInfo(name = "giftIdeas")
    val giftIdeasJson: String = "[]",
    val spouseLookupKey: String? = null
)
