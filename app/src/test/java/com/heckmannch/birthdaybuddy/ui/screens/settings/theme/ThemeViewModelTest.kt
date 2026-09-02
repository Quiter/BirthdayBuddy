package com.heckmannch.birthdaybuddy.ui.screens.settings.theme

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.ThemeAccent
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
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
            themeMode = ThemeMode.DARK,
            themeAmoled = true,
            themeAccent = ThemeAccent.CUSTOM,
            customAccentColor = "#FF0000"
        )
        whenever(notificationRepository.settings).thenReturn(flowOf(testSettings))

        // Re-initialize to collect from new flow
        val viewModel = ThemeViewModel(notificationRepository)
        val uiState = viewModel.uiState.first { it.themeMode == ThemeMode.DARK }

        assertThat(uiState.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(uiState.themeAmoled).isTrue()
        assertThat(uiState.themeAccent).isEqualTo(ThemeAccent.CUSTOM)
        assertThat(uiState.customAccentColor).isEqualTo("#FF0000")
    }

    @Test
    fun `setThemeMode should delegate to repository`() = runTest {
        viewModel.onIntent(ThemeIntent.SetThemeMode(ThemeMode.LIGHT))

        val captor = argumentCaptor<(AppSettings) -> AppSettings>()
        verify(notificationRepository).updateSettings(captor.capture())
        val updated = captor.firstValue(AppSettings(themeMode = ThemeMode.DARK))
        assertThat(updated.themeMode).isEqualTo(ThemeMode.LIGHT)
    }

    @Test
    fun `setThemeAmoled should delegate to repository`() = runTest {
        viewModel.onIntent(ThemeIntent.SetThemeAmoled(true))

        val captor = argumentCaptor<(AppSettings) -> AppSettings>()
        verify(notificationRepository).updateSettings(captor.capture())
        val updated = captor.firstValue(AppSettings(themeAmoled = false))
        assertThat(updated.themeAmoled).isTrue()
    }

    @Test
    fun `setThemeAccent should delegate to repository`() = runTest {
        viewModel.onIntent(ThemeIntent.SetThemeAccent(ThemeAccent.CUSTOM, "#00FF00"))

        val captor = argumentCaptor<(AppSettings) -> AppSettings>()
        verify(notificationRepository).updateSettings(captor.capture())
        val updated = captor.firstValue(AppSettings(themeAccent = ThemeAccent.SYSTEM, customAccentColor = null))
        assertThat(updated.themeAccent).isEqualTo(ThemeAccent.CUSTOM)
        assertThat(updated.customAccentColor).isEqualTo("#00FF00")
    }
}
