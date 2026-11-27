package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.calendar

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.domain.usecase.SetCalendarSyncEnabledUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.UpdateCalendarColorUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository: SettingsRepository = mock()
    private val contactRepository: ContactRepository = mock()
    private val calendarSyncRepository: CalendarSyncRepository = mock()
    private val setCalendarSyncEnabledUseCase: SetCalendarSyncEnabledUseCase = mock()
    private val updateCalendarColorUseCase: UpdateCalendarColorUseCase = mock()

    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setup() {
        whenever(settingsRepository.settings).thenReturn(flowOf(AppSettings()))
        whenever(contactRepository.allContacts).thenReturn(flowOf(emptyList()))
        whenever(calendarSyncRepository.hasCalendarPermissions()).thenReturn(false)
        viewModel = createViewModel()
    }

    private fun createViewModel() = CalendarViewModel(
        settingsRepository = settingsRepository,
        contactRepository = contactRepository,
        calendarSyncRepository = calendarSyncRepository,
        setCalendarSyncEnabledUseCase = setCalendarSyncEnabledUseCase,
        updateCalendarColorUseCase = updateCalendarColorUseCase,
        defaultDispatcher = mainDispatcherRule.testDispatcher,
    )

    @Test
    fun `uiState should reflect calendar settings and detected custom event calendars`() = runTest {
        val customKey = ContactLabels.customEventLabel("Jahrestag")
        val disabledCustomKey = ContactLabels.customEventLabel("Jubiläum")
        val testSettings = AppSettings(
            calendarSyncEnabled = true,
            otherEventsEnabled = true,
            birthdayCalendarColor = 123,
            anniversaryCalendarColor = 456,
            nameDayCalendarColor = 789,
            disabledOtherEventLabels = listOf(ContactLabels.LABEL_NAME_DAY, disabledCustomKey),
            customEventCalendarColors = mapOf(customKey to 999),
        )
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Alice",
                customEvents = mapOf(
                    customKey to LocalDate.of(2020, 6, 1),
                    disabledCustomKey to LocalDate.of(2021, 7, 1),
                ),
            )
        )
        whenever(settingsRepository.settings).thenReturn(flowOf(testSettings))
        whenever(contactRepository.allContacts).thenReturn(flowOf(contacts))
        whenever(calendarSyncRepository.hasCalendarPermissions()).thenReturn(true)

        val viewModel = createViewModel()
        val uiState = viewModel.uiState.first { it.calendarSyncEnabled }

        assertThat(uiState.calendarSyncEnabled).isTrue()
        assertThat(uiState.otherEventsEnabled).isTrue()
        assertThat(uiState.anniversaryEnabled).isTrue()
        assertThat(uiState.nameDayEnabled).isFalse()
        assertThat(uiState.birthdayCalendarColor).isEqualTo(123)
        assertThat(uiState.anniversaryCalendarColor).isEqualTo(456)
        assertThat(uiState.nameDayCalendarColor).isEqualTo(789)
        assertThat(uiState.customEventCalendars).containsExactly(
            CustomEventCalendarColorItem(eventKey = customKey, color = 999)
        )
        assertThat(uiState.hasCalendarPermission).isTrue()
    }

    @Test
    fun `uiState should reflect calendar permission`() = runTest {
        whenever(calendarSyncRepository.hasCalendarPermissions()).thenReturn(true)
        val viewModel = createViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        runCurrent()
        assertThat(viewModel.uiState.value.hasCalendarPermission).isTrue()
        collectJob.cancel()
    }

    @Test
    fun `checkPermissionStatus should update uiState hasCalendarPermission`() = runTest {
        whenever(calendarSyncRepository.hasCalendarPermissions()).thenReturn(false)
        val viewModel = createViewModel()

        val collectJob = launch { viewModel.uiState.collect {} }
        runCurrent()
        assertThat(viewModel.uiState.value.hasCalendarPermission).isFalse()

        whenever(calendarSyncRepository.hasCalendarPermissions()).thenReturn(true)
        viewModel.onIntent(CalendarIntent.CheckPermissionStatus)
        runCurrent()

        assertThat(viewModel.uiState.value.hasCalendarPermission).isTrue()
        collectJob.cancel()
    }

    @Test
    fun `updateCalendarColor should delegate to usecase`() = runTest {
        val type = CalendarSyncRepository.CalendarType.BIRTHDAY
        viewModel.onIntent(CalendarIntent.UpdateCalendarColor(type, 111))

        verify(updateCalendarColorUseCase).invoke(type, 111)
    }

    @Test
    fun `updateCustomEventCalendarColor should delegate to usecase`() = runTest {
        val eventKey = ContactLabels.customEventLabel("Jahrestag")
        viewModel.onIntent(CalendarIntent.UpdateCustomEventCalendarColor(eventKey, 222))

        verify(updateCalendarColorUseCase).invoke(eventKey, 222)
    }

    @Test
    fun `setCalendarSyncEnabled should delegate to usecase`() = runTest {
        viewModel.onIntent(CalendarIntent.SetCalendarSyncEnabled(true))

        verify(setCalendarSyncEnabledUseCase).invoke(true)
    }
}
