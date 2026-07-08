package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.NotificationScheduler
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SnoozeNotificationUseCaseTest {

    private val notificationScheduler: NotificationScheduler = mock()
    private lateinit var useCase: SnoozeNotificationUseCase

    @Before
    fun setUp() {
        useCase = SnoozeNotificationUseCase(notificationScheduler)
    }

    @Test
    fun `when invoked, calls snoozeNotification on scheduler`() {
        // Arrange
        val pendingId = 42
        val daysBefore = 3
        val lookupKeys = listOf("key1", "key2")

        // Act
        useCase(pendingId, daysBefore, lookupKeys)

        // Assert
        verify(notificationScheduler).snoozeNotification(pendingId, daysBefore, lookupKeys)
    }
}
