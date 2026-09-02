package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import dagger.Reusable
import javax.inject.Inject

/**
 * Domain Use Case to toggle calendar synchronization state.
 * If enabling, it saves settings and immediately synchronizes contacts.
 * If disabling, it saves settings and removes the synced calendar from the system.
 */
@Reusable
class SetCalendarSyncEnabledUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val calendarSyncRepository: CalendarSyncRepository,
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        notificationRepository.updateSettings { it.copy(calendarSyncEnabled = enabled) }
        if (enabled) {
            val contacts = contactRepository.getAllContactsImmediate()
            calendarSyncRepository.syncBirthdays(contacts)
        } else {
            calendarSyncRepository.deleteCalendar()
        }
    }
}
