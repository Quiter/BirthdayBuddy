package com.heckmannch.birthdaybuddy.domain.util

import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository

/**
 * Interface providing localized strings for calendar synchronization (such as calendar
 * display names, event titles, and event descriptions).
 *
 * Decouples the domain and repository layers from Android platform resources ([android.content.Context]),
 * enabling pure JVM unit tests without Android runtime dependencies.
 */
interface CalendarStringProvider {

    /** Localized display name for the birthdays calendar. */
    fun calendarNameBirthdays(): String

    /** Localized display name for the anniversaries calendar. */
    fun calendarNameAnniversaries(): String

    /** Localized display name for the name days calendar. */
    fun calendarNameNameDays(): String

    /** Localized display name for a given [CalendarSyncRepository.CalendarType]. */
    fun calendarDisplayName(type: CalendarSyncRepository.CalendarType): String

    /** Localized event title for a birthday event (e.g. "Max's Birthday"). */
    fun calendarEventTitle(contactName: String): String

    /** Localized event description for a birthday with known birth year (e.g. "Birth year: 1990"). */
    fun calendarEventBirthYear(year: Int): String

    /** Localized event description for a birthday without birth year. */
    fun calendarEventNoYear(): String

    /** Localized event title for an individual's wedding anniversary. */
    fun calendarEventAnniversaryTitle(contactName: String): String

    /** Localized event title for a couple's wedding anniversary. */
    fun calendarEventAnniversaryTitleCouple(coupleName: String): String

    /** Localized event description for an anniversary with known wedding year. */
    fun calendarEventAnniversaryYear(year: Int): String

    /** Localized event description for an anniversary without wedding year. */
    fun calendarEventAnniversaryNoYear(): String

    /** Localized event title for a name day. */
    fun calendarEventNameDayTitle(contactName: String): String

    /** Localized event description for a name day. */
    fun calendarEventNameDayDescription(contactName: String): String
}
