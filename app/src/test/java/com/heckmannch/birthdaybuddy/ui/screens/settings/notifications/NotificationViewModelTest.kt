package com.heckmannch.birthdaybuddy.ui.screens.settings.notifications

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.AppSettings
import com.heckmannch.birthdaybuddy.data.local.NotificationRule
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notificationRepository: NotificationRepository = mock()
    private val contactRepository: ContactRepository = mock()

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
    fun setup() {
        whenever(notificationRepository.settings).thenReturn(flowOf(testSettings))
        whenever(notificationRepository.allRules).thenReturn(flowOf(testRules))

        viewModel = NotificationViewModel(notificationRepository, contactRepository)
    }

    @Test
    fun `uiState should reflect data from repositories`() = runTest {
        val uiState = viewModel.uiState.first { it.notificationRules.isNotEmpty() }

        assertThat(uiState.notificationsEnabled).isTrue()
        assertThat(uiState.persistentNotifications).isFalse()
        assertThat(uiState.otherEventsEnabled).isFalse()
        assertThat(uiState.notificationRules).isEqualTo(testRules)
    }

    @Test
    fun `setNotificationsEnabled should delegate to repository`() = runTest {
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(testRules)

        viewModel.setNotificationsEnabled(false)

        verify(notificationRepository).updateSettings(
            notificationsEnabled = eq(false),
            persistentNotifications = eq(null),
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
    fun `setOtherEventsEnabled should delegate to repository and sync contacts`() = runTest {
        viewModel.setOtherEventsEnabled(true)

        verify(notificationRepository).updateSettings(
            notificationsEnabled = eq(null),
            persistentNotifications = eq(null),
            onboardingCompleted = eq(null),
            lastSyncTimestamp = eq(null),
            calendarSyncEnabled = eq(null),
            calendarId = eq(null),
            clearCalendarId = eq(false),
            otherEventsEnabled = eq(true),
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
    fun `addNotificationRule should delegate to repository`() = runTest {
        viewModel.addNotificationRule(daysBefore = 2, hour = 10, minute = 30)

        verify(notificationRepository).insertRule(any())
    }

    @Test
    fun `updateNotificationRule should delegate to repository`() = runTest {
        val rule = testRules[0]
        viewModel.updateNotificationRule(rule)
        verify(notificationRepository).updateRule(rule)
    }

    @Test
    fun `deleteNotificationRule should delegate to repository`() = runTest {
        val rule = testRules[0]
        viewModel.deleteNotificationRule(rule)
        verify(notificationRepository).deleteRule(rule)
    }
}
