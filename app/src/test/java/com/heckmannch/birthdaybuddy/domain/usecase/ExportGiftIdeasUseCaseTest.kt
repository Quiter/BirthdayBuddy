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
class ExportGiftIdeasUseCaseTest {

    private val contactRepository: ContactRepository = mock()
    private lateinit var useCase: ExportGiftIdeasUseCase

    @Before
    fun setUp() {
        useCase = ExportGiftIdeasUseCase(contactRepository)
    }

    @Test
    fun `invoke should delegate export call to repository`() = runTest {
        // Arrange
        val expectedJson = "{\"ideas\": []}"
        whenever(contactRepository.exportGiftIdeas()).thenReturn(expectedJson)

        // Act
        val result = useCase()

        // Assert
        assertThat(result).isEqualTo(expectedJson)
    }
}
