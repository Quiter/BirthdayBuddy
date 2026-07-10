package com.heckmannch.birthdaybuddy.viewmodel

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.ui.screens.settings.theme.ThemeIntent
import com.heckmannch.birthdaybuddy.ui.screens.settings.theme.ThemeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notificationRepository: NotificationRepository = mock()

    private lateinit var viewModel: ThemeViewModel

    @Before
    fun setup() {
        whenever(notificationRepository.settings).thenReturn(flowOf(AppSettings()))
        viewModel = ThemeViewModel(notificationRepository)
    }

    @Test
    fun `uiState should reflect theme settings`() = runTest {
        val testSettings = AppSettings(
            themeMode = "DARK",
            themeAmoled = true,
            themeAccent = "#FF0000"
        )
        whenever(notificationRepository.settings).thenReturn(flowOf(testSettings))

        // Re-initialize to collect from new flow
        val viewModel = ThemeViewModel(notificationRepository)
        val uiState = viewModel.uiState.first { it.themeMode == "DARK" }

        assertThat(uiState.themeMode).isEqualTo("DARK")
        assertThat(uiState.themeAmoled).isTrue()
        assertThat(uiState.themeAccent).isEqualTo("#FF0000")
    }

    @Test
    fun `setThemeMode should delegate to repository`() = runTest {
        viewModel.onIntent(ThemeIntent.SetThemeMode("LIGHT"))

        verify(notificationRepository).updateSettings(
            notificationsEnabled = eq(null),
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
            themeMode = eq("LIGHT"),
            themeAmoled = eq(null),
            themeAccent = eq(null)
        )
    }

    @Test
    fun `setThemeAmoled should delegate to repository`() = runTest {
        viewModel.onIntent(ThemeIntent.SetThemeAmoled(true))

        verify(notificationRepository).updateSettings(
            notificationsEnabled = eq(null),
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
            themeAmoled = eq(true),
            themeAccent = eq(null)
        )
    }

    @Test
    fun `setThemeAccent should delegate to repository`() = runTest {
        viewModel.onIntent(ThemeIntent.SetThemeAccent("#00FF00"))

        verify(notificationRepository).updateSettings(
            notificationsEnabled = eq(null),
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
            themeAccent = eq("#00FF00")
        )
    }
}
