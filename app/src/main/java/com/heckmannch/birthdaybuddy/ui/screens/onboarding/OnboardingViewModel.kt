package com.heckmannch.birthdaybuddy.ui.screens.onboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.ui.model.OnboardingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel managing the onboarding setup flow for BirthdayBuddy.
 *
 * Coordinates permissions checks, initial user preferences (such as persistent notifications,
 * reminder schedules, and calendar sync), and persists initial settings atomically when onboarding completes.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val contactRepository: ContactRepository,
    private val permissionChecker: PermissionChecker,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _currentPage = MutableStateFlow(0)
    private val _permissions = MutableStateFlow(checkPermissions())

    /**
     * UI state combining current page, permission statuses, and notification settings.
     */
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

    /**
     * Emits whether onboarding has been completed, mapped from [AppSettings].
     */
    val onboardingCompleted: StateFlow<Boolean?> = notificationRepository.settings
        .map<AppSettings, Boolean?> { it.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Dispatches user intents to appropriate handler methods.
     *
     * @param intent The [OnboardingIntent] to handle.
     */
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

            is OnboardingIntent.CompleteOnboarding -> {
                completeOnboarding(
                    notificationsEnabled = intent.notificationsEnabled,
                    calendarSyncEnabled = intent.calendarSyncEnabled
                )
            }
        }
    }

    private fun setPersistentNotifications(persistent: Boolean) = viewModelScope.launch {
        notificationRepository.updateSettings(persistentNotifications = persistent)
    }

    /**
     * Completes the onboarding workflow by persisting user preferences, creating a default
     * notification rule if enabled and none exist, updating global settings, and triggering
     * the initial contact synchronization.
     *
     * Design: Uses [NonCancellable] combined with [Dispatchers.IO] to guarantee that all persistence
     * operations complete atomically without being cancelled when navigation occurs and the
     * [OnboardingViewModel] is cleared.
     *
     * @param notificationsEnabled Whether birthday notifications should be enabled.
     * @param calendarSyncEnabled Whether calendar synchronization should be enabled.
     */
    private fun completeOnboarding(
        notificationsEnabled: Boolean,
        calendarSyncEnabled: Boolean,
    ) = viewModelScope.launch {
        withContext(NonCancellable + ioDispatcher) {
            try {
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
                contactRepository.syncContacts()
            } catch (e: Exception) {
                Log.e(TAG, "Error completing onboarding: ${e.message}", e)
            }
        }
    }

    private fun checkPermissions(): OnboardingPermissions {
        val hasContact = permissionChecker.hasContactsPermission()
        val hasNotif = permissionChecker.hasNotificationPermission()
        val hasCalendar = permissionChecker.hasCalendarPermission()

        return OnboardingPermissions(hasContact, hasNotif, hasCalendar)
    }

    private data class OnboardingPermissions(
        val hasContact: Boolean,
        val hasNotification: Boolean,
        val hasCalendar: Boolean
    )

    companion object {
        private const val TAG = "OnboardingViewModel"
    }
}

/**
 * UI intents representing user actions within the onboarding flow.
 */
sealed interface OnboardingIntent {
    /**
     * Refreshes runtime permission states.
     */
    data object RefreshPermissions : OnboardingIntent

    /**
     * Updates the persistent notification preference.
     */
    data class SetPersistentNotifications(val enabled: Boolean) : OnboardingIntent

    /**
     * Updates the active onboarding page index.
     */
    data class SetCurrentPage(val page: Int) : OnboardingIntent

    /**
     * Finalizes onboarding with chosen notification and calendar sync configurations.
     */
    data class CompleteOnboarding(
        val notificationsEnabled: Boolean,
        val calendarSyncEnabled: Boolean
    ) : OnboardingIntent
}
