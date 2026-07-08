package com.heckmannch.birthdaybuddy.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val contactRepository: ContactRepository,
) : ViewModel() {

    val onboardingCompleted: StateFlow<Boolean?> = notificationRepository.settings
        .map<AppSettings, Boolean?> { it.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setPersistentNotifications(persistent: Boolean) = viewModelScope.launch {
        notificationRepository.updateSettings(persistentNotifications = persistent)
    }

    fun completeOnboarding(
        notificationsEnabled: Boolean,
        calendarSyncEnabled: Boolean,
    ) = viewModelScope.launch {
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
            calendarSyncEnabled = calendarSyncEnabled,
            onboardingCompleted = true,
        )

        // Trigger initial background sync of contacts
        launch {
            contactRepository.syncContacts()
        }
    }
}
