package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import dagger.Reusable
import javax.inject.Inject

/**
 * Marks a pending notification as completed in the local database
 * by delegating to [NotificationRepository.markAsDone].
 */
@Reusable
class MarkNotificationAsDoneUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(pendingId: Int) {
        notificationRepository.markAsDone(pendingId)
    }
}
