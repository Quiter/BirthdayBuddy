package com.heckmannch.birthdaybuddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.data.local.NotificationRule
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val settings = notificationRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val notificationsEnabled: StateFlow<Boolean> = settings
        .filterNotNull()
        .map { it.notificationsEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val persistentNotifications: StateFlow<Boolean> = settings
        .filterNotNull()
        .map { it.persistentNotifications }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationRules: StateFlow<List<NotificationRule>?> = notificationRepository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)



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
