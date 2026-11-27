package com.heckmannch.birthdaybuddy.domain.repository

import com.heckmannch.birthdaybuddy.domain.model.Contact

/**
 * Domain repository interface for synchronizing birthdays, anniversaries, name days,
 * and custom date-field events with the system calendar.
 */
interface CalendarSyncRepository {

    enum class CalendarType {
        BIRTHDAY,
        ANNIVERSARY,
        NAMEDAY
    }

    fun hasCalendarPermissions(): Boolean
    suspend fun updateCalendarColor(type: CalendarType, newColor: Int): Boolean
    suspend fun updateCustomEventCalendarColor(eventKey: String, newColor: Int): Boolean = false
    suspend fun deleteCalendar(): Boolean
    suspend fun syncBirthdays(contacts: List<Contact>): Boolean
}
