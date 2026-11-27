package com.heckmannch.birthdaybuddy.ui.screens.onboarding

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import com.heckmannch.birthdaybuddy.domain.usecase.SyncCalendarUseCase
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notificationRepository: NotificationRepository = mock()
    private val settingsRepository: SettingsRepository = mock()
    private val contactRepository: ContactRepository = mock()
    private val syncCalendarUseCase: SyncCalendarUseCase = mock()
    private val widgetUpdater: WidgetUpdater = mock()
    private val permissionChecker: PermissionChecker = mock()

    private lateinit var viewModel: OnboardingViewModel

    private fun createViewModel(
        scope: CoroutineScope = CoroutineScope(mainDispatcherRule.testDispatcher),
    ) = OnboardingViewModel(
        notificationRepository = notificationRepository,
        settingsRepository = settingsRepository,
        contactRepository = contactRepository,
        syncCalendarUseCase = syncCalendarUseCase,
        widgetUpdater = widgetUpdater,
        permissionChecker = permissionChecker,
        applicationScope = scope,
        ioDispatcher = mainDispatcherRule.testDispatcher,
    )

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        whenever(settingsRepository.settings).thenReturn(flowOf(AppSettings(onboardingCompleted = false)))
        // Default permission mock values to false
        whenever(permissionChecker.hasContactsPermission()).thenReturn(false)
        whenever(permissionChecker.hasNotificationPermission()).thenReturn(false)
        whenever(permissionChecker.hasCalendarPermission()).thenReturn(false)
        viewModel = createViewModel()
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `uiState should reflect permissions and app settings`() = runTest {
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        whenever(permissionChecker.hasNotificationPermission()).thenReturn(true)
        whenever(permissionChecker.hasCalendarPermission()).thenReturn(false)

        val localViewModel = createViewModel()
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

        viewModel.onIntent(OnboardingIntent.SetCurrentPage(-1))
        runCurrent()

        assertThat(viewModel.uiState.value.currentPage).isEqualTo(0)
        collectJob.cancel()
    }

    @Test
    fun `uiState initial value should reflect permissions immediately without collection`() {
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        whenever(permissionChecker.hasNotificationPermission()).thenReturn(false)
        whenever(permissionChecker.hasCalendarPermission()).thenReturn(true)

        val vm = createViewModel()

        // Verify initial value BEFORE any collection
        assertThat(vm.uiState.value.hasContactPermission).isTrue()
        assertThat(vm.uiState.value.hasNotificationPermission).isFalse()
        assertThat(vm.uiState.value.hasCalendarPermission).isTrue()
    }

    @Test
    fun `SetPersistentNotifications intent should delegate to repository`() = runTest {
        viewModel.onIntent(OnboardingIntent.SetPersistentNotifications(true))

        val captor = argumentCaptor<(AppSettings) -> AppSettings>()
        verify(settingsRepository).updateSettings(captor.capture())
        val updated = captor.firstValue(AppSettings(persistentNotifications = false))
        assertThat(updated.persistentNotifications).isTrue()
    }

    @Test
    fun `completeOnboarding with contacts permission should update settings and delegate calendar and widget sync to contact sync`() =
        runTest {
            whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
            whenever(notificationRepository.getAllRulesImmediate()).thenReturn(emptyList())

            viewModel.onIntent(
                OnboardingIntent.CompleteOnboarding(
                    notificationsEnabled = true,
                    calendarSyncEnabled = true
                )
            )

            val captor = argumentCaptor<(AppSettings) -> AppSettings>()
            verify(settingsRepository).updateSettings(captor.capture())
            val updated = captor.firstValue(
                AppSettings(
                    notificationsEnabled = false,
                    calendarSyncEnabled = false,
                    onboardingCompleted = false
                )
            )
            assertThat(updated.notificationsEnabled).isTrue()
            assertThat(updated.calendarSyncEnabled).isTrue()
            assertThat(updated.onboardingCompleted).isTrue()
            verify(notificationRepository).syncScheduling()

            verify(contactRepository).syncContacts()
            verify(syncCalendarUseCase, never()).invoke()
            verify(widgetUpdater, never()).updateWidget()
        }

    @Test
    fun `completeOnboarding without contacts permission should sync calendar and update widget`() =
        runTest {
            whenever(permissionChecker.hasContactsPermission()).thenReturn(false)

            viewModel.onIntent(
                OnboardingIntent.CompleteOnboarding(
                    notificationsEnabled = false,
                    calendarSyncEnabled = true
                )
            )

            verify(contactRepository, never()).syncContacts()
            inOrder(syncCalendarUseCase, widgetUpdater) {
                verify(syncCalendarUseCase).invoke()
                verify(widgetUpdater).updateWidget()
            }
        }

    @Test
    fun `completeOnboarding without contacts permission should update widget but skip calendar sync when calendarSyncEnabled is false`() =
        runTest {
            viewModel.onIntent(
                OnboardingIntent.CompleteOnboarding(
                    notificationsEnabled = false,
                    calendarSyncEnabled = false
                )
            )

            verify(contactRepository, never()).syncContacts()
            verify(syncCalendarUseCase, never()).invoke()
            verify(widgetUpdater).updateWidget()
        }

    @Test
    fun `completeOnboarding should still update widget when calendar sync fails`() = runTest {
        whenever(syncCalendarUseCase.invoke()).thenThrow(RuntimeException("Calendar sync failed"))

        viewModel.onIntent(
            OnboardingIntent.CompleteOnboarding(
                notificationsEnabled = false,
                calendarSyncEnabled = true
            )
        )

        verify(contactRepository, never()).syncContacts()
        verify(syncCalendarUseCase).invoke()
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun `completeOnboarding should not crash when widget update fails`() = runTest {
        whenever(widgetUpdater.updateWidget()).thenThrow(RuntimeException("Widget error"))

        viewModel.onIntent(
            OnboardingIntent.CompleteOnboarding(
                notificationsEnabled = false,
                calendarSyncEnabled = false
            )
        )

        verify(contactRepository, never()).syncContacts()
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun `completeOnboarding should rethrow CancellationException from calendar sync`() = runTest {
        whenever(syncCalendarUseCase.invoke()).thenThrow(CancellationException("Cancelled"))

        val vm = createViewModel(scope = this)
        vm.onIntent(
            OnboardingIntent.CompleteOnboarding(
                notificationsEnabled = false,
                calendarSyncEnabled = true
            )
        )

        verify(contactRepository, never()).syncContacts()
        verify(syncCalendarUseCase).invoke()
        verify(widgetUpdater, never()).updateWidget()
    }

    @Test
    fun `completeOnboarding should fall back to calendar sync and widget update when contact sync fails`() = runTest {
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        whenever(contactRepository.syncContacts()).thenThrow(RuntimeException("Contact sync failed"))

        viewModel.onIntent(
            OnboardingIntent.CompleteOnboarding(
                notificationsEnabled = false,
                calendarSyncEnabled = true
            )
        )

        verify(contactRepository).syncContacts()
        verify(syncCalendarUseCase).invoke()
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun `completeOnboarding should still update widget when contact sync fails and calendarSyncEnabled is false`() = runTest {
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        whenever(contactRepository.syncContacts()).thenThrow(RuntimeException("Contact sync failed"))

        viewModel.onIntent(
            OnboardingIntent.CompleteOnboarding(
                notificationsEnabled = false,
                calendarSyncEnabled = false
            )
        )

        verify(contactRepository).syncContacts()
        verify(syncCalendarUseCase, never()).invoke()
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun `completeOnboarding should rethrow CancellationException from contact sync`() = runTest {
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        whenever(contactRepository.syncContacts()).thenThrow(CancellationException("Cancelled"))

        val vm = createViewModel(scope = this)
        vm.onIntent(
            OnboardingIntent.CompleteOnboarding(
                notificationsEnabled = false,
                calendarSyncEnabled = true
            )
        )

        verify(contactRepository).syncContacts()
        verify(syncCalendarUseCase, never()).invoke()
        verify(widgetUpdater, never()).updateWidget()
    }

    @Test
    fun `completeOnboarding should insert default rule if notifications enabled and no rules exist`() =
        runTest {
            whenever(notificationRepository.getAllRulesImmediate()).thenReturn(emptyList())

            viewModel.onIntent(
                OnboardingIntent.CompleteOnboarding(
                    notificationsEnabled = true,
                    calendarSyncEnabled = false
                )
            )

            verify(notificationRepository).insertRule(any())
        }

    @Test
    fun `completeOnboarding should NOT insert rule if notifications disabled`() = runTest {
        viewModel.onIntent(
            OnboardingIntent.CompleteOnboarding(
                notificationsEnabled = false,
                calendarSyncEnabled = false
            )
        )

        verify(notificationRepository, never()).insertRule(any())
    }

    @Test
    fun `completeOnboarding should NOT insert rule if rules already exist`() = runTest {
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(mock()))

        viewModel.onIntent(
            OnboardingIntent.CompleteOnboarding(
                notificationsEnabled = true,
                calendarSyncEnabled = false
            )
        )

        verify(notificationRepository, never()).insertRule(any())
    }
}
