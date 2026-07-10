package com.heckmannch.birthdaybuddy.domain.usecase

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ImportGiftIdeasUseCaseTest {

    private val contactRepository: ContactRepository = mock()
    private val uri: Uri = mock()
    private lateinit var useCase: ImportGiftIdeasUseCase

    @Before
    fun setUp() {
        useCase = ImportGiftIdeasUseCase(contactRepository)
    }

    @Test
    fun `invoke should delegate import call to repository`() = runTest {
        // Arrange
        whenever(contactRepository.importGiftIdeas(uri)).thenReturn(5)

        // Act
        val result = useCase(uri)

        // Assert
        verify(contactRepository).importGiftIdeas(uri)
        assertThat(result).isEqualTo(5)
    }
}
