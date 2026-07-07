package com.heckmannch.birthdaybuddy.ui.screens.settings.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.data.local.NotificationRule
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.ui.model.NotificationUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val contactRepository: ContactRepository,
) : ViewModel() {

    val uiState: StateFlow<NotificationUiState> = combine(
        notificationRepository.settings,
        notificationRepository.allRules
    ) { settings, rules ->
        NotificationUiState(
            notificationsEnabled = settings.notificationsEnabled,
            persistentNotifications = settings.persistentNotifications,
            otherEventsEnabled = settings.otherEventsEnabled,
            notificationRules = rules
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationUiState()
    )


    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        if (enabled) {
            val rules = notificationRepository.getAllRulesImmediate()
            if (rules.isEmpty()) {
                addNotificationRule(daysBefore = 0, hour = 9, minute = 0)
            }
        }
        notificationRepository.updateSettings(notificationsEnabled = enabled)
    }

    fun setPersistentNotifications(persistent: Boolean) = viewModelScope.launch {
        notificationRepository.updateSettings(persistentNotifications = persistent)
    }

    fun setOtherEventsEnabled(enabled: Boolean) = viewModelScope.launch {
        notificationRepository.updateSettings(otherEventsEnabled = enabled)
        if (enabled) {
            contactRepository.syncContacts()
        }
    }

    fun addNotificationRule(daysBefore: Int, hour: Int, minute: Int) = viewModelScope.launch {
        notificationRepository.insertRule(
            NotificationRule(
                daysBefore = daysBefore,
                hour = hour,
                minute = minute
            )
        )
    }

    fun updateNotificationRule(rule: NotificationRule) = viewModelScope.launch {
        notificationRepository.updateRule(rule)
    }

    fun deleteNotificationRule(rule: NotificationRule) = viewModelScope.launch {
        notificationRepository.deleteRule(rule)
    }
}
