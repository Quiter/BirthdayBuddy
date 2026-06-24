package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 0, // Es gibt nur einen Datensatz mit ID 0
    val notificationsEnabled: Boolean = false,
    val persistentNotifications: Boolean = true, // NEU: Benachrichtigungen müssen aktiv quittiert werden
    val onboardingCompleted: Boolean = false, // NEU: Flag für den Erststart
    val lastSyncTimestamp: Long = 0L, // Zeitstempel des letzten erfolgreichen Syncs
    val calendarSyncEnabled: Boolean = false,
    val calendarId: Long? = null,
    val otherEventsEnabled: Boolean = false,
    val ignoredCouplePairs: List<String> = emptyList(),
    val birthdayCalendarColor: Int = 0xFFE91E63.toInt(),
    val anniversaryCalendarColor: Int = 0xFF9C27B0.toInt(),
    val nameDayCalendarColor: Int = 0xFFFF9800.toInt(),
    val themeMode: String = "SYSTEM",
    val themeAmoled: Boolean = false,
    val themeAccent: String = "SYSTEM",
    val labelsEnabled: Boolean = true
)
