package com.heckmannch.birthdaybuddy.domain.repository

import com.heckmannch.birthdaybuddy.domain.model.Contact

/**
 * Domain repository interface for synchronizing birthdays, anniversaries, and name days with the system calendar.
 */
interface CalendarSyncRepository {

    enum class CalendarType {
        BIRTHDAY,
        ANNIVERSARY,
        NAMEDAY
    }

    fun hasCalendarPermissions(): Boolean
    suspend fun updateCalendarColor(type: CalendarType, newColor: Int): Boolean
    suspend fun deleteCalendar(): Boolean
    suspend fun syncBirthdays(contacts: List<Contact>): Boolean
}
