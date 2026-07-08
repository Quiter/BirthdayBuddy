package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.data.repository.CalendarSyncRepository
import dagger.Reusable
import javax.inject.Inject

/**
 * Domain Use Case to update the custom calendar color for birthdays, anniversaries, or name days.
 */
@Reusable
class UpdateCalendarColorUseCase @Inject constructor(
    private val calendarSyncRepository: CalendarSyncRepository
) {
    suspend operator fun invoke(type: CalendarSyncRepository.CalendarType, color: Int): Boolean {
        return calendarSyncRepository.updateCalendarColor(type, color)
    }
}
