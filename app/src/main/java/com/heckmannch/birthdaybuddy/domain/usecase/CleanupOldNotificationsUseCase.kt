package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import dagger.Reusable
import javax.inject.Inject

/**
 * Deletes old, completed pending notifications from previous years
 * by delegating to [NotificationRepository.deleteOldNotifications].
 */
@Reusable
class CleanupOldNotificationsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(year: Int) {
        notificationRepository.deleteOldNotifications(year)
    }
}
