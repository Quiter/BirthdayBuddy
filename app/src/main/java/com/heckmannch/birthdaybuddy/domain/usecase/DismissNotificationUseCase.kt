package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import dagger.Reusable
import javax.inject.Inject

/**
 * Increments the dismiss counter of a pending notification
 * by delegating to [NotificationRepository.incrementDismissCount].
 */
@Reusable
class DismissNotificationUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(pendingId: Int) {
        notificationRepository.incrementDismissCount(pendingId)
    }
}
