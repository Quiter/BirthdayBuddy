package com.heckmannch.birthdaybuddy.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * JVM Unit Tests for [SyncCalendarUseCase].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncCalendarUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contactRepository: ContactRepository = mockk()
    private val calendarSyncRepository: CalendarSyncRepository = mockk()
    private val notificationRepository: NotificationRepository = mockk()

    private lateinit var useCase: SyncCalendarUseCase

    private val settingsFlow = MutableStateFlow(AppSettings())

    @Before
    fun setUp() {
        every { notificationRepository.settings } returns settingsFlow
        useCase = SyncCalendarUseCase(
            contactRepository = contactRepository,
            calendarSyncRepository = calendarSyncRepository,
            notificationRepository = notificationRepository
        )
    }

    @Test
    fun `when calendarSyncEnabled is true, fetches contacts and syncs calendar`() = runTest {
        // Arrange
        settingsFlow.value = AppSettings(calendarSyncEnabled = true)
        val contacts = listOf(
            Contact(
                contactId = "c1",
                lookupKey = "key1",
                fullName = "Max Mustermann",
                birthday = null
            )
        )
        coEvery { contactRepository.getAllContactsImmediate() } returns contacts
        coEvery { calendarSyncRepository.syncBirthdays(contacts) } returns true

        // Act
        useCase()

        // Assert
        coVerify(exactly = 1) { contactRepository.getAllContactsImmediate() }
        coVerify(exactly = 1) { calendarSyncRepository.syncBirthdays(contacts) }
    }

    @Test
    fun `when calendarSyncEnabled is false, does not sync calendar`() = runTest {
        // Arrange
        settingsFlow.value = AppSettings(calendarSyncEnabled = false)

        // Act
        useCase()

        // Assert
        coVerify(exactly = 0) { contactRepository.getAllContactsImmediate() }
        coVerify(exactly = 0) { calendarSyncRepository.syncBirthdays(any()) }
    }

    @Test
    fun `when syncBirthdays throws exception, propagates the exception`() = runTest {
        // Arrange
        settingsFlow.value = AppSettings(calendarSyncEnabled = true)
        val contacts = emptyList<Contact>()
        coEvery { contactRepository.getAllContactsImmediate() } returns contacts
        val exceptionMessage = "Sync failed"
        coEvery { calendarSyncRepository.syncBirthdays(contacts) } throws RuntimeException(exceptionMessage)

        // Act & Assert
        try {
            useCase()
            org.junit.Assert.fail("Expected RuntimeException to be thrown")
        } catch (e: RuntimeException) {
            assertThat(e).hasMessageThat().isEqualTo(exceptionMessage)
        }
    }

    @Test
    fun `when settings flow throws exception, propagates the exception`() = runTest {
        // Arrange
        val exceptionMessage = "Failed to load settings"
        every { notificationRepository.settings } returns flow { throw RuntimeException(exceptionMessage) }

        // Act & Assert
        try {
            useCase()
            org.junit.Assert.fail("Expected RuntimeException to be thrown")
        } catch (e: RuntimeException) {
            assertThat(e).hasMessageThat().isEqualTo(exceptionMessage)
        }
    }
}
