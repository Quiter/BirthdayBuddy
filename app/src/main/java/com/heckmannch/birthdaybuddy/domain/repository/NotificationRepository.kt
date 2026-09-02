package com.heckmannch.birthdaybuddy.domain.repository

import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.model.PendingNotification
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for managing notifications, schedules, and app settings.
 */
interface NotificationRepository {
    val allRules: Flow<List<NotificationRule>>
    val settings: Flow<AppSettings>

    suspend fun syncScheduling()

    /**
     * Updates application settings by applying the given transformation function [transform]
     * to the current settings state.
     *
     * @param transform Function to apply modifications to the current [AppSettings].
     */
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings)

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
