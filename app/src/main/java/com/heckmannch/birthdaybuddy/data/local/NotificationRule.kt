package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_rules")
data class NotificationRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val daysBefore: Int, // 0 = heute, 1 = gestern, 7 = eine woche vorher
    val hour: Int,
    val minute: Int
)
