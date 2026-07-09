package com.heckmannch.birthdaybuddy.widget

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class BirthdayWidgetWorkerTest {

    @Test
    fun `calculateDelayUntilMidnight - at noon - calculates correct delay to next day 00_01`() {
        // Arrange: 9th July 2026, 12:00:00
        val now = LocalDateTime.of(2026, 7, 9, 12, 0, 0)
        val expectedTarget = LocalDateTime.of(2026, 7, 10, 0, 1, 0)
        val expectedDelay = Duration.between(now, expectedTarget).toMillis()

        // Act
        val actualDelay = BirthdayWidgetWorker.calculateDelayUntilMidnight(now)

        // Assert
        assertThat(actualDelay).isEqualTo(expectedDelay)
        assertThat(actualDelay).isEqualTo(43_260_000L) // 12 hours + 1 minute
    }

    @Test
    fun `calculateDelayUntilMidnight - at midnight - calculates correct delay to next day 00_01`() {
        // Arrange: 9th July 2026, 00:00:00
        val now = LocalDateTime.of(2026, 7, 9, 0, 0, 0)
        val expectedTarget = LocalDateTime.of(2026, 7, 10, 0, 1, 0)
        val expectedDelay = Duration.between(now, expectedTarget).toMillis()

        // Act
        val actualDelay = BirthdayWidgetWorker.calculateDelayUntilMidnight(now)

        // Assert
        assertThat(actualDelay).isEqualTo(expectedDelay)
        assertThat(actualDelay).isEqualTo(86_460_000L) // 24 hours + 1 minute
    }

    @Test
    fun `calculateDelayUntilMidnight - right before midnight - calculates correct delay to next day 00_01`() {
        // Arrange: 9th July 2026, 23:59:00
        val now = LocalDateTime.of(2026, 7, 9, 23, 59, 0)
        val expectedTarget = LocalDateTime.of(2026, 7, 10, 0, 1, 0)
        val expectedDelay = Duration.between(now, expectedTarget).toMillis()

        // Act
        val actualDelay = BirthdayWidgetWorker.calculateDelayUntilMidnight(now)

        // Assert
        assertThat(actualDelay).isEqualTo(expectedDelay)
        assertThat(actualDelay).isEqualTo(120_000L) // 2 minutes
    }
}
