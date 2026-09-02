package com.heckmannch.birthdaybuddy.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
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
        whenever(contactRepository.labelsEnabled).thenReturn(flowOf(false))
        whenever(contactRepository.labelConfigs).thenReturn(flowOf(emptyList()))
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
    fun `when rules are present but in the future today, returns empty list`() = runTest {
        // Arrange
        val settings = AppSettings(notificationsEnabled = true)
        val rule =
            NotificationRule(id = 1, daysBefore = 0, hour = 10, minute = 0) // 10:00 (base is 9:00)
        whenever(notificationRepository.settings).thenReturn(flowOf(settings))
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))

        // Act
        val result = useCase(baseTime)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `when rules are present and due, but no contacts match event date, returns empty list`() =
        runTest {
            // Arrange
            val settings = AppSettings(notificationsEnabled = true)
            val rule =
                NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0) // 09:00 (active)
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
        val rule = NotificationRule(
            id = 1,
            daysBefore = 1,
            hour = 9,
            minute = 0
        ) // 09:00 (active, 1 day before)
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
        assertThat(event.eventType).isEqualTo(EventType.BIRTHDAY)
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
        whenever(
            notificationRepository.hasNotificationBeenScheduled(
                eq(2024),
                eq(0),
                eq("nameday:key1")
            )
        )
            .thenReturn(false)

        // Act
        val result = useCase(baseTime)

        // Assert
        assertThat(result).hasSize(1)
        val event = result[0]
        assertThat(event.eventType).isEqualTo(EventType.NAME_DAY)
        assertThat(event.dbKeys).containsExactly("nameday:key1")
    }

    @Test
    fun `when other events are enabled and spouse couples match, returns joint anniversary event`() =
        runTest {
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
            assertThat(event.eventType).isEqualTo(EventType.ANNIVERSARY)
            assertThat(event.contacts).containsExactly(contacts[0], contacts[1])
            assertThat(event.dbKeys).containsExactly("anniversary:key1", "anniversary:key2")
        }

    @Test
    fun `when other events are enabled but spouse couples anniversary was already scheduled, returns empty list`() =
        runTest {
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
            whenever(
                notificationRepository.hasNotificationBeenScheduled(
                    eq(2024),
                    eq(0),
                    eq("anniversary:key1")
                )
            )
                .thenReturn(true)
            whenever(
                notificationRepository.hasNotificationBeenScheduled(
                    eq(2024),
                    eq(0),
                    eq("anniversary:key2")
                )
            )
                .thenReturn(false)

            // Act
            val result = useCase(baseTime)

            // Assert
            assertThat(result).isEmpty()
        }

    @Test
    fun `when label notifications are disabled, skips contacts with that label`() = runTest {
        // Arrange
        val settings = AppSettings(notificationsEnabled = true)
        val rule = NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0)
        val contactWithDisabledLabel = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Work Colleague",
            birthday = LocalDate.of(1990, 5, 15),
            labels = listOf("Work")
        )
        val labelConfigs = listOf(
            LabelConfig(name = "Work", notificationsEnabled = false)
        )

        whenever(notificationRepository.settings).thenReturn(flowOf(settings))
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
        whenever(contactRepository.allContacts).thenReturn(flowOf(listOf(contactWithDisabledLabel)))
        whenever(contactRepository.labelsEnabled).thenReturn(flowOf(true))
        whenever(contactRepository.labelConfigs).thenReturn(flowOf(labelConfigs))
        whenever(
            notificationRepository.hasNotificationBeenScheduled(
                any(),
                any(),
                any()
            )
        ).thenReturn(false)

        // Act
        val result = useCase(baseTime)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `when contact has leap day birthday in leap year 2028 on Feb 29, returns birthday event`() =
        runTest {
            // Arrange
            val leapYearTime = LocalDateTime.of(2028, 2, 29, 9, 0)
            val settings = AppSettings(notificationsEnabled = true)
            val rule = NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0)
            val leapDayContact = Contact(
                contactId = "1",
                lookupKey = "leap_key",
                fullName = "Leap Year Baby",
                birthday = LocalDate.of(2000, 2, 29)
            )

            whenever(notificationRepository.settings).thenReturn(flowOf(settings))
            whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
            whenever(contactRepository.allContacts).thenReturn(flowOf(listOf(leapDayContact)))
            whenever(
                notificationRepository.hasNotificationBeenScheduled(
                    eq(2028),
                    eq(0),
                    eq("leap_key")
                )
            )
                .thenReturn(false)

            // Act
            val result = useCase(leapYearTime)

            // Assert
            assertThat(result).hasSize(1)
            val event = result[0]
            assertThat(event.eventType).isEqualTo(EventType.BIRTHDAY)
            assertThat(event.contacts).containsExactly(leapDayContact)
            assertThat(event.daysBefore).isEqualTo(0)
        }

    @Test
    fun `when contact has leap day birthday in leap year 2028 on Feb 28 with daysBefore 0, returns empty list`() =
        runTest {
            // Arrange
            val leapYearEve = LocalDateTime.of(2028, 2, 28, 9, 0)
            val settings = AppSettings(notificationsEnabled = true)
            val rule = NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0)
            val leapDayContact = Contact(
                contactId = "1",
                lookupKey = "leap_key",
                fullName = "Leap Year Baby",
                birthday = LocalDate.of(2000, 2, 29)
            )

            whenever(notificationRepository.settings).thenReturn(flowOf(settings))
            whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
            whenever(contactRepository.allContacts).thenReturn(flowOf(listOf(leapDayContact)))

            // Act
            val result = useCase(leapYearEve)

            // Assert
            assertThat(result).isEmpty()
        }

    @Test
    fun `when contact has leap day birthday in leap year 2028 on Feb 28 with daysBefore 1, returns birthday event`() =
        runTest {
            // Arrange
            val leapYearEve = LocalDateTime.of(2028, 2, 28, 9, 0)
            val settings = AppSettings(notificationsEnabled = true)
            val rule = NotificationRule(id = 1, daysBefore = 1, hour = 9, minute = 0)
            val leapDayContact = Contact(
                contactId = "1",
                lookupKey = "leap_key",
                fullName = "Leap Year Baby",
                birthday = LocalDate.of(2000, 2, 29)
            )

            whenever(notificationRepository.settings).thenReturn(flowOf(settings))
            whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
            whenever(contactRepository.allContacts).thenReturn(flowOf(listOf(leapDayContact)))
            whenever(
                notificationRepository.hasNotificationBeenScheduled(
                    eq(2028),
                    eq(1),
                    eq("leap_key")
                )
            )
                .thenReturn(false)

            // Act
            val result = useCase(leapYearEve)

            // Assert
            assertThat(result).hasSize(1)
            val event = result[0]
            assertThat(event.eventType).isEqualTo(EventType.BIRTHDAY)
            assertThat(event.contacts).containsExactly(leapDayContact)
            assertThat(event.daysBefore).isEqualTo(1)
        }

    @Test
    fun `when contact has leap day birthday in non-leap year 2027 on Feb 28 with daysBefore 0, returns birthday event`() =
        runTest {
            // Arrange
            val nonLeapYearDate = LocalDateTime.of(2027, 2, 28, 9, 0)
            val settings = AppSettings(notificationsEnabled = true)
            val rule = NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0)
            val leapDayContact = Contact(
                contactId = "1",
                lookupKey = "leap_key",
                fullName = "Leap Year Baby",
                birthday = LocalDate.of(2000, 2, 29)
            )

            whenever(notificationRepository.settings).thenReturn(flowOf(settings))
            whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
            whenever(contactRepository.allContacts).thenReturn(flowOf(listOf(leapDayContact)))
            whenever(
                notificationRepository.hasNotificationBeenScheduled(
                    eq(2027),
                    eq(0),
                    eq("leap_key")
                )
            )
                .thenReturn(false)

            // Act
            val result = useCase(nonLeapYearDate)

            // Assert
            assertThat(result).hasSize(1)
            val event = result[0]
            assertThat(event.eventType).isEqualTo(EventType.BIRTHDAY)
            assertThat(event.contacts).containsExactly(leapDayContact)
            assertThat(event.daysBefore).isEqualTo(0)
        }

    @Test
    fun `when contact has leap day birthday in non-leap year 2027 on Feb 27 with daysBefore 1, returns birthday event`() =
        runTest {
            // Arrange
            val nonLeapYearEve = LocalDateTime.of(2027, 2, 27, 9, 0)
            val settings = AppSettings(notificationsEnabled = true)
            val rule = NotificationRule(id = 1, daysBefore = 1, hour = 9, minute = 0)
            val leapDayContact = Contact(
                contactId = "1",
                lookupKey = "leap_key",
                fullName = "Leap Year Baby",
                birthday = LocalDate.of(2000, 2, 29)
            )

            whenever(notificationRepository.settings).thenReturn(flowOf(settings))
            whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
            whenever(contactRepository.allContacts).thenReturn(flowOf(listOf(leapDayContact)))
            whenever(
                notificationRepository.hasNotificationBeenScheduled(
                    eq(2027),
                    eq(1),
                    eq("leap_key")
                )
            )
                .thenReturn(false)

            // Act
            val result = useCase(nonLeapYearEve)

            // Assert
            assertThat(result).hasSize(1)
            val event = result[0]
            assertThat(event.eventType).isEqualTo(EventType.BIRTHDAY)
            assertThat(event.contacts).containsExactly(leapDayContact)
            assertThat(event.daysBefore).isEqualTo(1)
        }

    @Test
    fun `when contact has leap day birthday in non-leap year 2027 on Feb 27 with daysBefore 0, returns empty list`() =
        runTest {
            // Arrange
            val nonLeapYearEve = LocalDateTime.of(2027, 2, 27, 9, 0)
            val settings = AppSettings(notificationsEnabled = true)
            val rule = NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0)
            val leapDayContact = Contact(
                contactId = "1",
                lookupKey = "leap_key",
                fullName = "Leap Year Baby",
                birthday = LocalDate.of(2000, 2, 29)
            )

            whenever(notificationRepository.settings).thenReturn(flowOf(settings))
            whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
            whenever(contactRepository.allContacts).thenReturn(flowOf(listOf(leapDayContact)))

            // Act
            val result = useCase(nonLeapYearEve)

            // Assert
            assertThat(result).isEmpty()
        }

    @Test
    fun `when contact has leap day anniversary and name day in non-leap year 2027 on Feb 28, returns events`() =
        runTest {
            // Arrange
            val nonLeapYearDate = LocalDateTime.of(2027, 2, 28, 9, 0)
            val settings = AppSettings(notificationsEnabled = true, otherEventsEnabled = true)
            val rule = NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0)
            val leapDayContact = Contact(
                contactId = "1",
                lookupKey = "leap_key",
                fullName = "Leap Year Contact",
                anniversary = LocalDate.of(2016, 2, 29),
                nameDay = LocalDate.of(2004, 2, 29)
            )

            whenever(notificationRepository.settings).thenReturn(flowOf(settings))
            whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
            whenever(contactRepository.allContacts).thenReturn(flowOf(listOf(leapDayContact)))
            whenever(
                notificationRepository.hasNotificationBeenScheduled(
                    eq(2027),
                    eq(0),
                    eq("anniversary:leap_key")
                )
            )
                .thenReturn(false)
            whenever(
                notificationRepository.hasNotificationBeenScheduled(
                    eq(2027),
                    eq(0),
                    eq("nameday:leap_key")
                )
            )
                .thenReturn(false)

            // Act
            val result = useCase(nonLeapYearDate)

            // Assert
            assertThat(result).hasSize(2)
            val anniversaryEvent = result.first { it.eventType == EventType.ANNIVERSARY }
            assertThat(anniversaryEvent.contacts).containsExactly(leapDayContact)
            assertThat(anniversaryEvent.dbKeys).containsExactly("anniversary:leap_key")

            val nameDayEvent = result.first { it.eventType == EventType.NAME_DAY }
            assertThat(nameDayEvent.contacts).containsExactly(leapDayContact)
            assertThat(nameDayEvent.dbKeys).containsExactly("nameday:leap_key")
        }

    @Test
    fun `when worker execution is delayed by 50 minutes, rule is still evaluated and returns birthday event`() =
        runTest {
            // Arrange: Rule set to 09:00, worker executed at 09:50 (50 min delay due to Doze Mode / battery saver)
            val delayedTime = LocalDateTime.of(2024, 5, 15, 9, 50)
            val settings = AppSettings(notificationsEnabled = true)
            val rule = NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0)
            val contact = Contact(
                contactId = "1",
                lookupKey = "key1",
                fullName = "John Doe",
                birthday = LocalDate.of(1990, 5, 15)
            )

            whenever(notificationRepository.settings).thenReturn(flowOf(settings))
            whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
            whenever(contactRepository.allContacts).thenReturn(flowOf(listOf(contact)))
            whenever(
                notificationRepository.hasNotificationBeenScheduled(
                    eq(2024),
                    eq(0),
                    eq("key1")
                )
            )
                .thenReturn(false)

            // Act
            val result = useCase(delayedTime)

            // Assert
            assertThat(result).hasSize(1)
            val event = result[0]
            assertThat(event.eventType).isEqualTo(EventType.BIRTHDAY)
            assertThat(event.contacts).containsExactly(contact)
            assertThat(event.daysBefore).isEqualTo(0)
            assertThat(event.dbKeys).containsExactly("key1")
        }

    @Test
    fun `when worker execution is delayed by 2 hours, rule is still evaluated and returns birthday event`() =
        runTest {
            // Arrange: Rule set to 09:00, worker executed at 11:00 (2 hours delay)
            val delayedTime = LocalDateTime.of(2024, 5, 15, 11, 0)
            val settings = AppSettings(notificationsEnabled = true)
            val rule = NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0)
            val contact = Contact(
                contactId = "1",
                lookupKey = "key1",
                fullName = "John Doe",
                birthday = LocalDate.of(1990, 5, 15)
            )

            whenever(notificationRepository.settings).thenReturn(flowOf(settings))
            whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
            whenever(contactRepository.allContacts).thenReturn(flowOf(listOf(contact)))
            whenever(
                notificationRepository.hasNotificationBeenScheduled(
                    eq(2024),
                    eq(0),
                    eq("key1")
                )
            )
                .thenReturn(false)

            // Act
            val result = useCase(delayedTime)

            // Assert
            assertThat(result).hasSize(1)
            val event = result[0]
            assertThat(event.eventType).isEqualTo(EventType.BIRTHDAY)
            assertThat(event.contacts).containsExactly(contact)
            assertThat(event.daysBefore).isEqualTo(0)
        }

    @Test
    fun `when worker execution is delayed but notification was already scheduled earlier today, returns empty list`() =
        runTest {
            // Arrange: Rule set to 09:00, worker executed late at 14:00, but notification was already scheduled
            val delayedTime = LocalDateTime.of(2024, 5, 15, 14, 0)
            val settings = AppSettings(notificationsEnabled = true)
            val rule = NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0)
            val contact = Contact(
                contactId = "1",
                lookupKey = "key1",
                fullName = "John Doe",
                birthday = LocalDate.of(1990, 5, 15)
            )

            whenever(notificationRepository.settings).thenReturn(flowOf(settings))
            whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
            whenever(contactRepository.allContacts).thenReturn(flowOf(listOf(contact)))
            whenever(
                notificationRepository.hasNotificationBeenScheduled(
                    eq(2024),
                    eq(0),
                    eq("key1")
                )
            )
                .thenReturn(true)

            // Act
            val result = useCase(delayedTime)

            // Assert
            assertThat(result).isEmpty()
        }

    @Test
    fun `when multiple rules exist throughout the day, only rules whose scheduled time has arrived are evaluated`() =
        runTest {
            // Arrange: Rule 1 at 09:00 (due), Rule 2 at 18:00 (future), executed at 11:30
            val midDayTime = LocalDateTime.of(2024, 5, 15, 11, 30)
            val settings = AppSettings(notificationsEnabled = true)
            val ruleMorning = NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0)
            val ruleEvening = NotificationRule(id = 2, daysBefore = 1, hour = 18, minute = 0)
            val contactToday = Contact(
                contactId = "1",
                lookupKey = "key_today",
                fullName = "Today Birthday",
                birthday = LocalDate.of(1990, 5, 15)
            )
            val contactTomorrow = Contact(
                contactId = "2",
                lookupKey = "key_tomorrow",
                fullName = "Tomorrow Birthday",
                birthday = LocalDate.of(1990, 5, 16)
            )

            whenever(notificationRepository.settings).thenReturn(flowOf(settings))
            whenever(notificationRepository.getAllRulesImmediate()).thenReturn(
                listOf(
                    ruleMorning,
                    ruleEvening
                )
            )
            whenever(contactRepository.allContacts).thenReturn(
                flowOf(
                    listOf(
                        contactToday,
                        contactTomorrow
                    )
                )
            )
            whenever(
                notificationRepository.hasNotificationBeenScheduled(
                    eq(2024),
                    eq(0),
                    eq("key_today")
                )
            )
                .thenReturn(false)
            whenever(
                notificationRepository.hasNotificationBeenScheduled(
                    eq(2024),
                    eq(1),
                    eq("key_tomorrow")
                )
            )
                .thenReturn(false)

            // Act
            val result = useCase(midDayTime)

            // Assert: Only the morning rule (09:00 <= 11:30) is evaluated; evening rule (18:00 > 11:30) is skipped
            assertThat(result).hasSize(1)
            val event = result[0]
            assertThat(event.eventType).isEqualTo(EventType.BIRTHDAY)
            assertThat(event.contacts).containsExactly(contactToday)
            assertThat(event.daysBefore).isEqualTo(0)
            assertThat(event.dbKeys).containsExactly("key_today")
        }

    @Test
    fun `when contact has multiple labels and at least one is ignored, contact is ignored`() = runTest {
        // Arrange
        val settings = AppSettings(notificationsEnabled = true)
        val rule = NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0)
        val contact = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Multi Label Contact",
            birthday = LocalDate.of(1990, 5, 15),
            labels = listOf("Family", "Ex-Colleagues")
        )
        val labelConfigs = listOf(
            LabelConfig(name = "Family", isIgnored = false, notificationsEnabled = true),
            LabelConfig(name = "Ex-Colleagues", isIgnored = true, notificationsEnabled = true)
        )

        whenever(notificationRepository.settings).thenReturn(flowOf(settings))
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
        whenever(contactRepository.allContacts).thenReturn(flowOf(listOf(contact)))
        whenever(contactRepository.labelsEnabled).thenReturn(flowOf(true))
        whenever(contactRepository.labelConfigs).thenReturn(flowOf(labelConfigs))
        whenever(notificationRepository.hasNotificationBeenScheduled(any(), any(), any())).thenReturn(false)

        // Act
        val result = useCase(baseTime)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `when contact has both hidden and non-hidden labels and none are ignored, returns notification`() = runTest {
        // Arrange
        val settings = AppSettings(notificationsEnabled = true)
        val rule = NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0)
        val contact = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Multi Label Contact",
            birthday = LocalDate.of(1990, 5, 15),
            labels = listOf("Friends", "Work")
        )
        val labelConfigs = listOf(
            LabelConfig(name = "Friends", isIgnored = false, notificationsEnabled = true),
            LabelConfig(name = "Work", isIgnored = false, notificationsEnabled = false)
        )

        whenever(notificationRepository.settings).thenReturn(flowOf(settings))
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
        whenever(contactRepository.allContacts).thenReturn(flowOf(listOf(contact)))
        whenever(contactRepository.labelsEnabled).thenReturn(flowOf(true))
        whenever(contactRepository.labelConfigs).thenReturn(flowOf(labelConfigs))
        whenever(notificationRepository.hasNotificationBeenScheduled(eq(2024), eq(0), eq("key1"))).thenReturn(false)

        // Act
        val result = useCase(baseTime)

        // Assert
        assertThat(result).hasSize(1)
        val event = result[0]
        assertThat(event.eventType).isEqualTo(EventType.BIRTHDAY)
        assertThat(event.contacts).containsExactly(contact)
    }

    @Test
    fun `when contact has exclusively hidden labels, skips notification`() = runTest {
        // Arrange
        val settings = AppSettings(notificationsEnabled = true)
        val rule = NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0)
        val contact = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Hidden Labels Contact",
            birthday = LocalDate.of(1990, 5, 15),
            labels = listOf("Work", "Gym")
        )
        val labelConfigs = listOf(
            LabelConfig(name = "Work", isIgnored = false, notificationsEnabled = false),
            LabelConfig(name = "Gym", isIgnored = false, notificationsEnabled = false)
        )

        whenever(notificationRepository.settings).thenReturn(flowOf(settings))
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(rule))
        whenever(contactRepository.allContacts).thenReturn(flowOf(listOf(contact)))
        whenever(contactRepository.labelsEnabled).thenReturn(flowOf(true))
        whenever(contactRepository.labelConfigs).thenReturn(flowOf(labelConfigs))
        whenever(notificationRepository.hasNotificationBeenScheduled(any(), any(), any())).thenReturn(false)

        // Act
        val result = useCase(baseTime)

        // Assert
        assertThat(result).isEmpty()
    }
}
