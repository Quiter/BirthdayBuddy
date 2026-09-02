package com.heckmannch.birthdaybuddy.ui.screens.settings.backup

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.usecase.ExportGiftIdeasUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.ImportGiftIdeasUseCase
import com.heckmannch.birthdaybuddy.ui.model.BackupMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val exportGiftIdeasUseCase: ExportGiftIdeasUseCase = mock()
    private val importGiftIdeasUseCase: ImportGiftIdeasUseCase = mock()
    private val uri: Uri = mock()

    private val uriString = "content://test_uri"

    private lateinit var viewModel: BackupViewModel

    @Before
    fun setup() {
        whenever(uri.toString()).thenReturn(uriString)
        viewModel = BackupViewModel(exportGiftIdeasUseCase, importGiftIdeasUseCase, testDispatcher)
    }

    @Test
    fun `initial uiState should be default values`() {
        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.message).isNull()
    }

    @Test
    fun `ExportBackup intent should delegate call to usecase and update state to ExportSuccess`() =
        runTest(testDispatcher) {
            // When
            viewModel.onIntent(BackupIntent.ExportBackup(uri))

            // Then
            verify(exportGiftIdeasUseCase).invoke(uriString)
            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.message).isEqualTo(BackupMessage.ExportSuccess)
        }

    @Test
    fun `ExportBackup intent should update state to ExportError when usecase throws exception`() =
        runTest(testDispatcher) {
            // Given
            val exception = RuntimeException("Export failed")
            whenever(exportGiftIdeasUseCase(uriString)).thenThrow(exception)

            // When
            viewModel.onIntent(BackupIntent.ExportBackup(uri))

            // Then
            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.message).isEqualTo(BackupMessage.ExportError("Export failed"))
        }

    @Test
    fun `ImportBackup intent should delegate to usecase and update state to ImportSuccess`() =
        runTest(testDispatcher) {
            // Given
            whenever(importGiftIdeasUseCase(uriString)).thenReturn(5)

            // When
            viewModel.onIntent(BackupIntent.ImportBackup(uri))

            // Then
            verify(importGiftIdeasUseCase).invoke(uriString)
            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.message).isEqualTo(BackupMessage.ImportSuccess(5))
        }

    @Test
    fun `ImportBackup intent should update state to ImportInvalid when usecase returns negative count`() =
        runTest(testDispatcher) {
            // Given
            whenever(importGiftIdeasUseCase(uriString)).thenReturn(-1)

            // When
            viewModel.onIntent(BackupIntent.ImportBackup(uri))

            // Then
            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.message).isEqualTo(BackupMessage.ImportInvalid)
        }

    @Test
    fun `ImportBackup intent should update state to ImportError when usecase throws exception`() =
        runTest(testDispatcher) {
            // Given
            val exception = RuntimeException("Import failed")
            whenever(importGiftIdeasUseCase(uriString)).thenThrow(exception)

            // When
            viewModel.onIntent(BackupIntent.ImportBackup(uri))

            // Then
            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.message).isEqualTo(BackupMessage.ImportError("Import failed"))
        }

    @Test
    fun `ClearMessage intent should clear the message in uiState`() =
        runTest(testDispatcher) {
            // Given
            viewModel.onIntent(BackupIntent.ExportBackup(uri))
            assertThat(viewModel.uiState.value.message).isEqualTo(BackupMessage.ExportSuccess)

            // When
            viewModel.onIntent(BackupIntent.ClearMessage)

            // Then
            assertThat(viewModel.uiState.value.message).isNull()
        }
}
