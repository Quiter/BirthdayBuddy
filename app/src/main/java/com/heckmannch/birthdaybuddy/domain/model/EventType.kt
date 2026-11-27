package com.heckmannch.birthdaybuddy.domain.model

/**
 * Type-safe discriminator for the currently displayed or notified event type.
 *
 * Replaces the previously used [String]-based approach ("birthday", "anniversary", "name_day")
 * to prevent typos and silent failures in filtering and mapping logic.
 */
enum class EventType {
    /** Birthday – default event type */
    BIRTHDAY,

    /** Anniversary / Wedding day – active only when "Other events" and anniversary are enabled */
    ANNIVERSARY,

    /** Name day – active only when "Other events" and name day are enabled */
    NAME_DAY,

    /** Generic other event (`Event.TYPE_OTHER`) or user-defined custom date-field event (`Event.TYPE_CUSTOM`) */
    CUSTOM,
}
