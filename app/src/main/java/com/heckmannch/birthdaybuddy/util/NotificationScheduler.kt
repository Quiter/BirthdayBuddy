package com.heckmannch.birthdaybuddy.util

import com.heckmannch.birthdaybuddy.data.local.NotificationRule

interface NotificationScheduler {
    fun scheduleNext(rules: List<NotificationRule>)
    fun cancelNotification()
}
