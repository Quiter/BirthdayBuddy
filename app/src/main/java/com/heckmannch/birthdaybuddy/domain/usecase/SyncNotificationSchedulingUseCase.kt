package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import dagger.Reusable
import javax.inject.Inject

/**
 * Synchronizes notification scheduling based on current rules and settings
 * by delegating to [NotificationRepository.syncScheduling].
 */
@Reusable
class SyncNotificationSchedulingUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke() {
        notificationRepository.syncScheduling()
    }
}
