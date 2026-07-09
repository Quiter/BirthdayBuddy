package com.heckmannch.birthdaybuddy.viewmodel

import android.content.ContentResolver
import android.net.Uri
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.usecase.ExportGiftIdeasUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.ImportGiftIdeasUseCase
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val exportGiftIdeasUseCase: ExportGiftIdeasUseCase = mock()
    private val importGiftIdeasUseCase: ImportGiftIdeasUseCase = mock()
    private val contentResolver: ContentResolver = mock()
    private val uri: Uri = mock()

    private lateinit var viewModel: BackupViewModel

    @Before
    fun setup() {
        viewModel = BackupViewModel(exportGiftIdeasUseCase, importGiftIdeasUseCase)
        viewModel.ioDispatcher = testDispatcher
    }

    @Test
    fun `exportGiftIdeas should write JSON from usecase to output stream`() =
        runTest(testDispatcher) {
            // Given
            val testJson = "{\"test\": \"data\"}"
            whenever(exportGiftIdeasUseCase()).thenReturn(testJson)
            val outputStream = ByteArrayOutputStream()
            whenever(contentResolver.openOutputStream(uri)).thenReturn(outputStream)

            val onSuccess: () -> Unit = mock()
            val onError: (Exception) -> Unit = mock()

            // When
            viewModel.exportGiftIdeas(contentResolver, uri, onSuccess, onError)

            // Then
            verify(exportGiftIdeasUseCase).invoke()
            verify(contentResolver).openOutputStream(uri)
            verify(onSuccess).invoke()
            assert(outputStream.toString() == testJson)
        }

    @Test
    fun `exportGiftIdeas should call onError when exception occurs`() = runTest(testDispatcher) {
        // Given
        val exception = RuntimeException("Export failed")
        whenever(exportGiftIdeasUseCase()).thenThrow(exception)

        val onSuccess: () -> Unit = mock()
        val onError: (Exception) -> Unit = mock()

        // When
        viewModel.exportGiftIdeas(contentResolver, uri, onSuccess, onError)

        // Then
        verify(onError).invoke(any())
    }

    @Test
    fun `importGiftIdeas should read JSON from input stream and delegate to usecase`() =
        runTest(testDispatcher) {
            // Given
            val testJson = "{\"test\": \"data\"}"
            val inputStream = ByteArrayInputStream(testJson.toByteArray())
            whenever(contentResolver.openInputStream(uri)).thenReturn(inputStream)
            whenever(importGiftIdeasUseCase(testJson)).thenReturn(5)

            val onSuccess: (Int) -> Unit = mock()
            val onInvalid: () -> Unit = mock()
            val onError: (Exception) -> Unit = mock()

            // When
            viewModel.importGiftIdeas(contentResolver, uri, onSuccess, onInvalid, onError)

            // Then
            verify(contentResolver).openInputStream(uri)
            verify(importGiftIdeasUseCase).invoke(testJson)
            verify(onSuccess).invoke(5)
        }

    @Test
    fun `importGiftIdeas should call onInvalid when usecase returns negative count`() =
        runTest(testDispatcher) {
            // Given
            val testJson = "invalid json"
            val inputStream = ByteArrayInputStream(testJson.toByteArray())
            whenever(contentResolver.openInputStream(uri)).thenReturn(inputStream)
            whenever(importGiftIdeasUseCase(testJson)).thenReturn(-1)

            val onSuccess: (Int) -> Unit = mock()
            val onInvalid: () -> Unit = mock()
            val onError: (Exception) -> Unit = mock()

            // When
            viewModel.importGiftIdeas(contentResolver, uri, onSuccess, onInvalid, onError)

            // Then
            verify(onInvalid).invoke()
        }

    @Test
    fun `importGiftIdeas should call onError when exception occurs`() = runTest(testDispatcher) {
        // Given
        val exception = RuntimeException("Import failed")
        whenever(contentResolver.openInputStream(uri)).thenThrow(exception)

        val onSuccess: (Int) -> Unit = mock()
        val onInvalid: () -> Unit = mock()
        val onError: (Exception) -> Unit = mock()

        // When
        viewModel.importGiftIdeas(contentResolver, uri, onSuccess, onInvalid, onError)

        // Then
        verify(onError).invoke(any())
    }
}
