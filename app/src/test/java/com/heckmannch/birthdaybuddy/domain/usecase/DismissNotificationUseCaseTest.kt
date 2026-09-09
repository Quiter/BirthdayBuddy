package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Unit tests for [DismissNotificationUseCase].
 */
class DismissNotificationUseCaseTest {

    private val notificationRepository: NotificationRepository = mock()
    private lateinit var useCase: DismissNotificationUseCase

    @Before
    fun setUp() {
        useCase = DismissNotificationUseCase(notificationRepository)
    }

    @Test
    fun `when invoked, delegates to incrementDismissCount on notificationRepository`() = runTest {
        val pendingId = 456

        useCase(pendingId)

        verify(notificationRepository).incrementDismissCount(pendingId)
    }
}
