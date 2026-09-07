package com.heckmannch.birthdaybuddy.data.repository

/**
 * Model representing calendar metadata from the Android system calendar,
 * decoupled from cursor-based implementations for improved testability.
 */
data class SystemCalendarInfo(
    val id: Long,
    val name: String,
    val accountName: String,
    val accountType: String,
    val displayName: String?,
    val visible: Int
)
