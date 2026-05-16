package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 0, // Es gibt nur einen Datensatz mit ID 0
    val notificationsEnabled: Boolean = false,
    val persistentNotifications: Boolean = true, // NEU: Benachrichtigungen müssen aktiv quittiert werden
    val swipeHintShown: Boolean = false,
    val onboardingCompleted: Boolean = false, // NEU: Flag für den Erststart
    val lastSyncTimestamp: Long = 0L // Zeitstempel des letzten erfolgreichen Syncs
)
