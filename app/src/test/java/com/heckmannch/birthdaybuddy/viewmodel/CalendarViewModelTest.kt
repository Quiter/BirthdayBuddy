package com.heckmannch.birthdaybuddy.viewmodel

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.AppSettings
import com.heckmannch.birthdaybuddy.data.repository.CalendarSyncRepository
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
class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notificationRepository: NotificationRepository = mock()
    private val calendarSyncRepository: CalendarSyncRepository = mock()
    private val contactRepository: ContactRepository = mock()

    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setup() {
        whenever(notificationRepository.settings).thenReturn(flowOf(AppSettings()))
        viewModel =
            CalendarViewModel(notificationRepository, calendarSyncRepository, contactRepository)
    }

    @Test
    fun `uiState should reflect calendar settings`() = runTest {
        val testSettings = AppSettings(
            calendarSyncEnabled = true,
            otherEventsEnabled = true,
            birthdayCalendarColor = 123,
            anniversaryCalendarColor = 456,
            nameDayCalendarColor = 789
        )
        whenever(notificationRepository.settings).thenReturn(flowOf(testSettings))

        val viewModel =
            CalendarViewModel(notificationRepository, calendarSyncRepository, contactRepository)
        val uiState = viewModel.uiState.first { it.calendarSyncEnabled }

        assertThat(uiState.calendarSyncEnabled).isTrue()
        assertThat(uiState.otherEventsEnabled).isTrue()
        assertThat(uiState.birthdayCalendarColor).isEqualTo(123)
        assertThat(uiState.anniversaryCalendarColor).isEqualTo(456)
        assertThat(uiState.nameDayCalendarColor).isEqualTo(789)
    }

    @Test
    fun `updateCalendarColor should delegate to repository`() = runTest {
        val type = CalendarSyncRepository.CalendarType.BIRTHDAY
        viewModel.updateCalendarColor(type, 111)

        verify(calendarSyncRepository).updateCalendarColor(type, 111)
    }

    @Test
    fun `setCalendarSyncEnabled(true) should update settings and trigger sync`() = runTest {
        whenever(contactRepository.getAllContactsImmediate()).thenReturn(emptyList())
        whenever(calendarSyncRepository.syncBirthdays(any())).thenReturn(true)

        viewModel.setCalendarSyncEnabled(true)

        verify(notificationRepository).updateSettings(
            notificationsEnabled = eq(null),
            persistentNotifications = eq(null),
            onboardingCompleted = eq(null),
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
            themeAccent = eq(null),
            themeContrast = eq(null)
        )
        verify(calendarSyncRepository).syncBirthdays(any())
    }

    @Test
    fun `setCalendarSyncEnabled(false) should update settings and delete calendar`() = runTest {
        whenever(calendarSyncRepository.deleteCalendar()).thenReturn(true)

        viewModel.setCalendarSyncEnabled(false)

        verify(notificationRepository).updateSettings(
            notificationsEnabled = eq(null),
            persistentNotifications = eq(null),
            onboardingCompleted = eq(null),
            lastSyncTimestamp = eq(null),
            calendarSyncEnabled = eq(false),
            calendarId = eq(null),
            clearCalendarId = eq(false),
            otherEventsEnabled = eq(null),
            birthdayCalendarColor = eq(null),
            anniversaryCalendarColor = eq(null),
            nameDayCalendarColor = eq(null),
            themeMode = eq(null),
            themeAmoled = eq(null),
            themeAccent = eq(null),
            themeContrast = eq(null)
        )
        verify(calendarSyncRepository).deleteCalendar()
    }
}
