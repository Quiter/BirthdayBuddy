package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.domain.repository.NotificationScheduler
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ReshowNotificationUseCaseTest {

    private val notificationScheduler: NotificationScheduler = mock()
    private lateinit var useCase: ReshowNotificationUseCase

    @Before
    fun setUp() {
        useCase = ReshowNotificationUseCase(notificationScheduler)
    }

    @Test
    fun `when invoked with default delay, calls reshowNotification on scheduler`() {
        // Arrange
        val pendingId = 42
        val daysBefore = 3
        val lookupKeys = listOf("key1", "key2")
        val eventType = EventType.BIRTHDAY

        // Act
        useCase(pendingId, daysBefore, lookupKeys, eventType)

        // Assert
        verify(notificationScheduler).reshowNotification(
            pendingId = pendingId,
            daysBefore = daysBefore,
            lookupKeys = lookupKeys,
            eventType = eventType,
            delayMillis = 500L
        )
    }

    @Test
    fun `when invoked with custom delay, calls reshowNotification on scheduler`() {
        // Arrange
        val pendingId = 42
        val daysBefore = 3
        val lookupKeys = listOf("anniversary:key1")
        val eventType = EventType.ANNIVERSARY
        val customDelay = 1000L

        // Act
        useCase(pendingId, daysBefore, lookupKeys, eventType, customDelay)

        // Assert
        verify(notificationScheduler).reshowNotification(
            pendingId = pendingId,
            daysBefore = daysBefore,
            lookupKeys = lookupKeys,
            eventType = eventType,
            delayMillis = customDelay
        )
    }
}
