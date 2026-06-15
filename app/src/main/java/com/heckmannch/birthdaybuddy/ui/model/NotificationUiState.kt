package com.heckmannch.birthdaybuddy.ui.model

import com.heckmannch.birthdaybuddy.data.local.NotificationRule

data class NotificationUiState(
    val notificationsEnabled: Boolean = false,
    val persistentNotifications: Boolean = true,
    val otherEventsEnabled: Boolean = false,
    val notificationRules: List<NotificationRule> = emptyList()
)
