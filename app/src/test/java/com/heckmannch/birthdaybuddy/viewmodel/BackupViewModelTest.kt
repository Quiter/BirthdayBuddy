package com.heckmannch.birthdaybuddy.viewmodel

import android.content.ContentResolver
import android.net.Uri
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
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

    private val contactRepository: ContactRepository = mock()
    private val contentResolver: ContentResolver = mock()
    private val uri: Uri = mock()

    private lateinit var viewModel: BackupViewModel

    @Before
    fun setup() {
        viewModel = BackupViewModel(contactRepository)
        viewModel.ioDispatcher = testDispatcher
    }

    @Test
    fun `exportGiftIdeas should write JSON from repository to output stream`() =
        runTest(testDispatcher) {
            // Given
            val testJson = "{\"test\": \"data\"}"
            whenever(contactRepository.exportGiftIdeas()).thenReturn(testJson)
            val outputStream = ByteArrayOutputStream()
            whenever(contentResolver.openOutputStream(uri)).thenReturn(outputStream)

            val onSuccess: () -> Unit = mock()
            val onError: (Exception) -> Unit = mock()

            // When
            viewModel.exportGiftIdeas(contentResolver, uri, onSuccess, onError)

            // Then
            verify(contactRepository).exportGiftIdeas()
            verify(contentResolver).openOutputStream(uri)
            verify(onSuccess).invoke()
            assert(outputStream.toString() == testJson)
        }

    @Test
    fun `exportGiftIdeas should call onError when exception occurs`() = runTest(testDispatcher) {
        // Given
        val exception = RuntimeException("Export failed")
        whenever(contactRepository.exportGiftIdeas()).thenThrow(exception)

        val onSuccess: () -> Unit = mock()
        val onError: (Exception) -> Unit = mock()

        // When
        viewModel.exportGiftIdeas(contentResolver, uri, onSuccess, onError)

        // Then
        verify(onError).invoke(any())
    }

    @Test
    fun `importGiftIdeas should read JSON from input stream and delegate to repository`() =
        runTest(testDispatcher) {
            // Given
            val testJson = "{\"test\": \"data\"}"
            val inputStream = ByteArrayInputStream(testJson.toByteArray())
            whenever(contentResolver.openInputStream(uri)).thenReturn(inputStream)
            whenever(contactRepository.importGiftIdeas(testJson)).thenReturn(5)

            val onSuccess: (Int) -> Unit = mock()
            val onInvalid: () -> Unit = mock()
            val onError: (Exception) -> Unit = mock()

            // When
            viewModel.importGiftIdeas(contentResolver, uri, onSuccess, onInvalid, onError)

            // Then
            verify(contentResolver).openInputStream(uri)
            verify(contactRepository).importGiftIdeas(testJson)
            verify(onSuccess).invoke(5)
        }

    @Test
    fun `importGiftIdeas should call onInvalid when repository returns negative count`() =
        runTest(testDispatcher) {
            // Given
            val testJson = "invalid json"
            val inputStream = ByteArrayInputStream(testJson.toByteArray())
            whenever(contentResolver.openInputStream(uri)).thenReturn(inputStream)
            whenever(contactRepository.importGiftIdeas(testJson)).thenReturn(-1)

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
