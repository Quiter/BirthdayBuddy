package com.heckmannch.birthdaybuddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.data.local.NotificationRule
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val settings = notificationRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val onboardingCompleted: StateFlow<Boolean?> = settings
        .map { it?.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setPersistentNotifications(persistent: Boolean) = viewModelScope.launch {
        notificationRepository.updateSettings(persistentNotifications = persistent)
    }

    fun completeOnboarding(notificationsEnabled: Boolean) = viewModelScope.launch {
        if (notificationsEnabled) {
            val rules = notificationRepository.getAllRulesImmediate()
            if (rules.isEmpty()) {
                notificationRepository.insertRule(
                    NotificationRule(
                        daysBefore = 0,
                        hour = 9,
                        minute = 0
                    )
                )
            }
        }
        notificationRepository.updateSettings(
            notificationsEnabled = notificationsEnabled,
            onboardingCompleted = true,
        )
    }
}
