package com.heckmannch.birthdaybuddy.data.util

import android.content.Context
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.util.CalendarStringProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Android implementation of [CalendarStringProvider] that resolves localized string
 * resources using [Context].
 */
class AndroidCalendarStringProvider @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : CalendarStringProvider {

    override fun calendarNameBirthdays(): String =
        appContext.getString(R.string.calendar_name_birthdays)

    override fun calendarNameAnniversaries(): String =
        appContext.getString(R.string.calendar_name_anniversaries)

    override fun calendarNameNameDays(): String =
        appContext.getString(R.string.calendar_name_namedays)

    override fun calendarDisplayName(type: CalendarSyncRepository.CalendarType): String =
        when (type) {
            CalendarSyncRepository.CalendarType.BIRTHDAY -> calendarNameBirthdays()
            CalendarSyncRepository.CalendarType.ANNIVERSARY -> calendarNameAnniversaries()
            CalendarSyncRepository.CalendarType.NAMEDAY -> calendarNameNameDays()
        }

    override fun calendarEventTitle(contactName: String): String =
        appContext.getString(R.string.calendar_event_title, contactName)

    override fun calendarEventBirthYear(year: Int): String =
        appContext.getString(R.string.calendar_event_birth_year, year)

    override fun calendarEventNoYear(): String =
        appContext.getString(R.string.calendar_event_no_year)

    override fun calendarEventAnniversaryTitle(contactName: String): String =
        appContext.getString(R.string.calendar_event_anniversary_title, contactName)

    override fun calendarEventAnniversaryTitleCouple(coupleName: String): String =
        appContext.getString(R.string.calendar_event_anniversary_title_couple, coupleName)

    override fun calendarEventAnniversaryYear(year: Int): String =
        appContext.getString(R.string.calendar_event_anniversary_year, year)

    override fun calendarEventAnniversaryNoYear(): String =
        appContext.getString(R.string.calendar_event_anniversary_no_year)

    override fun calendarEventNameDayTitle(contactName: String): String =
        appContext.getString(R.string.calendar_event_nameday_title, contactName)

    override fun calendarEventNameDayDescription(contactName: String): String =
        appContext.getString(R.string.calendar_event_nameday_description, contactName)
}
