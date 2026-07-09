package com.heckmannch.birthdaybuddy.ui.model

data class CalendarUiState(
    val calendarSyncEnabled: Boolean = false,
    val otherEventsEnabled: Boolean = false,
    val birthdayCalendarColor: Int = 0xFFE91E63.toInt(),
    val anniversaryCalendarColor: Int = 0xFF9C27B0.toInt(),
    val nameDayCalendarColor: Int = 0xFFFF9800.toInt(),
    val hasCalendarPermission: Boolean = false
)
