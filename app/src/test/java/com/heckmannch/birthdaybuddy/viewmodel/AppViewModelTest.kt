package com.heckmannch.birthdaybuddy.viewmodel

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.AppSettings
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
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
            themeMode = "DARK",
            themeAmoled = true,
            themeAccent = "#FF0000",
            themeContrast = 0.5
        )
        whenever(notificationRepository.settings).thenReturn(flowOf(customSettings))

        // Re-initialize to collect from the new flow
        val freshViewModel = AppViewModel(notificationRepository)
        val result = freshViewModel.appSettings.first { it.themeMode == "DARK" }

        assertThat(result.themeMode).isEqualTo("DARK")
        assertThat(result.themeAmoled).isTrue()
        assertThat(result.themeAccent).isEqualTo("#FF0000")
        assertThat(result.themeContrast).isEqualTo(0.5)
    }

    @Test
    fun `syncScheduling is called once on init`() = runTest {
        // syncScheduling() is triggered in init {} of AppViewModel.
        // The viewModel is already created in @Before – verify the call occurred.
        verify(notificationRepository).syncScheduling()
    }
}
