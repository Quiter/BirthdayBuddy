package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.otherevents

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.domain.usecase.GetDetectedOtherEventsUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.SyncCalendarUseCase
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class OtherEventsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository: SettingsRepository = mock()
    private val contactRepository: ContactRepository = mock()
    private val syncCalendarUseCase: SyncCalendarUseCase = mock()
    private val permissionChecker: PermissionChecker = mock()
    private lateinit var getDetectedOtherEventsUseCase: GetDetectedOtherEventsUseCase

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        getDetectedOtherEventsUseCase = GetDetectedOtherEventsUseCase(mainDispatcherRule.testDispatcher)
        whenever(settingsRepository.settings).thenReturn(flowOf(AppSettings()))
        whenever(contactRepository.allContacts).thenReturn(flowOf(emptyList()))
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun createViewModel() = OtherEventsViewModel(
        settingsRepository = settingsRepository,
        contactRepository = contactRepository,
        syncCalendarUseCase = syncCalendarUseCase,
        getDetectedOtherEventsUseCase = getDetectedOtherEventsUseCase,
        permissionChecker = permissionChecker,
        defaultDispatcher = mainDispatcherRule.testDispatcher,
    )

    @Test
    fun `uiState reflects otherEventsEnabled and detected events from contacts`() = runTest {
        val customKey = ContactLabels.customEventLabel("Jahrestag")
        val settings = AppSettings(
            otherEventsEnabled = true,
            disabledOtherEventLabels = listOf(customKey),
        )
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Alice",
                anniversary = LocalDate.of(2020, 6, 1),
                customEvents = mapOf(customKey to LocalDate.of(2021, 7, 1)),
            )
        )
        whenever(settingsRepository.settings).thenReturn(flowOf(settings))
        whenever(contactRepository.allContacts).thenReturn(flowOf(contacts))

        val viewModel = createViewModel()
        val state = viewModel.uiState.first { it.otherEventsEnabled && it.detectedEvents.isNotEmpty() }

        assertThat(state.otherEventsEnabled).isTrue()
        assertThat(state.detectedEvents).containsExactly(
            DetectedOtherEventUiModel(
                eventKey = ContactLabels.LABEL_ANNIVERSARY,
                contactCount = 1,
                isEnabled = true,
            ),
            DetectedOtherEventUiModel(
                eventKey = customKey,
                contactCount = 1,
                isEnabled = false,
            ),
        ).inOrder()
    }

    @Test
    fun `SetOtherEventsEnabled true with contacts permission updates settings, syncs contacts, and skips duplicate calendar sync`() = runTest {
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        val viewModel = createViewModel()

        viewModel.onIntent(OtherEventsIntent.SetOtherEventsEnabled(true))

        val captor = argumentCaptor<(AppSettings) -> AppSettings>()
        verify(settingsRepository).updateSettings(captor.capture())
        val updated = captor.firstValue(AppSettings(otherEventsEnabled = false))
        assertThat(updated.otherEventsEnabled).isTrue()
        verify(contactRepository).syncContacts()
        verify(syncCalendarUseCase, never()).invoke()
    }

    @Test
    fun `SetOtherEventsEnabled true without contacts permission updates settings and falls back to calendar sync`() = runTest {
        whenever(permissionChecker.hasContactsPermission()).thenReturn(false)
        val viewModel = createViewModel()

        viewModel.onIntent(OtherEventsIntent.SetOtherEventsEnabled(true))

        val captor = argumentCaptor<(AppSettings) -> AppSettings>()
        verify(settingsRepository).updateSettings(captor.capture())
        val updated = captor.firstValue(AppSettings(otherEventsEnabled = false))
        assertThat(updated.otherEventsEnabled).isTrue()
        verify(contactRepository, never()).syncContacts()
        verify(syncCalendarUseCase).invoke()
    }

    @Test
    fun `SetOtherEventsEnabled false updates settings and syncs calendar without syncing contacts`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(OtherEventsIntent.SetOtherEventsEnabled(false))

        val captor = argumentCaptor<(AppSettings) -> AppSettings>()
        verify(settingsRepository).updateSettings(captor.capture())
        val updated = captor.firstValue(AppSettings(otherEventsEnabled = true))
        assertThat(updated.otherEventsEnabled).isFalse()
        verify(contactRepository, never()).syncContacts()
        verify(syncCalendarUseCase).invoke()
    }

    @Test
    fun `SetEventLabelEnabled toggles key in disabledOtherEventLabels and syncs calendar`() = runTest {
        val customKey = ContactLabels.customEventLabel("Jahrestag")
        val viewModel = createViewModel()

        // Disable
        viewModel.onIntent(OtherEventsIntent.SetEventLabelEnabled(customKey, enabled = false))

        val captor = argumentCaptor<(AppSettings) -> AppSettings>()
        verify(settingsRepository).updateSettings(captor.capture())
        val disabledState = captor.firstValue(AppSettings(disabledOtherEventLabels = emptyList()))
        assertThat(disabledState.disabledOtherEventLabels).containsExactly(customKey)
        verify(syncCalendarUseCase).invoke()

        // Re-enable
        viewModel.onIntent(OtherEventsIntent.SetEventLabelEnabled(customKey, enabled = true))
        val captor2 = argumentCaptor<(AppSettings) -> AppSettings>()
        verify(settingsRepository, org.mockito.kotlin.times(2)).updateSettings(captor2.capture())
        val reenabledState = captor2.secondValue(AppSettings(disabledOtherEventLabels = listOf(customKey)))
        assertThat(reenabledState.disabledOtherEventLabels).isEmpty()
    }

    @Test
    fun `SetOtherEventsEnabled true logs error and still syncs calendar when contact sync fails`() = runTest {
        whenever(contactRepository.syncContacts()).thenThrow(RuntimeException("Sync failed"))
        val viewModel = createViewModel()

        viewModel.onIntent(OtherEventsIntent.SetOtherEventsEnabled(true))

        verify(contactRepository).syncContacts()
        verify(syncCalendarUseCase).invoke()
        io.mockk.verify(exactly = 1) { Log.e("OtherEventsViewModel", any(), any<RuntimeException>()) }
    }

    @Test
    fun `SetOtherEventsEnabled true rethrows CancellationException from contact sync without syncing calendar`() = runTest {
        whenever(contactRepository.syncContacts()).thenThrow(CancellationException("Cancelled"))
        val viewModel = createViewModel()

        viewModel.onIntent(OtherEventsIntent.SetOtherEventsEnabled(true))

        verify(contactRepository).syncContacts()
        verify(syncCalendarUseCase, never()).invoke()
        io.mockk.verify(exactly = 0) { Log.e(any(), any(), any()) }
    }
}
