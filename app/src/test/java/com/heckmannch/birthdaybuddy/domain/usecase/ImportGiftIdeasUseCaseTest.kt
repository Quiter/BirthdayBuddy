package com.heckmannch.birthdaybuddy.domain.usecase

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
    private val uriString: String = "content://com.android.providers.downloads.documents/document/123"
    private lateinit var useCase: ImportGiftIdeasUseCase

    @Before
    fun setUp() {
        useCase = ImportGiftIdeasUseCase(contactRepository)
    }

    @Test
    fun `invoke should delegate import call to repository`() = runTest {
        // Arrange
        whenever(contactRepository.importGiftIdeas(uriString)).thenReturn(5)

        // Act
        val result = useCase(uriString)

        // Assert
        verify(contactRepository).importGiftIdeas(uriString)
        assertThat(result).isEqualTo(5)
    }
}
