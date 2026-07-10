package com.heckmannch.birthdaybuddy.viewmodel

import android.net.Uri
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.usecase.ExportGiftIdeasUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.ImportGiftIdeasUseCase
import com.heckmannch.birthdaybuddy.ui.screens.settings.backup.BackupIntent
import com.heckmannch.birthdaybuddy.ui.screens.settings.backup.BackupViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
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

    private lateinit var viewModel: BackupViewModel

    @Before
    fun setup() {
        viewModel = BackupViewModel(exportGiftIdeasUseCase, importGiftIdeasUseCase, testDispatcher)
    }

    @Test
    fun `ExportBackup intent should delegate call to usecase and call onSuccess`() =
        runTest(testDispatcher) {
            // Given
            val onSuccess: () -> Unit = mock()
            val onError: (Exception) -> Unit = mock()

            // When
            viewModel.onIntent(BackupIntent.ExportBackup(uri, onSuccess, onError))

            // Then
            verify(exportGiftIdeasUseCase).invoke(uri)
            verify(onSuccess).invoke()
        }

    @Test
    fun `ExportBackup intent should call onError when usecase throws exception`() =
        runTest(testDispatcher) {
            // Given
            val exception = RuntimeException("Export failed")
            whenever(exportGiftIdeasUseCase(uri)).thenThrow(exception)

            val onSuccess: () -> Unit = mock()
            val onError: (Exception) -> Unit = mock()

            // When
            viewModel.onIntent(BackupIntent.ExportBackup(uri, onSuccess, onError))

            // Then
            verify(onError).invoke(any())
        }

    @Test
    fun `ImportBackup intent should delegate to usecase and call onSuccess with count`() =
        runTest(testDispatcher) {
            // Given
            whenever(importGiftIdeasUseCase(uri)).thenReturn(5)

            val onSuccess: (Int) -> Unit = mock()
            val onInvalid: () -> Unit = mock()
            val onError: (Exception) -> Unit = mock()

            // When
            viewModel.onIntent(BackupIntent.ImportBackup(uri, onSuccess, onInvalid, onError))

            // Then
            verify(importGiftIdeasUseCase).invoke(uri)
            verify(onSuccess).invoke(5)
        }

    @Test
    fun `ImportBackup intent should call onInvalid when usecase returns negative count`() =
        runTest(testDispatcher) {
            // Given
            whenever(importGiftIdeasUseCase(uri)).thenReturn(-1)

            val onSuccess: (Int) -> Unit = mock()
            val onInvalid: () -> Unit = mock()
            val onError: (Exception) -> Unit = mock()

            // When
            viewModel.onIntent(BackupIntent.ImportBackup(uri, onSuccess, onInvalid, onError))

            // Then
            verify(onInvalid).invoke()
        }

    @Test
    fun `ImportBackup intent should call onError when usecase throws exception`() =
        runTest(testDispatcher) {
            // Given
            val exception = RuntimeException("Import failed")
            whenever(importGiftIdeasUseCase(uri)).thenThrow(exception)

            val onSuccess: (Int) -> Unit = mock()
            val onInvalid: () -> Unit = mock()
            val onError: (Exception) -> Unit = mock()

            // When
            viewModel.onIntent(BackupIntent.ImportBackup(uri, onSuccess, onInvalid, onError))

            // Then
            verify(onError).invoke(any())
        }
}
