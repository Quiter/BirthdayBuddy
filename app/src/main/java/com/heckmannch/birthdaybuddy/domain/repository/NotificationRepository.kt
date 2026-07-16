package com.heckmannch.birthdaybuddy.domain.repository

import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.model.PendingNotification
import com.heckmannch.birthdaybuddy.domain.model.ThemeAccent
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for managing notifications, schedules, and app settings.
 */
interface NotificationRepository {
    val allRules: Flow<List<NotificationRule>>
    val settings: Flow<AppSettings>

    suspend fun syncScheduling()

    suspend fun updateSettings(
        notificationsEnabled: Boolean? = null,
        persistentNotifications: Boolean? = null,
        onboardingCompleted: Boolean? = null,
        lastSyncTimestamp: Long? = null,
        calendarSyncEnabled: Boolean? = null,
        calendarId: Long? = null,
        clearCalendarId: Boolean = false,
        otherEventsEnabled: Boolean? = null,
        birthdayCalendarColor: Int? = null,
        anniversaryCalendarColor: Int? = null,
        nameDayCalendarColor: Int? = null,
        themeMode: ThemeMode? = null,
        themeAmoled: Boolean? = null,
        themeAccent: ThemeAccent? = null,
        customAccentColor: String? = null
    )

    suspend fun getAllRulesImmediate(): List<NotificationRule>
    suspend fun insertRule(rule: NotificationRule)
    suspend fun updateRule(rule: NotificationRule)
    suspend fun deleteRule(rule: NotificationRule)

    // Pending Notifications
    suspend fun getActiveNotificationsImmediate(): List<PendingNotification>
    suspend fun insertPendingNotification(notification: PendingNotification): Long
    suspend fun getPendingNotificationById(id: Int): PendingNotification?
    suspend fun hasNotificationBeenScheduled(
        year: Int,
        daysBefore: Int,
        lookupKey: String
    ): Boolean

    suspend fun incrementDismissCount(id: Int)
    suspend fun markAsDone(id: Int)
    suspend fun deleteOldNotifications(currentYear: Int)
}
