package com.heckmannch.birthdaybuddy.viewmodel

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.AppSettings
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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notificationRepository: NotificationRepository = mock()
    private val contactRepository: ContactRepository = mock()

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        whenever(notificationRepository.settings).thenReturn(flowOf(AppSettings(onboardingCompleted = false)))
        viewModel = OnboardingViewModel(notificationRepository, contactRepository)
    }

    @Test
    fun `onboardingCompleted should reflect settings from repository`() = runTest {
        val completed = viewModel.onboardingCompleted.first { it == false }
        assertThat(completed).isFalse()
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
    fun `completeOnboarding should insert default rule if notifications enabled and no rules exist`() = runTest {
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
