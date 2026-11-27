package com.heckmannch.birthdaybuddy.domain.repository

import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule

/**
 * Domain interface for scheduling and managing notifications.
 */
interface NotificationScheduler {
    fun scheduleNext(rules: List<NotificationRule>)
    fun cancelNotification()

    /**
     * Cancels all pending snoozed or re-shown notification jobs scheduled via WorkManager.
     */
    fun cancelAllSnoozedNotifications()
    fun snoozeNotification(pendingId: Int, daysBefore: Int, lookupKeys: List<String>)
    fun reshowNotification(
        pendingId: Int,
        daysBefore: Int,
        lookupKeys: List<String>,
        eventType: EventType,
        delayMillis: Long = 500L
    )
}

