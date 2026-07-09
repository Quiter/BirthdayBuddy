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
 * JVM Unit Tests for [UnlinkCoupleUseCase].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UnlinkCoupleUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contactRepository: ContactRepository = mockk()
    private lateinit var useCase: UnlinkCoupleUseCase

    @Before
    fun setUp() {
        useCase = UnlinkCoupleUseCase(contactRepository)
    }

    @Test
    fun `when invoked, unlinks couple in repository`() = runTest {
        // Arrange
        val lookupKey = "lookupKey"
        coEvery { contactRepository.unlinkCouple(lookupKey) } returns Unit

        // Act
        useCase(lookupKey)

        // Assert
        coVerify(exactly = 1) { contactRepository.unlinkCouple(lookupKey) }
    }

    @Test
    fun `when repository throws exception, propagates the exception`() = runTest {
        // Arrange
        val lookupKey = "lookupKey"
        val exceptionMessage = "Failed to unlink couple"
        coEvery { contactRepository.unlinkCouple(lookupKey) } throws RuntimeException(exceptionMessage)

        // Act & Assert
        try {
            useCase(lookupKey)
            org.junit.Assert.fail("Expected RuntimeException to be thrown")
        } catch (e: RuntimeException) {
            assertThat(e).hasMessageThat().isEqualTo(exceptionMessage)
        }
    }
}
