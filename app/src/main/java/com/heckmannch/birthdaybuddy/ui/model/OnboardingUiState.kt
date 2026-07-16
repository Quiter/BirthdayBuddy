package com.heckmannch.birthdaybuddy.ui.model

import androidx.compose.runtime.Immutable

@Immutable
data class OnboardingUiState(
    val hasContactPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val hasCalendarPermission: Boolean = false,
    val isPersistentNotificationEnabled: Boolean = false,
    val currentPage: Int = 0
)
