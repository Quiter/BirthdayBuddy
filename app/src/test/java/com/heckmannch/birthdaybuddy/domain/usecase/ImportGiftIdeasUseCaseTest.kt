package com.heckmannch.birthdaybuddy.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ImportGiftIdeasUseCaseTest {

    private val contactRepository: ContactRepository = mock()
    private lateinit var useCase: ImportGiftIdeasUseCase

    @Before
    fun setUp() {
        useCase = ImportGiftIdeasUseCase(contactRepository)
    }

    @Test
    fun `invoke should delegate import call to repository`() = runTest {
        // Arrange
        val json = "{\"ideas\": []}"
        whenever(contactRepository.importGiftIdeas(json)).thenReturn(5)

        // Act
        val result = useCase(json)

        // Assert
        assertThat(result).isEqualTo(5)
    }
}
