package com.heckmannch.birthdaybuddy.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * JVM Unit Tests for [UpdateCalendarColorUseCase].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UpdateCalendarColorUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val calendarSyncRepository: CalendarSyncRepository = mockk()
    private lateinit var useCase: UpdateCalendarColorUseCase

    @Before
    fun setUp() {
        useCase = UpdateCalendarColorUseCase(calendarSyncRepository)
    }

    @Test
    fun `when repository returns true, returns true`() = runTest {
        // Arrange
        val type = CalendarSyncRepository.CalendarType.BIRTHDAY
        val color = 0xFFFF0000.toInt()
        coEvery { calendarSyncRepository.updateCalendarColor(type, color) } returns true

        // Act
        val result = useCase(type, color)

        // Assert
        assertThat(result).isTrue()
        coVerify(exactly = 1) { calendarSyncRepository.updateCalendarColor(type, color) }
    }

    @Test
    fun `when repository returns false, returns false`() = runTest {
        // Arrange
        val type = CalendarSyncRepository.CalendarType.ANNIVERSARY
        val color = 0xFF00FF00.toInt()
        coEvery { calendarSyncRepository.updateCalendarColor(type, color) } returns false

        // Act
        val result = useCase(type, color)

        // Assert
        assertThat(result).isFalse()
        coVerify(exactly = 1) { calendarSyncRepository.updateCalendarColor(type, color) }
    }

    @Test
    fun `when repository throws exception, propagates the exception`() = runTest {
        // Arrange
        val type = CalendarSyncRepository.CalendarType.NAMEDAY
        val color = 0xFF0000FF.toInt()
        val exceptionMessage = "Failed to update calendar color"
        coEvery { calendarSyncRepository.updateCalendarColor(type, color) } throws RuntimeException(
            exceptionMessage
        )

        // Act & Assert
        try {
            useCase(type, color)
            org.junit.Assert.fail("Expected RuntimeException to be thrown")
        } catch (e: RuntimeException) {
            assertThat(e).hasMessageThat().isEqualTo(exceptionMessage)
        }
    }
}
