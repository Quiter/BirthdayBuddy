package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import dagger.Reusable
import javax.inject.Inject

/**
 * Domain Use Case to update the custom calendar color for birthdays, anniversaries,
 * name days, or custom date-field events.
 */
@Reusable
class UpdateCalendarColorUseCase @Inject constructor(
    private val calendarSyncRepository: CalendarSyncRepository
) {
    suspend operator fun invoke(type: CalendarSyncRepository.CalendarType, color: Int): Boolean {
        return calendarSyncRepository.updateCalendarColor(type, color)
    }

    suspend operator fun invoke(eventKey: String, color: Int): Boolean {
        return when (eventKey) {
            ContactLabels.LABEL_ANNIVERSARY ->
                calendarSyncRepository.updateCalendarColor(CalendarSyncRepository.CalendarType.ANNIVERSARY, color)
            ContactLabels.LABEL_NAME_DAY ->
                calendarSyncRepository.updateCalendarColor(CalendarSyncRepository.CalendarType.NAMEDAY, color)
            else ->
                calendarSyncRepository.updateCustomEventCalendarColor(eventKey, color)
        }
    }
}
