package com.heckmannch.birthdaybuddy.domain.model

/**
 * Type-safe discriminator for the currently displayed event type.
 *
 * Replaces the previously used [String]-based approach ("birthday", "anniversary", "name_day")
 * to prevent typos and silent failures in filtering and mapping logic.
 */
enum class EventType {
    /** Birthday – default event type */
    BIRTHDAY,

    /** Anniversary / Wedding day – active only when "Other events" is enabled */
    ANNIVERSARY,

    /** Name day – active only when "Other events" is enabled */
    NAME_DAY,
}
