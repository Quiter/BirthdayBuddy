package com.heckmannch.birthdaybuddy

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.ThemeAccent
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import com.heckmannch.birthdaybuddy.ui.navigation.AppAction
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
class AppViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notificationRepository: NotificationRepository = mock()
    private val widgetUpdater: WidgetUpdater = mock()

    private lateinit var viewModel: AppViewModel

    @Before
    fun setup() {
        whenever(notificationRepository.settings).thenReturn(flowOf(AppSettings()))
        viewModel = AppViewModel(notificationRepository, widgetUpdater)
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
        whenever(notificationRepository.settings).thenReturn(flowOf(customSettings))

        // Re-initialize to collect from the new flow
        val freshViewModel = AppViewModel(notificationRepository, widgetUpdater)
        val result = freshViewModel.appSettings.first { it.themeMode == ThemeMode.DARK }

        assertThat(result.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(result.themeAmoled).isTrue()
        assertThat(result.themeAccent).isEqualTo(ThemeAccent.CUSTOM)
        assertThat(result.customAccentColor).isEqualTo("#FF0000")
    }

    @Test
    fun `syncScheduling is called once on init`() = runTest {
        // syncScheduling() is triggered in init {} of AppViewModel.
        // The viewModel is already created in @Before – verify the call occurred.
        verify(notificationRepository).syncScheduling()
    }

    @Test
    fun `scheduleDailyUpdate is called once on init`() = runTest {
        // scheduleDailyUpdate() is triggered in init {} of AppViewModel.
        // The viewModel is already created in @Before – verify the call occurred.
        verify(widgetUpdater).scheduleDailyUpdate()
    }

    @Test
    fun `onboardingCompleted emits value from repository settings`() = runTest {
        val completedSettings = AppSettings(onboardingCompleted = true)
        whenever(notificationRepository.settings).thenReturn(flowOf(completedSettings))

        val freshViewModel = AppViewModel(notificationRepository, widgetUpdater)
        val result = freshViewModel.onboardingCompleted.first { it != null }

        assertThat(result).isTrue()
    }

    @Test
    fun `init completes safely and handles widget scheduling exception without crashing`() = runTest {
        val failingWidgetUpdater: WidgetUpdater = mock()
        whenever(failingWidgetUpdater.scheduleDailyUpdate()).thenThrow(RuntimeException("Scheduling failed"))

        val vm = AppViewModel(notificationRepository, failingWidgetUpdater)
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
