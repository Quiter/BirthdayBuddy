package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.calendar

import androidx.compose.runtime.Immutable
import com.heckmannch.birthdaybuddy.domain.model.AppSettings

/**
 * UI representation of a dynamic custom/other date-field event calendar color option.
 */
@Immutable
data class CustomEventCalendarColorItem(
    val eventKey: String,
    val color: Int,
)

@Immutable
data class CalendarUiState(
    val calendarSyncEnabled: Boolean = false,
    val otherEventsEnabled: Boolean = false,
    val anniversaryEnabled: Boolean = false,
    val nameDayEnabled: Boolean = false,
    val birthdayCalendarColor: Int = AppSettings.DEFAULT_BIRTHDAY_CALENDAR_COLOR,
    val anniversaryCalendarColor: Int = AppSettings.DEFAULT_ANNIVERSARY_CALENDAR_COLOR,
    val nameDayCalendarColor: Int = AppSettings.DEFAULT_NAMEDAY_CALENDAR_COLOR,
    val customEventCalendars: List<CustomEventCalendarColorItem> = emptyList(),
    val hasCalendarPermission: Boolean = false,
)
