package com.heckmannch.birthdaybuddy.ui.screens.settings.notifications

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notificationRepository: NotificationRepository = mock()
    private val contactRepository: ContactRepository = mock()
    private val permissionChecker: PermissionChecker = mock()

    private lateinit var viewModel: NotificationViewModel

    private val testSettings = AppSettings(
        notificationsEnabled = true,
        persistentNotifications = false,
        otherEventsEnabled = false
    )

    private val testRules = listOf(
        NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0),
        NotificationRule(id = 2, daysBefore = 1, hour = 18, minute = 0)
    )

    @Before
    fun setup() = runTest {
        whenever(notificationRepository.settings).thenReturn(flowOf(testSettings))
        whenever(notificationRepository.allRules).thenReturn(flowOf(testRules))
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(testRules)
        whenever(permissionChecker.hasNotificationPermission()).thenReturn(true)

        viewModel = NotificationViewModel(notificationRepository, contactRepository, permissionChecker)
    }

    @Test
    fun `uiState should reflect data from repositories`() = runTest {
        val uiState = viewModel.uiState.first { it.notificationRules.isNotEmpty() }

        assertThat(uiState.notificationsEnabled).isTrue()
        assertThat(uiState.persistentNotifications).isFalse()
        assertThat(uiState.otherEventsEnabled).isFalse()
        assertThat(uiState.notificationRules).isEqualTo(testRules)
        assertThat(uiState.hasSystemNotificationPermission).isTrue()
    }

    @Test
    fun `refreshPermissionStatus should update uiState hasSystemNotificationPermission`() = runTest {
        whenever(permissionChecker.hasNotificationPermission()).thenReturn(false)
        val viewModel = NotificationViewModel(
            notificationRepository,
            contactRepository,
            permissionChecker
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        runCurrent()
        assertThat(viewModel.uiState.value.hasSystemNotificationPermission).isFalse()

        whenever(permissionChecker.hasNotificationPermission()).thenReturn(true)
        viewModel.onIntent(NotificationIntent.RefreshPermissionStatus)
        runCurrent()

        assertThat(viewModel.uiState.value.hasSystemNotificationPermission).isTrue()
        collectJob.cancel()
    }

    @Test
    fun `setNotificationsEnabled should delegate to repository`() = runTest {
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(testRules)

        viewModel.onIntent(NotificationIntent.SetEnabled(false))

        val captor = argumentCaptor<(AppSettings) -> AppSettings>()
        verify(notificationRepository).updateSettings(captor.capture())
        val updated = captor.firstValue(AppSettings(notificationsEnabled = true))
        assertThat(updated.notificationsEnabled).isFalse()
    }

    @Test
    fun `setPersistentNotifications should delegate to repository`() = runTest {
        viewModel.onIntent(NotificationIntent.SetPersistent(true))

        val captor = argumentCaptor<(AppSettings) -> AppSettings>()
        verify(notificationRepository).updateSettings(captor.capture())
        val updated = captor.firstValue(AppSettings(persistentNotifications = false))
        assertThat(updated.persistentNotifications).isTrue()
    }

    @Test
    fun `setOtherEventsEnabled should delegate to repository and sync contacts`() = runTest {
        viewModel.onIntent(NotificationIntent.SetOtherEventsEnabled(true))

        val captor = argumentCaptor<(AppSettings) -> AppSettings>()
        verify(notificationRepository).updateSettings(captor.capture())
        val updated = captor.firstValue(AppSettings(otherEventsEnabled = false))
        assertThat(updated.otherEventsEnabled).isTrue()
        verify(contactRepository).syncContacts()
    }

    @Test
    fun `addNotificationRule should delegate to repository`() = runTest {
        viewModel.onIntent(NotificationIntent.AddRule(daysBefore = 2, hour = 10, minute = 30))

        verify(notificationRepository).insertRule(any())
    }

    @Test
    fun `addNotificationRule with duplicate daysBefore should not insert rule into repository`() = runTest {
        // daysBefore = 0 already exists in testRules
        viewModel.onIntent(NotificationIntent.AddRule(daysBefore = 0, hour = 18, minute = 0))

        verify(notificationRepository, never()).insertRule(any())
    }

    @Test
    fun `updateNotificationRule should delegate to repository`() = runTest {
        val rule = testRules[0]
        viewModel.onIntent(NotificationIntent.UpdateRule(rule))
        verify(notificationRepository).updateRule(rule)
    }

    @Test
    fun `updateNotificationRule with duplicate daysBefore from another rule should not update rule in repository`() = runTest {
        // testRules[0] has daysBefore = 0, testRules[1] has daysBefore = 1.
        // Trying to update testRules[1] (id=2) to daysBefore = 0 conflicts with testRules[0] (id=1).
        val conflictingRule = testRules[1].copy(daysBefore = 0)
        viewModel.onIntent(NotificationIntent.UpdateRule(conflictingRule))

        verify(notificationRepository, never()).updateRule(any())
    }

    @Test
    fun `deleteNotificationRule should delegate to repository`() = runTest {
        val rule = testRules[0]
        viewModel.onIntent(NotificationIntent.DeleteRule(rule))
        verify(notificationRepository).deleteRule(rule)
    }
}
