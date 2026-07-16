package com.heckmannch.birthdaybuddy

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode
import com.heckmannch.birthdaybuddy.domain.model.ThemeAccent
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
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

    private lateinit var viewModel: AppViewModel

    @Before
    fun setup() {
        whenever(notificationRepository.settings).thenReturn(flowOf(AppSettings()))
        viewModel = AppViewModel(notificationRepository)
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
        val freshViewModel = AppViewModel(notificationRepository)
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
}
