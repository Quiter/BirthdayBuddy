package com.heckmannch.birthdaybuddy.ui.screens.settings.calendar

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.AppSettings
import com.heckmannch.birthdaybuddy.data.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.usecase.SetCalendarSyncEnabledUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.UpdateCalendarColorUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notificationRepository: NotificationRepository = mock()
    private val calendarSyncRepository: CalendarSyncRepository = mock()
    private val setCalendarSyncEnabledUseCase: SetCalendarSyncEnabledUseCase = mock()
    private val updateCalendarColorUseCase: UpdateCalendarColorUseCase = mock()

    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setup() {
        whenever(notificationRepository.settings).thenReturn(flowOf(AppSettings()))
        viewModel = CalendarViewModel(
            notificationRepository,
            calendarSyncRepository,
            setCalendarSyncEnabledUseCase,
            updateCalendarColorUseCase
        )
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

        val viewModel = CalendarViewModel(
            notificationRepository,
            calendarSyncRepository,
            setCalendarSyncEnabledUseCase,
            updateCalendarColorUseCase
        )
        val uiState = viewModel.uiState.first { it.calendarSyncEnabled }

        assertThat(uiState.calendarSyncEnabled).isTrue()
        assertThat(uiState.otherEventsEnabled).isTrue()
        assertThat(uiState.birthdayCalendarColor).isEqualTo(123)
        assertThat(uiState.anniversaryCalendarColor).isEqualTo(456)
        assertThat(uiState.nameDayCalendarColor).isEqualTo(789)
    }

    @Test
    fun `updateCalendarColor should delegate to usecase`() = runTest {
        val type = CalendarSyncRepository.CalendarType.BIRTHDAY
        viewModel.updateCalendarColor(type, 111)

        verify(updateCalendarColorUseCase).invoke(type, 111)
    }

    @Test
    fun `setCalendarSyncEnabled should delegate to usecase`() = runTest {
        viewModel.setCalendarSyncEnabled(true)

        verify(setCalendarSyncEnabledUseCase).invoke(true)
    }
}
