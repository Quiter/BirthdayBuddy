package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.data.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
import dagger.Reusable
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Domain Use Case to trigger birthday and anniversary calendar synchronization.
 * It will run the sync only if calendar sync is enabled in user settings.
 */
@Reusable
class SyncCalendarUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val calendarSyncRepository: CalendarSyncRepository,
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke() {
        val settings = notificationRepository.settings.first()
        if (settings.calendarSyncEnabled) {
            val contacts = contactRepository.getAllContactsImmediate()
            calendarSyncRepository.syncBirthdays(contacts)
        }
    }
}
