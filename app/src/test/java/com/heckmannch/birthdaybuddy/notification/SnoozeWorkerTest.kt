package com.heckmannch.birthdaybuddy.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

class SnoozeWorkerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workerParams = mockk<WorkerParameters>(relaxed = true)
    private val contactRepository = mockk<ContactRepository>(relaxed = true)
    private val notificationHelper = mockk<NotificationHelper>(relaxed = true)

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `doWork - returns failure when LOOKUP_KEYS is missing`() = runTest {
        // Arrange
        val inputData = Data.Builder().build()
        every { workerParams.inputData } returns inputData

        val worker = SnoozeWorker(context, workerParams, contactRepository, notificationHelper)

        // Act
        val result = worker.doWork()

        // Assert
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `doWork - processes lookup keys and triggers notification with correct event type`() =
        runTest {
            // Arrange
            val inputData = Data.Builder()
                .putInt(NotificationActions.EXTRA_DAYS_BEFORE, 2)
                .putInt(NotificationActions.EXTRA_PENDING_ID, 101)
                .putStringArray(
                    NotificationActions.EXTRA_LOOKUP_KEYS,
                    arrayOf("anniversary:contact_a", "nameday:contact_b"),
                )
                .build()
            every { workerParams.inputData } returns inputData

            val contacts = listOf(
                Contact(contactId = "1", lookupKey = "contact_a", fullName = "Alice"),
                Contact(contactId = "2", lookupKey = "contact_b", fullName = "Bob"),
                Contact(contactId = "3", lookupKey = "contact_c", fullName = "Charlie")
            )
            every { contactRepository.allContacts } returns flowOf(contacts)

            val worker = SnoozeWorker(context, workerParams, contactRepository, notificationHelper)

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            coVerify {
                notificationHelper.showBirthdayNotification(
                    contacts = listOf(contacts[0], contacts[1]),
                    daysBefore = 2,
                    pendingId = 101,
                    eventType = EventType.ANNIVERSARY
                )
            }
        }

    @Test
    fun `doWork - infers nameday event type from prefix`() = runTest {
        // Arrange
        val inputData = Data.Builder()
            .putInt(NotificationActions.EXTRA_DAYS_BEFORE, 0)
            .putInt(NotificationActions.EXTRA_PENDING_ID, 102)
            .putStringArray(NotificationActions.EXTRA_LOOKUP_KEYS, arrayOf("nameday:contact_b"))
            .build()
        every { workerParams.inputData } returns inputData

        val contacts = listOf(
            Contact(contactId = "2", lookupKey = "contact_b", fullName = "Bob")
        )
        every { contactRepository.allContacts } returns flowOf(contacts)

        val worker = SnoozeWorker(context, workerParams, contactRepository, notificationHelper)

        // Act
        val result = worker.doWork()

        // Assert
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify {
            notificationHelper.showBirthdayNotification(
                contacts = contacts,
                daysBefore = 0,
                pendingId = 102,
                eventType = EventType.NAME_DAY
            )
        }
    }

    @Test
    fun `doWork - uses specified event type from inputData`() = runTest {
        // Arrange
        val inputData = Data.Builder()
            .putInt(NotificationActions.EXTRA_DAYS_BEFORE, 1)
            .putInt(NotificationActions.EXTRA_PENDING_ID, 103)
            .putStringArray(NotificationActions.EXTRA_LOOKUP_KEYS, arrayOf("anniversary:contact_a"))
            .putString(NotificationActions.EXTRA_EVENT_TYPE, "NAME_DAY")
            .build()
        every { workerParams.inputData } returns inputData

        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "contact_a", fullName = "Alice")
        )
        every { contactRepository.allContacts } returns flowOf(contacts)

        val worker = SnoozeWorker(context, workerParams, contactRepository, notificationHelper)

        // Act
        val result = worker.doWork()

        // Assert
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify {
            notificationHelper.showBirthdayNotification(
                contacts = contacts,
                daysBefore = 1,
                pendingId = 103,
                eventType = EventType.NAME_DAY
            )
        }
    }

    @Test
    fun `doWork - falls back to birthday event type when invalid event type string is provided`() =
        runTest {
            // Arrange
            val inputData = Data.Builder()
                .putInt(NotificationActions.EXTRA_DAYS_BEFORE, 1)
                .putInt(NotificationActions.EXTRA_PENDING_ID, 104)
                .putStringArray(NotificationActions.EXTRA_LOOKUP_KEYS, arrayOf("anniversary:contact_a"))
                .putString(NotificationActions.EXTRA_EVENT_TYPE, "INVALID_TYPE")
                .build()
            every { workerParams.inputData } returns inputData

            val contacts = listOf(
                Contact(contactId = "1", lookupKey = "contact_a", fullName = "Alice")
            )
            every { contactRepository.allContacts } returns flowOf(contacts)

            val worker = SnoozeWorker(context, workerParams, contactRepository, notificationHelper)

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            coVerify {
                notificationHelper.showBirthdayNotification(
                    contacts = contacts,
                    daysBefore = 1,
                    pendingId = 104,
                    eventType = EventType.BIRTHDAY
                )
            }
        }

    @Test
    fun `doWork - returns success but shows no notification when target contacts are empty`() =
        runTest {
            // Arrange
            val inputData = Data.Builder()
                .putInt(NotificationActions.EXTRA_DAYS_BEFORE, 1)
                .putInt(NotificationActions.EXTRA_PENDING_ID, 105)
                .putStringArray(
                    NotificationActions.EXTRA_LOOKUP_KEYS,
                    arrayOf("contact_non_existent")
                )
                .build()
            every { workerParams.inputData } returns inputData

            val contacts = listOf(
                Contact(contactId = "1", lookupKey = "contact_a", fullName = "Alice")
            )
            every { contactRepository.allContacts } returns flowOf(contacts)

            val worker = SnoozeWorker(context, workerParams, contactRepository, notificationHelper)

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            coVerify(exactly = 0) {
                notificationHelper.showBirthdayNotification(any(), any(), any(), any())
            }
        }
}
