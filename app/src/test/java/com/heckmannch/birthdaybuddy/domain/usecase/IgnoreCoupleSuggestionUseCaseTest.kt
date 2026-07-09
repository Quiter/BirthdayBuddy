package com.heckmannch.birthdaybuddy.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * JVM Unit Tests for [IgnoreCoupleSuggestionUseCase].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IgnoreCoupleSuggestionUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contactRepository: ContactRepository = mockk()
    private lateinit var useCase: IgnoreCoupleSuggestionUseCase

    @Before
    fun setUp() {
        useCase = IgnoreCoupleSuggestionUseCase(contactRepository)
    }

    @Test
    fun `when invoked, ignores couple suggestion in repository`() = runTest {
        // Arrange
        val lookupKey1 = "lookupKey1"
        val lookupKey2 = "lookupKey2"
        coEvery { contactRepository.ignoreCoupleSuggestion(lookupKey1, lookupKey2) } returns Unit

        // Act
        useCase(lookupKey1, lookupKey2)

        // Assert
        coVerify(exactly = 1) { contactRepository.ignoreCoupleSuggestion(lookupKey1, lookupKey2) }
    }

    @Test
    fun `when repository throws exception, propagates the exception`() = runTest {
        // Arrange
        val lookupKey1 = "lookupKey1"
        val lookupKey2 = "lookupKey2"
        val exceptionMessage = "Failed to ignore suggestion"
        coEvery { contactRepository.ignoreCoupleSuggestion(lookupKey1, lookupKey2) } throws RuntimeException(exceptionMessage)

        // Act & Assert
        try {
            useCase(lookupKey1, lookupKey2)
            org.junit.Assert.fail("Expected RuntimeException to be thrown")
        } catch (e: RuntimeException) {
            assertThat(e).hasMessageThat().isEqualTo(exceptionMessage)
        }
    }
}
