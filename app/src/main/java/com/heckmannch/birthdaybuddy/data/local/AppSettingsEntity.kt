package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode

/**
 * Room entity representing global application settings and user preferences.
 * Only a single record with [id] = 0 is maintained in the database.
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 0, // Single record with ID 0
    val notificationsEnabled: Boolean = false,
    val persistentNotifications: Boolean = true, // Notifications must be explicitly dismissed/acknowledged
    val onboardingCompleted: Boolean = false, // Flag indicating whether initial onboarding has been completed
    val lastSyncTimestamp: Long = 0L, // Timestamp of the last successful synchronization
    val calendarSyncEnabled: Boolean = false,
    val calendarId: Long? = null,
    val otherEventsEnabled: Boolean = false,
    val ignoredCouplePairs: List<String> = emptyList(),
    val birthdayCalendarColor: Int = 0xFFE91E63.toInt(),
    val anniversaryCalendarColor: Int = 0xFF9C27B0.toInt(),
    val nameDayCalendarColor: Int = 0xFFFF9800.toInt(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeAmoled: Boolean = false,
    val themeAccent: String = "SYSTEM",
    val labelsEnabled: Boolean = true
)
