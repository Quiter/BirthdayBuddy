package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.NotificationScheduler
import dagger.Reusable
import javax.inject.Inject

/**
 * Handles the snooze action for a notification by delegating to the platform-specific
 * [NotificationScheduler].
 */
@Reusable
class SnoozeNotificationUseCase @Inject constructor(
    private val notificationScheduler: NotificationScheduler,
) {
    operator fun invoke(pendingId: Int, daysBefore: Int, lookupKeys: List<String>) {
        notificationScheduler.snoozeNotification(pendingId, daysBefore, lookupKeys)
    }
}
