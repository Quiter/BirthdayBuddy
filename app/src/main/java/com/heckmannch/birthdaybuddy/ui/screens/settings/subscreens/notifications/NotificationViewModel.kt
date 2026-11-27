package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository,
    private val permissionChecker: PermissionChecker,
) : ViewModel() {

    private val _hasNotificationPermission =
        MutableStateFlow(permissionChecker.hasNotificationPermission())

    val uiState: StateFlow<NotificationUiState> = combine(
        settingsRepository.settings,
        notificationRepository.allRules,
        _hasNotificationPermission
    ) { settings, rules, hasPermission ->
        NotificationUiState(
            notificationsEnabled = settings.notificationsEnabled,
            persistentNotifications = settings.persistentNotifications,
            notificationRules = rules,
            hasSystemNotificationPermission = hasPermission
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationUiState(
            hasSystemNotificationPermission = permissionChecker.hasNotificationPermission()
        )
    )


    fun onIntent(intent: NotificationIntent) {
        when (intent) {
            is NotificationIntent.SetEnabled -> setNotificationsEnabled(intent.enabled)
            is NotificationIntent.SetPersistent -> setPersistentNotifications(intent.persistent)
            is NotificationIntent.AddRule -> addNotificationRule(
                intent.daysBefore,
                intent.hour,
                intent.minute
            )

            is NotificationIntent.UpdateRule -> updateNotificationRule(intent.rule)
            is NotificationIntent.ReplaceRule -> replaceNotificationRule(
                intent.oldRule,
                intent.newDaysBefore,
                intent.newHour,
                intent.newMinute
            )

            is NotificationIntent.DeleteRule -> deleteNotificationRule(intent.rule)
            is NotificationIntent.RefreshPermissionStatus -> refreshPermissionStatus()
        }
    }

    /**
     * Enables or disables global notifications.
     *
     * If enabled and no rules exist, a default rule (9:00 AM on the exact day) is automatically created
     * sequentially before updating settings and rescheduling alarms.
     * Design: Immediate rules lookup ensures a fallback rule is set only when enabling notifications for the first time.
     *
     * @param enabled True to enable notifications, false to disable.
     */
    private fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        if (enabled) {
            val rules = notificationRepository.getAllRulesImmediate()
            if (rules.isEmpty()) {
                insertRuleIfMissing(daysBefore = 0, hour = 9, minute = 0)
            }
        }
        settingsRepository.updateSettings { it.copy(notificationsEnabled = enabled) }
        notificationRepository.syncScheduling()
    }

    /**
     * Toggles whether notifications should be persistent (ongoing/non-dismissible).
     *
     * Design: Persistent notifications help prevent users from accidentally dismissing important reminders.
     *
     * @param persistent True to make notifications persistent, false otherwise.
     */
    private fun setPersistentNotifications(persistent: Boolean) = viewModelScope.launch {
        settingsRepository.updateSettings { it.copy(persistentNotifications = persistent) }
    }

    private suspend fun insertRuleIfMissing(daysBefore: Int, hour: Int, minute: Int) {
        val existingRules = notificationRepository.getAllRulesImmediate()
        if (existingRules.any { it.daysBefore == daysBefore }) {
            return
        }
        notificationRepository.insertRule(
            NotificationRule(
                daysBefore = daysBefore,
                hour = hour,
                minute = minute
            )
        )
    }

    private fun addNotificationRule(daysBefore: Int, hour: Int, minute: Int) =
        viewModelScope.launch {
            insertRuleIfMissing(daysBefore, hour, minute)
        }

    private fun updateNotificationRule(rule: NotificationRule) = viewModelScope.launch {
        notificationRepository.updateRule(rule)
    }

    /**
     * Replaces an existing notification rule with a new lead time and time of day.
     *
     * Checks whether [newDaysBefore] is already occupied before deleting [oldRule] so that
     * duplicate target lead times abort cleanly without silently deleting the original rule.
     */
    private fun replaceNotificationRule(
        oldRule: NotificationRule,
        newDaysBefore: Int,
        newHour: Int,
        newMinute: Int
    ) = viewModelScope.launch {
        if (oldRule.daysBefore == newDaysBefore) {
            notificationRepository.updateRule(
                oldRule.copy(hour = newHour, minute = newMinute)
            )
            return@launch
        }
        val existingRules = notificationRepository.getAllRulesImmediate()
        if (existingRules.any { it.daysBefore == newDaysBefore }) {
            return@launch
        }
        notificationRepository.deleteRule(oldRule)
        insertRuleIfMissing(
            daysBefore = newDaysBefore,
            hour = newHour,
            minute = newMinute
        )
    }

    private fun deleteNotificationRule(rule: NotificationRule) = viewModelScope.launch {
        notificationRepository.deleteRule(rule)
    }

    private fun refreshPermissionStatus() {
        _hasNotificationPermission.value = permissionChecker.hasNotificationPermission()
    }
}

sealed interface NotificationIntent {
    data class SetEnabled(val enabled: Boolean) : NotificationIntent
    data class SetPersistent(val persistent: Boolean) : NotificationIntent
    data class AddRule(val daysBefore: Int, val hour: Int, val minute: Int) : NotificationIntent
    data class UpdateRule(val rule: NotificationRule) : NotificationIntent
    data class ReplaceRule(
        val oldRule: NotificationRule,
        val newDaysBefore: Int,
        val newHour: Int,
        val newMinute: Int
    ) : NotificationIntent

    data class DeleteRule(val rule: NotificationRule) : NotificationIntent
    data object RefreshPermissionStatus : NotificationIntent
}

