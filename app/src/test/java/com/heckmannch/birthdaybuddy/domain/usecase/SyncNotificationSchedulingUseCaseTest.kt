package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Unit tests for [SyncNotificationSchedulingUseCase].
 */
class SyncNotificationSchedulingUseCaseTest {

    private val notificationRepository: NotificationRepository = mock()
    private lateinit var useCase: SyncNotificationSchedulingUseCase

    @Before
    fun setUp() {
        useCase = SyncNotificationSchedulingUseCase(notificationRepository)
    }

    @Test
    fun `when invoked, delegates to syncScheduling on notificationRepository`() = runTest {
        useCase()

        verify(notificationRepository).syncScheduling()
    }
}
