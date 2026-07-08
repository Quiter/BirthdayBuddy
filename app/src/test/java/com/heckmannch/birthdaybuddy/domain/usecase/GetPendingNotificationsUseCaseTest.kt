package com.heckmannch.birthdaybuddy.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class GetPendingNotificationsUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contactRepository: ContactRepository = mock()
    private val notificationRepository: NotificationRepository = mock()

    private lateinit var useCase: GetPendingNotificationsUseCase

    private val baseTime = LocalDateTime.of(2024, 5, 15, 9, 0) // May 15, 2024, 09:00

    @Before
    fun setUp() {
        useCase = GetPendingNotificationsUseCase(contactRepository, notificationRepository)
    }

    @Test
    fun `when notifications are disabled, returns empty list`() = runTest {
        // Arrange
        val settings = AppSettings(notificationsEnabled = false)
        whenever(notificationRepository.settings).thenReturn(flowOf(settings))

        // Act
        val result = useCase(baseTime)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `when rules are empty, returns empty list`() = runTest {
        // Arrange
        val settings = AppSettings(notificationsEnabled = true)
        whenever(notificationRepository.settings).thenReturn(flowOf(settings))
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(emptyList())

        // Act
        val result = useCase(baseTime)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `when rules are present but none are due in 45-minute window, returns empty list`() = runTest {
        // Arrange
        val settings = AppSettings(notificationsEnabled = true)
        val rule = NotificationRule(id = 1, daysBefore = 0, hour = 10, minute = 0) // 10:00 (base is 9:00)
        whenever(notificationRepository.settings).thenReturn(flowOf(settings))
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))

        // Act
        val result = useCase(baseTime)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `when rules are present and due, but no contacts match event date, returns empty list`() = runTest {
        // Arrange
        val settings = AppSettings(notificationsEnabled = true)
        val rule = NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0) // 09:00 (active)
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "key1",
                fullName = "John Doe",
                birthday = LocalDate.of(1990, 6, 20) // Different date
            )
        )
        whenever(notificationRepository.settings).thenReturn(flowOf(settings))
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
        whenever(contactRepository.allContacts).thenReturn(flowOf(contacts))

        // Act
        val result = useCase(baseTime)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `when rule is due, and contact matches birthday, returns birthday event`() = runTest {
        // Arrange
        val settings = AppSettings(notificationsEnabled = true)
        val rule = NotificationRule(id = 1, daysBefore = 1, hour = 9, minute = 0) // 09:00 (active, 1 day before)
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "key1",
                fullName = "John Doe",
                birthday = LocalDate.of(1990, 5, 16) // May 16 (tomorrow)
            )
        )
        whenever(notificationRepository.settings).thenReturn(flowOf(settings))
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
        whenever(contactRepository.allContacts).thenReturn(flowOf(contacts))
        whenever(notificationRepository.hasNotificationBeenScheduled(eq(2024), eq(1), eq("key1")))
            .thenReturn(false)

        // Act
        val result = useCase(baseTime)

        // Assert
        assertThat(result).hasSize(1)
        val event = result[0]
        assertThat(event.eventType).isEqualTo("birthday")
        assertThat(event.contacts).containsExactly(contacts[0])
        assertThat(event.daysBefore).isEqualTo(1)
        assertThat(event.dbKeys).containsExactly("key1")
    }

    @Test
    fun `when birthday was already scheduled, returns empty list`() = runTest {
        // Arrange
        val settings = AppSettings(notificationsEnabled = true)
        val rule = NotificationRule(id = 1, daysBefore = 1, hour = 9, minute = 0)
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "key1",
                fullName = "John Doe",
                birthday = LocalDate.of(1990, 5, 16)
            )
        )
        whenever(notificationRepository.settings).thenReturn(flowOf(settings))
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
        whenever(contactRepository.allContacts).thenReturn(flowOf(contacts))
        whenever(notificationRepository.hasNotificationBeenScheduled(eq(2024), eq(1), eq("key1")))
            .thenReturn(true)

        // Act
        val result = useCase(baseTime)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `when other events are disabled, skips name days and anniversaries`() = runTest {
        // Arrange
        val settings = AppSettings(notificationsEnabled = true, otherEventsEnabled = false)
        val rule = NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0)
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "key1",
                fullName = "John Doe",
                anniversary = LocalDate.of(2010, 5, 15),
                nameDay = LocalDate.of(2000, 5, 15)
            )
        )
        whenever(notificationRepository.settings).thenReturn(flowOf(settings))
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
        whenever(contactRepository.allContacts).thenReturn(flowOf(contacts))

        // Act
        val result = useCase(baseTime)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `when other events are enabled, returns name day event`() = runTest {
        // Arrange
        val settings = AppSettings(notificationsEnabled = true, otherEventsEnabled = true)
        val rule = NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0)
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "key1",
                fullName = "John Doe",
                nameDay = LocalDate.of(2000, 5, 15)
            )
        )
        whenever(notificationRepository.settings).thenReturn(flowOf(settings))
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
        whenever(contactRepository.allContacts).thenReturn(flowOf(contacts))
        whenever(notificationRepository.hasNotificationBeenScheduled(eq(2024), eq(0), eq("nameday:key1")))
            .thenReturn(false)

        // Act
        val result = useCase(baseTime)

        // Assert
        assertThat(result).hasSize(1)
        val event = result[0]
        assertThat(event.eventType).isEqualTo("nameday")
        assertThat(event.dbKeys).containsExactly("nameday:key1")
    }

    @Test
    fun `when other events are enabled and spouse couples match, returns joint anniversary event`() = runTest {
        // Arrange
        val settings = AppSettings(notificationsEnabled = true, otherEventsEnabled = true)
        val rule = NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0)
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "key1",
                fullName = "John Doe",
                anniversary = LocalDate.of(2015, 5, 15),
                spouseLookupKey = "key2"
            ),
            Contact(
                contactId = "2",
                lookupKey = "key2",
                fullName = "Jane Doe",
                anniversary = LocalDate.of(2015, 5, 15),
                spouseLookupKey = "key1"
            )
        )
        whenever(notificationRepository.settings).thenReturn(flowOf(settings))
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
        whenever(contactRepository.allContacts).thenReturn(flowOf(contacts))
        whenever(notificationRepository.hasNotificationBeenScheduled(eq(2024), eq(0), any()))
            .thenReturn(false)

        // Act
        val result = useCase(baseTime)

        // Assert
        assertThat(result).hasSize(1)
        val event = result[0]
        assertThat(event.eventType).isEqualTo("anniversary")
        assertThat(event.contacts).containsExactly(contacts[0], contacts[1])
        assertThat(event.dbKeys).containsExactly("anniversary:key1", "anniversary:key2")
    }

    @Test
    fun `when other events are enabled but spouse couples anniversary was already scheduled, returns empty list`() = runTest {
        // Arrange
        val settings = AppSettings(notificationsEnabled = true, otherEventsEnabled = true)
        val rule = NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0)
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "key1",
                fullName = "John Doe",
                anniversary = LocalDate.of(2015, 5, 15),
                spouseLookupKey = "key2"
            ),
            Contact(
                contactId = "2",
                lookupKey = "key2",
                fullName = "Jane Doe",
                anniversary = LocalDate.of(2015, 5, 15),
                spouseLookupKey = "key1"
            )
        )
        whenever(notificationRepository.settings).thenReturn(flowOf(settings))
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
        whenever(contactRepository.allContacts).thenReturn(flowOf(contacts))
        // Simulate that one of the spouses' anniversaries was already scheduled
        whenever(notificationRepository.hasNotificationBeenScheduled(eq(2024), eq(0), eq("anniversary:key1")))
            .thenReturn(true)
        whenever(notificationRepository.hasNotificationBeenScheduled(eq(2024), eq(0), eq("anniversary:key2")))
            .thenReturn(false)

        // Act
        val result = useCase(baseTime)

        // Assert
        assertThat(result).isEmpty()
    }
}
