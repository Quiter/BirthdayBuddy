package com.heckmannch.birthdaybuddy.data.util

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

class AndroidCalendarStringProviderTest {

    private val mockContext: Context = mockk()
    private lateinit var provider: AndroidCalendarStringProvider

    @Before
    fun setUp() {
        provider = AndroidCalendarStringProvider(mockContext)
    }

    @Test
    fun calendarNames_delegateToCorrectResources() {
        every { mockContext.getString(R.string.calendar_name_birthdays) } returns "Birthdays"
        every { mockContext.getString(R.string.calendar_name_anniversaries) } returns "Anniversaries"
        every { mockContext.getString(R.string.calendar_name_namedays) } returns "Name Days"

        assertThat(provider.calendarNameBirthdays()).isEqualTo("Birthdays")
        assertThat(provider.calendarNameAnniversaries()).isEqualTo("Anniversaries")
        assertThat(provider.calendarNameNameDays()).isEqualTo("Name Days")

        assertThat(provider.calendarDisplayName(CalendarSyncRepository.CalendarType.BIRTHDAY)).isEqualTo("Birthdays")
        assertThat(provider.calendarDisplayName(CalendarSyncRepository.CalendarType.ANNIVERSARY)).isEqualTo("Anniversaries")
        assertThat(provider.calendarDisplayName(CalendarSyncRepository.CalendarType.NAMEDAY)).isEqualTo("Name Days")
    }

    @Test
    fun calendarEvents_delegateToCorrectResources() {
        every { mockContext.getString(R.string.calendar_event_title, "Alice") } returns "Alice's Birthday"
        every { mockContext.getString(R.string.calendar_event_birth_year, 1990) } returns "Birth year: 1990"
        every { mockContext.getString(R.string.calendar_event_no_year) } returns "No year"
        every { mockContext.getString(R.string.calendar_event_anniversary_title, "Alice") } returns "Alice's Anniversary"
        every { mockContext.getString(R.string.calendar_event_anniversary_title_couple, "Alice & Bob") } returns "Alice & Bob's Anniversary"
        every { mockContext.getString(R.string.calendar_event_anniversary_year, 2015) } returns "Wedding year: 2015"
        every { mockContext.getString(R.string.calendar_event_anniversary_no_year) } returns "No anniversary year"
        every { mockContext.getString(R.string.calendar_event_nameday_title, "Alice") } returns "Alice's Name Day"
        every { mockContext.getString(R.string.calendar_event_nameday_description, "Alice") } returns "Alice's Name Day Desc"

        assertThat(provider.calendarEventTitle("Alice")).isEqualTo("Alice's Birthday")
        assertThat(provider.calendarEventBirthYear(1990)).isEqualTo("Birth year: 1990")
        assertThat(provider.calendarEventNoYear()).isEqualTo("No year")
        assertThat(provider.calendarEventAnniversaryTitle("Alice")).isEqualTo("Alice's Anniversary")
        assertThat(provider.calendarEventAnniversaryTitleCouple("Alice & Bob")).isEqualTo("Alice & Bob's Anniversary")
        assertThat(provider.calendarEventAnniversaryYear(2015)).isEqualTo("Wedding year: 2015")
        assertThat(provider.calendarEventAnniversaryNoYear()).isEqualTo("No anniversary year")
        assertThat(provider.calendarEventNameDayTitle("Alice")).isEqualTo("Alice's Name Day")
        assertThat(provider.calendarEventNameDayDescription("Alice")).isEqualTo("Alice's Name Day Desc")
    }
}
