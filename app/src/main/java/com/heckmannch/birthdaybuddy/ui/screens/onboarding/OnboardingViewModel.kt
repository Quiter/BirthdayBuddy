package com.heckmannch.birthdaybuddy.ui.screens.onboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.di.IoDispatcher
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel managing the onboarding setup flow for BirthdayBuddy following MVI / UDF architecture.
 *
 * **Responsibilities & Purpose**:
 * - Manages the onboarding multi-step setup flow, including welcome, permission requests, notification settings, and calendar sync configuration.
 * - Coordinates runtime permission verification across contacts, notifications, and calendar.
 * - Aggregates multiple asynchronous streams (repository settings, page state, permissions) into a single unified [uiState].
 * - Persists onboarding preferences atomically and triggers initial contact synchronization upon completion.
 *
 * **MVI & Flow Aggregation**:
 * - **Intent Handling**: Receives unidirectional intents via [onIntent] to drive state transitions or trigger side effects.
 * - **State Aggregation**: Uses [combine] to merge [NotificationRepository.settings], internal page state, and permissions into a reactive [StateFlow] of [OnboardingUiState], scoped to [viewModelScope] with [SharingStarted.WhileSubscribed].
 *
 * @param notificationRepository Repository for retrieving and persisting notification settings and rules.
 * @param contactRepository Repository for managing contacts and executing initial contact synchronization.
 * @param permissionChecker Utility for verifying Android runtime permissions.
 * @param ioDispatcher Coroutine dispatcher dedicated to I/O and persistence operations.
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
     * Emits updates to subscribers while the UI is active.
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
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = OnboardingUiState()
    )

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

    /**
     * Asynchronously updates the persistent notification preference in [NotificationRepository].
     *
     * @param persistent `true` if a persistent notification should be displayed, `false` otherwise.
     */
    private fun setPersistentNotifications(persistent: Boolean) = viewModelScope.launch {
        notificationRepository.updateSettings { it.copy(persistentNotifications = persistent) }
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
                                daysBefore = DEFAULT_RULE_DAYS_BEFORE,
                                hour = DEFAULT_RULE_HOUR,
                                minute = DEFAULT_RULE_MINUTE
                            )
                        )
                    }
                }

                notificationRepository.updateSettings {
                    it.copy(
                        notificationsEnabled = notificationsEnabled,
                        calendarSyncEnabled = calendarSyncEnabled,
                        onboardingCompleted = true
                    )
                }

                // Trigger initial background sync of contacts
                contactRepository.syncContacts()
            } catch (e: Exception) {
                Log.e(TAG, "Error completing onboarding: ${e.message}", e)
            }
        }
    }

    /**
     * Queries [PermissionChecker] for contacts, notification, and calendar runtime permissions
     * and packages the results into an [OnboardingPermissions] snapshot.
     *
     * @return [OnboardingPermissions] containing the current grant status of each required permission.
     */
    private fun checkPermissions(): OnboardingPermissions {
        val hasContact = permissionChecker.hasContactsPermission()
        val hasNotif = permissionChecker.hasNotificationPermission()
        val hasCalendar = permissionChecker.hasCalendarPermission()

        return OnboardingPermissions(hasContact, hasNotif, hasCalendar)
    }

    /**
     * Internal data structure representing a snapshot of runtime permission states.
     *
     * @property hasContact Whether READ_CONTACTS permission is granted.
     * @property hasNotification Whether POST_NOTIFICATIONS permission is granted.
     * @property hasCalendar Whether READ_CALENDAR/WRITE_CALENDAR permission is granted.
     */
    private data class OnboardingPermissions(
        val hasContact: Boolean,
        val hasNotification: Boolean,
        val hasCalendar: Boolean
    )

    companion object {
        private const val TAG = "OnboardingViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val DEFAULT_RULE_DAYS_BEFORE = 0
        private const val DEFAULT_RULE_HOUR = 9
        private const val DEFAULT_RULE_MINUTE = 0
    }
}

/**
 * UI intents representing user actions and events within the onboarding MVI flow.
 */
sealed interface OnboardingIntent {
    /**
     * Intent to re-check and refresh the status of all relevant runtime permissions
     * (contacts, notifications, calendar).
     */
    data object RefreshPermissions : OnboardingIntent

    /**
     * Intent to update the persistent status-bar notification setting.
     *
     * @property enabled `true` to enable persistent notifications, `false` to disable.
     */
    data class SetPersistentNotifications(val enabled: Boolean) : OnboardingIntent

    /**
     * Intent to update the active page index in the onboarding pager.
     *
     * @property page The 0-based page index to navigate to.
     */
    data class SetCurrentPage(val page: Int) : OnboardingIntent

    /**
     * Intent to finalize the onboarding process, persisting configuration, creating default
     * reminder rules if necessary, and triggering initial contact synchronization.
     *
     * @property notificationsEnabled Whether birthday notifications should be enabled.
     * @property calendarSyncEnabled Whether calendar synchronization should be enabled.
     */
    data class CompleteOnboarding(
        val notificationsEnabled: Boolean,
        val calendarSyncEnabled: Boolean
    ) : OnboardingIntent
}
