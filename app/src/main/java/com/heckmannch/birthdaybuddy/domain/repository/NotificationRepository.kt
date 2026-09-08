package com.heckmannch.birthdaybuddy.domain.repository

import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.model.PendingNotification
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for managing notifications, rules, and schedules.
 */
interface NotificationRepository {
    val allRules: Flow<List<NotificationRule>>

    suspend fun syncScheduling()

    suspend fun getAllRulesImmediate(): List<NotificationRule>
    suspend fun insertRule(rule: NotificationRule)
    suspend fun updateRule(rule: NotificationRule)
    suspend fun deleteRule(rule: NotificationRule)

    // Pending Notifications
    suspend fun getActiveNotificationsImmediate(): List<PendingNotification>
    suspend fun insertPendingNotification(notification: PendingNotification): Long
    suspend fun getPendingNotificationById(id: Int): PendingNotification?

    /**
     * Retrieves all contact lookup keys (including prefixed keys such as "anniversary:..." or "nameday:...")
     * for notifications that have already been scheduled for the specified year and days-before lead time.
     *
     * @param year The calendar year of the notification event.
     * @param daysBefore The lead time in days.
     * @return A set of already scheduled contact lookup keys for fast in-memory lookups.
     */
    suspend fun getScheduledContactLookupKeys(year: Int, daysBefore: Int): Set<String>

    suspend fun hasNotificationBeenScheduled(
        year: Int,
        daysBefore: Int,
        lookupKey: String
    ): Boolean

    suspend fun incrementDismissCount(id: Int)
    suspend fun markAsDone(id: Int)
    suspend fun deleteOldNotifications(currentYear: Int)
}
