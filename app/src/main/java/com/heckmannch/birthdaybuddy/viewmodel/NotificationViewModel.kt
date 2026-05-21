package com.heckmannch.birthdaybuddy.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.data.local.NotificationRule
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.components.NotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
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

    init {
        // Automatische Worker-Synchronisation
        viewModelScope.launch {
            combine(notificationsEnabled, notificationRules) { enabled, rules ->
                enabled to rules
            }.collect { (enabled, rules) ->
                if (rules == null) return@collect

                if (enabled && rules.isNotEmpty()) {
                    NotificationWorker.scheduleNext(context, rules)
                } else {
                    NotificationWorker.cancelNotification(context)
                }
            }
        }
    }

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
