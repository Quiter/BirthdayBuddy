package com.heckmannch.birthdaybuddy.ui.model

import androidx.compose.runtime.Immutable
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule

@Immutable
data class NotificationUiState(
    val notificationsEnabled: Boolean = false,
    val persistentNotifications: Boolean = true,
    val otherEventsEnabled: Boolean = false,
    // List ist ein instabiler Typ für den Compose-Compiler. @Immutable ist dennoch sicher,
    // da diese Data Class schreibgeschützt (read-only) ist und Änderungen nur per Kopie (copy) erfolgen.
    val notificationRules: List<NotificationRule> = emptyList(),
    val hasSystemNotificationPermission: Boolean = true
)
