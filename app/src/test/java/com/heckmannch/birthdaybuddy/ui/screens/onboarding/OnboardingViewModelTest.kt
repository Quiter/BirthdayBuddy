package com.heckmannch.birthdaybuddy.ui.screens.onboarding

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notificationRepository: NotificationRepository = mock()
    private val contactRepository: ContactRepository = mock()
    private val permissionChecker: PermissionChecker = mock()

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        whenever(notificationRepository.settings).thenReturn(flowOf(AppSettings(onboardingCompleted = false)))
        // Default permission mock values to false
        whenever(permissionChecker.hasContactsPermission()).thenReturn(false)
        whenever(permissionChecker.hasNotificationPermission()).thenReturn(false)
        whenever(permissionChecker.hasCalendarPermission()).thenReturn(false)
        viewModel = OnboardingViewModel(notificationRepository, contactRepository, permissionChecker)
    }

    @Test
    fun `onboardingCompleted should reflect settings from repository`() = runTest {
        val completed = viewModel.onboardingCompleted.first { it == false }
        assertThat(completed).isFalse()
    }

    @Test
    fun `uiState should reflect permissions and app settings`() = runTest {
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        whenever(permissionChecker.hasNotificationPermission()).thenReturn(true)
        whenever(permissionChecker.hasCalendarPermission()).thenReturn(false)

        val localViewModel = OnboardingViewModel(notificationRepository, contactRepository, permissionChecker)
        val collectJob = launch { localViewModel.uiState.collect {} }
        runCurrent()

        val state = localViewModel.uiState.value
        println("TEST STATE: hasContact=${state.hasContactPermission}, hasNotif=${state.hasNotificationPermission}, hasCalendar=${state.hasCalendarPermission}")
        assertThat(state.hasContactPermission).isTrue()
        assertThat(state.hasNotificationPermission).isTrue()
        assertThat(state.hasCalendarPermission).isFalse()
        assertThat(state.currentPage).isEqualTo(0)

        collectJob.cancel()
    }

    @Test
    fun `RefreshPermissions intent should update permissions in uiState`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        runCurrent()

        assertThat(viewModel.uiState.value.hasContactPermission).isFalse()

        // Grant permission and refresh
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        viewModel.onIntent(OnboardingIntent.RefreshPermissions)
        runCurrent()

        assertThat(viewModel.uiState.value.hasContactPermission).isTrue()
        collectJob.cancel()
    }

    @Test
    fun `SetCurrentPage intent should update currentPage in uiState`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        runCurrent()

        assertThat(viewModel.uiState.value.currentPage).isEqualTo(0)

        viewModel.onIntent(OnboardingIntent.SetCurrentPage(3))
        runCurrent()

        assertThat(viewModel.uiState.value.currentPage).isEqualTo(3)
        collectJob.cancel()
    }

    @Test
    fun `SetPersistentNotifications intent should delegate to repository`() = runTest {
        viewModel.onIntent(OnboardingIntent.SetPersistentNotifications(true))

        verify(notificationRepository).updateSettings(
            notificationsEnabled = eq(null),
            persistentNotifications = eq(true),
            onboardingCompleted = eq(null),
            lastSyncTimestamp = eq(null),
            calendarSyncEnabled = eq(null),
            calendarId = eq(null),
            clearCalendarId = eq(false),
            otherEventsEnabled = eq(null),
            birthdayCalendarColor = eq(null),
            anniversaryCalendarColor = eq(null),
            nameDayCalendarColor = eq(null),
            themeMode = eq(null),
            themeAmoled = eq(null),
            themeAccent = eq(null)
        )
    }

    @Test
    fun `setPersistentNotifications should delegate to repository`() = runTest {
        viewModel.setPersistentNotifications(true)

        verify(notificationRepository).updateSettings(
            notificationsEnabled = eq(null),
            persistentNotifications = eq(true),
            onboardingCompleted = eq(null),
            lastSyncTimestamp = eq(null),
            calendarSyncEnabled = eq(null),
            calendarId = eq(null),
            clearCalendarId = eq(false),
            otherEventsEnabled = eq(null),
            birthdayCalendarColor = eq(null),
            anniversaryCalendarColor = eq(null),
            nameDayCalendarColor = eq(null),
            themeMode = eq(null),
            themeAmoled = eq(null),
            themeAccent = eq(null)
        )
    }

    @Test
    fun `completeOnboarding should update settings and trigger sync`() = runTest {
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(emptyList())

        viewModel.completeOnboarding(
            notificationsEnabled = true,
            calendarSyncEnabled = true
        )

        verify(notificationRepository).updateSettings(
            notificationsEnabled = eq(true),
            persistentNotifications = eq(null),
            onboardingCompleted = eq(true),
            lastSyncTimestamp = eq(null),
            calendarSyncEnabled = eq(true),
            calendarId = eq(null),
            clearCalendarId = eq(false),
            otherEventsEnabled = eq(null),
            birthdayCalendarColor = eq(null),
            anniversaryCalendarColor = eq(null),
            nameDayCalendarColor = eq(null),
            themeMode = eq(null),
            themeAmoled = eq(null),
            themeAccent = eq(null)
        )
        verify(contactRepository).syncContacts()
    }

    @Test
    fun `completeOnboarding should insert default rule if notifications enabled and no rules exist`() =
        runTest {
            whenever(notificationRepository.getAllRulesImmediate()).thenReturn(emptyList())

            viewModel.completeOnboarding(
                notificationsEnabled = true,
                calendarSyncEnabled = false
            )

            verify(notificationRepository).insertRule(any())
        }

    @Test
    fun `completeOnboarding should NOT insert rule if notifications disabled`() = runTest {
        viewModel.completeOnboarding(
            notificationsEnabled = false,
            calendarSyncEnabled = false
        )

        verify(notificationRepository, never()).insertRule(any())
    }

    @Test
    fun `completeOnboarding should NOT insert rule if rules already exist`() = runTest {
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(mock()))

        viewModel.completeOnboarding(
            notificationsEnabled = true,
            calendarSyncEnabled = false
        )

        verify(notificationRepository, never()).insertRule(any())
    }
}
