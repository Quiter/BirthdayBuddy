package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a notification scheduling rule.
 * Specifies how many days before an event and at what time a notification is fired.
 */
@Entity(tableName = "notification_rules")
data class NotificationRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val daysBefore: Int, // 0 = today (event day), 1 = 1 day before the event, 7 = 1 week before
    val hour: Int,
    val minute: Int
)
