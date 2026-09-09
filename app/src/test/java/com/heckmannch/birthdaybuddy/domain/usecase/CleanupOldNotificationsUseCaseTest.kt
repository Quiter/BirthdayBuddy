package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Unit tests for [CleanupOldNotificationsUseCase].
 */
class CleanupOldNotificationsUseCaseTest {

    private val notificationRepository: NotificationRepository = mock()
    private lateinit var useCase: CleanupOldNotificationsUseCase

    @Before
    fun setUp() {
        useCase = CleanupOldNotificationsUseCase(notificationRepository)
    }

    @Test
    fun `when invoked, delegates to deleteOldNotifications on notificationRepository`() = runTest {
        val year = 2026

        useCase(year)

        verify(notificationRepository).deleteOldNotifications(year)
    }
}
