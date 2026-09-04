package com.heckmannch.birthdaybuddy.ui.screens.settings.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.ui.model.NotificationUiState
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
    private val contactRepository: ContactRepository,
    private val permissionChecker: PermissionChecker,
) : ViewModel() {

    private val _hasNotificationPermission =
        MutableStateFlow(permissionChecker.hasNotificationPermission())

    val uiState: StateFlow<NotificationUiState> = combine(
        notificationRepository.settings,
        notificationRepository.allRules,
        _hasNotificationPermission
    ) { settings, rules, hasPermission ->
        NotificationUiState(
            notificationsEnabled = settings.notificationsEnabled,
            persistentNotifications = settings.persistentNotifications,
            otherEventsEnabled = settings.otherEventsEnabled,
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
            is NotificationIntent.SetOtherEventsEnabled -> setOtherEventsEnabled(intent.enabled)
            is NotificationIntent.AddRule -> addNotificationRule(
                intent.daysBefore,
                intent.hour,
                intent.minute
            )

            is NotificationIntent.UpdateRule -> updateNotificationRule(intent.rule)
            is NotificationIntent.DeleteRule -> deleteNotificationRule(intent.rule)
            is NotificationIntent.RefreshPermissionStatus -> refreshPermissionStatus()
        }
    }

    /**
     * Enables or disables global notifications.
     *
     * If enabled and no rules exist, a default rule (9:00 AM on the exact day) is automatically created.
     * Design: Immediate rules lookup ensures a fallback rule is set only when enabling notifications for the first time.
     *
     * @param enabled True to enable notifications, false to disable.
     */
    private fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        if (enabled) {
            val rules = notificationRepository.getAllRulesImmediate()
            if (rules.isEmpty()) {
                addNotificationRule(daysBefore = 0, hour = 9, minute = 0)
            }
        }
        notificationRepository.updateSettings { it.copy(notificationsEnabled = enabled) }
    }

    /**
     * Toggles whether notifications should be persistent (ongoing/non-dismissible).
     *
     * Design: Persistent notifications help prevent users from accidentally dismissing important reminders.
     *
     * @param persistent True to make notifications persistent, false otherwise.
     */
    private fun setPersistentNotifications(persistent: Boolean) = viewModelScope.launch {
        notificationRepository.updateSettings { it.copy(persistentNotifications = persistent) }
    }

    /**
     * Enables or disables notifications for other events like anniversaries and name days.
     *
     * Design: Toggling this setting triggers a full sync of contacts so that their non-birthday events
     * are correctly populated or cleared in the local cache.
     *
     * @param enabled True to enable other events, false to disable.
     */
    private fun setOtherEventsEnabled(enabled: Boolean) = viewModelScope.launch {
        notificationRepository.updateSettings { it.copy(otherEventsEnabled = enabled) }
        if (enabled) {
            contactRepository.syncContacts()
        }
    }

    private fun addNotificationRule(daysBefore: Int, hour: Int, minute: Int) =
        viewModelScope.launch {
            notificationRepository.insertRule(
                NotificationRule(
                    daysBefore = daysBefore,
                    hour = hour,
                    minute = minute
                )
            )
        }

    private fun updateNotificationRule(rule: NotificationRule) = viewModelScope.launch {
        notificationRepository.updateRule(rule)
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
    data class SetOtherEventsEnabled(val enabled: Boolean) : NotificationIntent
    data class AddRule(val daysBefore: Int, val hour: Int, val minute: Int) : NotificationIntent
    data class UpdateRule(val rule: NotificationRule) : NotificationIntent
    data class DeleteRule(val rule: NotificationRule) : NotificationIntent
    data object RefreshPermissionStatus : NotificationIntent
}
