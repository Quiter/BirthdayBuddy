package com.heckmannch.birthdaybuddy.notification

object NotificationActions {
    // Actions
    const val ACTION_SNOOZE = "com.heckmannch.birthdaybuddy.action.SNOOZE"
    const val ACTION_DONE = "com.heckmannch.birthdaybuddy.action.DONE"
    const val ACTION_DISMISSED = "com.heckmannch.birthdaybuddy.action.DISMISSED"

    // Extra & WorkData Keys
    const val EXTRA_NOTIFICATION_ID = "NOTIFICATION_ID"
    const val EXTRA_PENDING_ID = "PENDING_ID"
    const val EXTRA_DAYS_BEFORE = "DAYS_BEFORE"
    const val EXTRA_LOOKUP_KEYS = "LOOKUP_KEYS"
    const val EXTRA_EVENT_TYPE = "EVENT_TYPE"

    // Notification Channels
    const val CHANNEL_ID = "birthday_reminders_v2"

    // WorkManager Work Names & Tags
    const val WORK_NAME_NOTIFICATION_UPDATE = "FlexibleNotificationUpdate"
    const val WORK_TAG_NOTIFICATION = "birthday_notification"
    const val WORK_TAG_SNOOZE = "notification_snooze"
}

