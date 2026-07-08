package com.heckmannch.birthdaybuddy.domain.model

/**
 * Pure domain model representing a pending notification.
 * Decoupled from any database-specific attributes.
 */
data class PendingNotification(
    val id: Int = 0,
    val contactLookupKeys: List<String>,
    val daysBefore: Int,
    val year: Int,
    val isDone: Boolean = false,
    val dismissCount: Int = 0
)
