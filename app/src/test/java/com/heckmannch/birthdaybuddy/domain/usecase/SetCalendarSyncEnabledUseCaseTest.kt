package com.heckmannch.birthdaybuddy.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SetCalendarSyncEnabledUseCaseTest {

    private val notificationRepository: NotificationRepository = mock()
    private val calendarSyncRepository: CalendarSyncRepository = mock()
    private val contactRepository: ContactRepository = mock()

    private lateinit var useCase: SetCalendarSyncEnabledUseCase

    @Before
    fun setUp() {
        useCase = SetCalendarSyncEnabledUseCase(
            notificationRepository,
            calendarSyncRepository,
            contactRepository
        )
    }

    @Test
    fun `when enabled is true, updates settings and triggers calendar sync with contacts`() =
        runTest {
            // Arrange
            val contacts = listOf(
                Contact(
                    contactId = "1",
                    lookupKey = "1",
                    fullName = "Max Mustermann",
                    birthday = null
                )
            )
            whenever(contactRepository.getAllContactsImmediate()).thenReturn(contacts)

            // Act
            useCase(true)

            // Assert
            val captor = argumentCaptor<(AppSettings) -> AppSettings>()
            verify(notificationRepository).updateSettings(captor.capture())
            val updated = captor.firstValue(AppSettings(calendarSyncEnabled = false))
            assertThat(updated.calendarSyncEnabled).isTrue()

            verify(contactRepository).getAllContactsImmediate()
            verify(calendarSyncRepository).syncBirthdays(contacts)
        }

    @Test
    fun `when enabled is false, updates settings and deletes calendar`() = runTest {
        // Act
        useCase(false)

        // Assert
        val captor = argumentCaptor<(AppSettings) -> AppSettings>()
        verify(notificationRepository).updateSettings(captor.capture())
        val updated = captor.firstValue(AppSettings(calendarSyncEnabled = true))
        assertThat(updated.calendarSyncEnabled).isFalse()

        verify(calendarSyncRepository).deleteCalendar()
    }
}
