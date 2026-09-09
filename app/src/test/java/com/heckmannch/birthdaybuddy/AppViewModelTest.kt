package com.heckmannch.birthdaybuddy

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.ThemeAccent
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.domain.usecase.ScheduleDailyWidgetUpdateUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.SyncNotificationSchedulingUseCase
import com.heckmannch.birthdaybuddy.ui.navigation.AppAction
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val syncNotificationSchedulingUseCase: SyncNotificationSchedulingUseCase = mock()
    private val scheduleDailyWidgetUpdateUseCase: ScheduleDailyWidgetUpdateUseCase = mock()
    private val settingsRepository: SettingsRepository = mock()

    private lateinit var viewModel: AppViewModel

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        whenever(settingsRepository.settings).thenReturn(flowOf(AppSettings()))
        viewModel = AppViewModel(syncNotificationSchedulingUseCase, scheduleDailyWidgetUpdateUseCase, settingsRepository)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `appSettings emits default AppSettings as initial value`() = runTest {
        val result = viewModel.appSettings.first()
        assertThat(result).isEqualTo(AppSettings())
    }

    @Test
    fun `appSettings emits values from repository settings flow`() = runTest {
        val customSettings = AppSettings(
            themeMode = ThemeMode.DARK,
            themeAmoled = true,
            themeAccent = ThemeAccent.CUSTOM,
            customAccentColor = "#FF0000"
        )
        whenever(settingsRepository.settings).thenReturn(flowOf(customSettings))

        // Re-initialize to collect from the new flow
        val freshViewModel = AppViewModel(syncNotificationSchedulingUseCase, scheduleDailyWidgetUpdateUseCase, settingsRepository)
        val result = freshViewModel.appSettings.first { it.themeMode == ThemeMode.DARK }

        assertThat(result.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(result.themeAmoled).isTrue()
        assertThat(result.themeAccent).isEqualTo(ThemeAccent.CUSTOM)
        assertThat(result.customAccentColor).isEqualTo("#FF0000")
    }

    @Test
    fun `syncScheduling is called once on init`() = runTest {
        // syncNotificationSchedulingUseCase() is triggered in init {} of AppViewModel.
        // The viewModel is already created in @Before – verify the call occurred.
        verify(syncNotificationSchedulingUseCase).invoke()
    }

    @Test
    fun `scheduleDailyUpdate is called once on init`() = runTest {
        // scheduleDailyWidgetUpdateUseCase() is triggered in init {} of AppViewModel.
        // The viewModel is already created in @Before – verify the call occurred.
        verify(scheduleDailyWidgetUpdateUseCase).invoke()
    }

    @Test
    fun `onboardingCompleted initial value is null until repository settings emits`() = runTest {
        val pendingSettingsFlow = kotlinx.coroutines.flow.MutableSharedFlow<AppSettings>()
        whenever(settingsRepository.settings).thenReturn(pendingSettingsFlow)

        val freshViewModel = AppViewModel(syncNotificationSchedulingUseCase, scheduleDailyWidgetUpdateUseCase, settingsRepository)
        assertThat(freshViewModel.onboardingCompleted.value).isNull()
    }

    @Test
    fun `onboardingCompleted emits value from repository settings`() = runTest {
        val completedSettings = AppSettings(onboardingCompleted = true)
        whenever(settingsRepository.settings).thenReturn(flowOf(completedSettings))

        val freshViewModel = AppViewModel(syncNotificationSchedulingUseCase, scheduleDailyWidgetUpdateUseCase, settingsRepository)
        val result = freshViewModel.onboardingCompleted.first { it != null }

        assertThat(result).isTrue()
    }

    @Test
    fun `onboardingCompleted emits false when repository settings has onboardingCompleted false`() = runTest {
        val notCompletedSettings = AppSettings(onboardingCompleted = false)
        whenever(settingsRepository.settings).thenReturn(flowOf(notCompletedSettings))

        val freshViewModel = AppViewModel(syncNotificationSchedulingUseCase, scheduleDailyWidgetUpdateUseCase, settingsRepository)
        val result = freshViewModel.onboardingCompleted.first { it != null }

        assertThat(result).isFalse()
    }

    @Test
    fun `init completes safely and handles widget scheduling exception without crashing`() = runTest {
        val failingScheduleDailyWidgetUpdateUseCase: ScheduleDailyWidgetUpdateUseCase = mock()
        whenever(failingScheduleDailyWidgetUpdateUseCase.invoke()).thenThrow(RuntimeException("Scheduling failed"))

        val vm = AppViewModel(syncNotificationSchedulingUseCase, failingScheduleDailyWidgetUpdateUseCase, settingsRepository)
        assertThat(vm.appSettings.value).isEqualTo(AppSettings())
    }

    @Test
    fun `pendingAction initial value is null`() = runTest {
        assertThat(viewModel.pendingAction.value).isNull()
    }

    @Test
    fun `handleAction updates pendingAction StateFlow`() = runTest {
        val action = AppAction.ScrollToTop
        viewModel.handleAction(action)

        assertThat(viewModel.pendingAction.value).isEqualTo(action)
    }

    @Test
    fun `handleAction with null ignores value and does not overwrite existing action`() = runTest {
        val action = AppAction.OpenSearch
        viewModel.handleAction(action)
        viewModel.handleAction(null)

        assertThat(viewModel.pendingAction.value).isEqualTo(action)
    }

    @Test
    fun `consumeAction resets pendingAction to null`() = runTest {
        val action = AppAction.OpenBirthdayPicker("lookup_1", 1990, 5, 20)
        viewModel.handleAction(action)
        assertThat(viewModel.pendingAction.value).isEqualTo(action)

        viewModel.consumeAction()
        assertThat(viewModel.pendingAction.value).isNull()
    }
}
