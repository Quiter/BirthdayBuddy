package com.heckmannch.birthdaybuddy.domain.model

/**
 * Pure domain model representing application settings.
 * Decoupled from any database-specific attributes.
 */
data class AppSettings(
    val id: Int = 0,
    val notificationsEnabled: Boolean = false,
    val persistentNotifications: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val calendarSyncEnabled: Boolean = false,
    val calendarId: Long? = null,
    val otherEventsEnabled: Boolean = false,
    val ignoredCouplePairs: List<String> = emptyList(),
    val birthdayCalendarColor: Int = 0xFFE91E63.toInt(),
    val anniversaryCalendarColor: Int = 0xFF9C27B0.toInt(),
    val nameDayCalendarColor: Int = 0xFFFF9800.toInt(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeAmoled: Boolean = false,
    val themeAccent: ThemeAccent = ThemeAccent.SYSTEM,
    val customAccentColor: String? = null,
    val labelsEnabled: Boolean = true
)
