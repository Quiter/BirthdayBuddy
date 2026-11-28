package com.heckmannch.birthdaybuddy.domain.model

/**
 * Pure domain model representing application settings.
 * Decoupled from any database-specific attributes.
 */
data class AppSettings(
    val id: Int = 0,
    val notificationsEnabled: Boolean = false,
    val persistentNotifications: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val calendarSyncEnabled: Boolean = false,
    val calendarId: Long? = null,
    val otherEventsEnabled: Boolean = false,
    val showOtherEventsInHomeList: Boolean = false,
    val disabledOtherEventLabels: List<String> = emptyList(),
    val ignoredCouplePairs: List<String> = emptyList(),
    val birthdayCalendarColor: Int = DEFAULT_BIRTHDAY_CALENDAR_COLOR,
    val anniversaryCalendarColor: Int = DEFAULT_ANNIVERSARY_CALENDAR_COLOR,
    val nameDayCalendarColor: Int = DEFAULT_NAMEDAY_CALENDAR_COLOR,
    val customEventCalendarColors: Map<String, Int> = emptyMap(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeAmoled: Boolean = false,
    val themeAccent: ThemeAccent = ThemeAccent.SYSTEM,
    val customAccentColor: String? = null,
    val labelsEnabled: Boolean = true
) {
    /**
     * Returns `true` if "Other events" is globally enabled and [eventKey] has not been
     * individually disabled by the user.
     */
    fun isOtherEventEnabled(eventKey: String): Boolean =
        otherEventsEnabled && eventKey !in disabledOtherEventLabels

    /**
     * Resolves the calendar color for a given non-birthday [eventKey], falling back to the
     * standard default color for custom events if no explicit color has been configured yet.
     */
    fun calendarColorForEvent(eventKey: String): Int = when (eventKey) {
        ContactLabels.LABEL_ANNIVERSARY -> anniversaryCalendarColor
        ContactLabels.LABEL_NAME_DAY -> nameDayCalendarColor
        else -> customEventCalendarColors[eventKey] ?: DEFAULT_CUSTOM_EVENT_CALENDAR_COLOR
    }

    companion object {
        val DEFAULT_BIRTHDAY_CALENDAR_COLOR: Int = 0xFFE91E63.toInt()
        val DEFAULT_ANNIVERSARY_CALENDAR_COLOR: Int = 0xFF9C27B0.toInt()
        val DEFAULT_NAMEDAY_CALENDAR_COLOR: Int = 0xFFFF9800.toInt()
        val DEFAULT_CUSTOM_EVENT_CALENDAR_COLOR: Int = 0xFF2196F3.toInt()
    }
}
