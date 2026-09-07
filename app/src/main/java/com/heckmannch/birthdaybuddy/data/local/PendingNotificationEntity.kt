package com.heckmannch.birthdaybuddy.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a pending notification for upcoming birthdays/events.
 *
 * Indices:
 * - `isDone`: Accelerates queries for active notifications (`WHERE isDone = 0`)
 *   in [PendingNotificationDao.getActiveNotifications] and [PendingNotificationDao.getActiveNotificationsImmediate].
 * - `year`, `daysBefore`: Optimizes duplicate checks in [PendingNotificationDao.hasNotificationBeenScheduled]
 *   and cleanup queries in [PendingNotificationDao.deleteOldNotifications] to prevent full-table scans.
 */
@Entity(
    tableName = "pending_notifications",
    indices = [
        Index(value = ["isDone"]),
        Index(value = ["year", "daysBefore"])
    ]
)
data class PendingNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactLookupKeys: List<String>, // List of lookup keys of the affected contacts
    val daysBefore: Int,
    val year: Int,
    val isDone: Boolean = false,
    @ColumnInfo(defaultValue = "0") val dismissCount: Int = 0 // Counter for swipe/dismiss attempts
)
