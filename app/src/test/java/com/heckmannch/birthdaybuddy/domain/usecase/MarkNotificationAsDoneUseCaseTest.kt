package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Unit tests for [MarkNotificationAsDoneUseCase].
 */
class MarkNotificationAsDoneUseCaseTest {

    private val notificationRepository: NotificationRepository = mock()
    private lateinit var useCase: MarkNotificationAsDoneUseCase

    @Before
    fun setUp() {
        useCase = MarkNotificationAsDoneUseCase(notificationRepository)
    }

    @Test
    fun `when invoked, delegates to markAsDone on notificationRepository`() = runTest {
        val pendingId = 123

        useCase(pendingId)

        verify(notificationRepository).markAsDone(pendingId)
    }
}
