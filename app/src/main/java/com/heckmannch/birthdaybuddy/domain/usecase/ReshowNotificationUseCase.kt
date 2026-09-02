package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.domain.repository.NotificationScheduler
import dagger.Reusable
import javax.inject.Inject

/**
 * Handles re-showing a dismissed notification by delegating to the platform-specific
 * [NotificationScheduler].
 */
@Reusable
class ReshowNotificationUseCase @Inject constructor(
    private val notificationScheduler: NotificationScheduler,
) {
    operator fun invoke(
        pendingId: Int,
        daysBefore: Int,
        lookupKeys: List<String>,
        eventType: EventType,
        delayMillis: Long = 500L,
    ) {
        notificationScheduler.reshowNotification(
            pendingId = pendingId,
            daysBefore = daysBefore,
            lookupKeys = lookupKeys,
            eventType = eventType,
            delayMillis = delayMillis,
        )
    }
}
