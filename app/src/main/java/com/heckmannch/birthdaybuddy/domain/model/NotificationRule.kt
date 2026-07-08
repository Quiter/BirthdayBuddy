package com.heckmannch.birthdaybuddy.domain.model

/**
 * Pure domain model representing a notification rule.
 * Decoupled from any database-specific attributes.
 */
data class NotificationRule(
    val id: Int = 0,
    val daysBefore: Int, // 0 = today, 1 = 1 day before, 7 = a week before, etc.
    val hour: Int,
    val minute: Int
)
