package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
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
            verify(notificationRepository).updateSettings(calendarSyncEnabled = true)
            verify(contactRepository).getAllContactsImmediate()
            verify(calendarSyncRepository).syncBirthdays(contacts)
        }

    @Test
    fun `when enabled is false, updates settings and deletes calendar`() = runTest {
        // Act
        useCase(false)

        // Assert
        verify(notificationRepository).updateSettings(calendarSyncEnabled = false)
        verify(calendarSyncRepository).deleteCalendar()
    }
}
