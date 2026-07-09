package com.heckmannch.birthdaybuddy.ui.screens.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.ui.model.OnboardingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val contactRepository: ContactRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _currentPage = MutableStateFlow(0)
    private val _permissions = MutableStateFlow(checkPermissions())

    val uiState: StateFlow<OnboardingUiState> = combine(
        notificationRepository.settings,
        _currentPage,
        _permissions
    ) { settings, currentPage, permissions ->
        OnboardingUiState(
            hasContactPermission = permissions.hasContact,
            hasNotificationPermission = permissions.hasNotification,
            hasCalendarPermission = permissions.hasCalendar,
            isPersistentNotificationEnabled = settings.persistentNotifications,
            currentPage = currentPage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OnboardingUiState()
    )

    val onboardingCompleted: StateFlow<Boolean?> = notificationRepository.settings
        .map<AppSettings, Boolean?> { it.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            is OnboardingIntent.RefreshPermissions -> {
                _permissions.value = checkPermissions()
            }
            is OnboardingIntent.SetPersistentNotifications -> {
                setPersistentNotifications(intent.enabled)
            }
            is OnboardingIntent.SetCurrentPage -> {
                _currentPage.value = intent.page
            }
        }
    }

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

    private fun checkPermissions(): OnboardingPermissions {
        val hasContact = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val hasCalendar = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_CALENDAR
                ) == PackageManager.PERMISSION_GRANTED

        return OnboardingPermissions(hasContact, hasNotif, hasCalendar)
    }

    private data class OnboardingPermissions(
        val hasContact: Boolean,
        val hasNotification: Boolean,
        val hasCalendar: Boolean
    )
}

sealed interface OnboardingIntent {
    data object RefreshPermissions : OnboardingIntent
    data class SetPersistentNotifications(val enabled: Boolean) : OnboardingIntent
    data class SetCurrentPage(val page: Int) : OnboardingIntent
}
