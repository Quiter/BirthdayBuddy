package com.heckmannch.birthdaybuddy.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.domain.model.PendingNotification
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class SnoozeWorkerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workerParams = mockk<WorkerParameters>(relaxed = true)
    private val contactRepository = mockk<ContactRepository>(relaxed = true)
    private val notificationRepository = mockk<NotificationRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val notificationHelper = mockk<NotificationHelper>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.w(any(), any<String>()) } returns 0
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(notificationsEnabled = true)
        coEvery { notificationRepository.getPendingNotificationById(any()) } returns null
        coEvery {
            notificationHelper.showBirthdayNotification(any(), any(), any(), any(), any())
        } returns true
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createWorker() = SnoozeWorker(
        context = context,
        workerParameters = workerParams,
        contactRepository = contactRepository,
        notificationRepository = notificationRepository,
        settingsRepository = settingsRepository,
        notificationHelper = notificationHelper,
    )

    @Test
    fun `doWork - returns success without showing notification when notifications are disabled`() =
        runTest {
            // Arrange
            coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(notificationsEnabled = false)
            val inputData = Data.Builder()
                .putInt(NotificationActions.EXTRA_DAYS_BEFORE, 0)
                .putInt(NotificationActions.EXTRA_PENDING_ID, 100)
                .putStringArray(NotificationActions.EXTRA_LOOKUP_KEYS, arrayOf("contact_a"))
                .build()
            every { workerParams.inputData } returns inputData

            val worker = createWorker()

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            coVerify(exactly = 0) { notificationRepository.getPendingNotificationById(any()) }
            coVerify(exactly = 0) { contactRepository.getAllContactsImmediate() }
            coVerify(exactly = 0) {
                notificationHelper.showBirthdayNotification(any(), any(), any(), any(), any())
            }
        }

    @Test
    fun `doWork - returns success without showing notification when pending notification is already done`() =
        runTest {
            // Arrange
            val inputData = Data.Builder()
                .putInt(NotificationActions.EXTRA_DAYS_BEFORE, 0)
                .putInt(NotificationActions.EXTRA_PENDING_ID, 101)
                .putStringArray(NotificationActions.EXTRA_LOOKUP_KEYS, arrayOf("contact_a"))
                .build()
            every { workerParams.inputData } returns inputData

            coEvery { notificationRepository.getPendingNotificationById(101) } returns PendingNotification(
                contactLookupKeys = listOf("contact_a"),
                daysBefore = 0,
                year = 2026,
                isDone = true,
                id = 101,
            )

            val worker = createWorker()

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            coVerify(exactly = 1) { notificationRepository.getPendingNotificationById(101) }
            coVerify(exactly = 0) { contactRepository.getAllContactsImmediate() }
            coVerify(exactly = 0) {
                notificationHelper.showBirthdayNotification(any(), any(), any(), any(), any())
            }
        }

    @Test
    fun `doWork - shows notification when pending notification exists and is not done`() =
        runTest {
            // Arrange
            val inputData = Data.Builder()
                .putInt(NotificationActions.EXTRA_DAYS_BEFORE, 0)
                .putInt(NotificationActions.EXTRA_PENDING_ID, 101)
                .putStringArray(NotificationActions.EXTRA_LOOKUP_KEYS, arrayOf("contact_a"))
                .build()
            every { workerParams.inputData } returns inputData

            coEvery { notificationRepository.getPendingNotificationById(101) } returns PendingNotification(
                contactLookupKeys = listOf("contact_a"),
                daysBefore = 0,
                year = 2026,
                isDone = false,
                id = 101,
            )
            val contacts = listOf(Contact(contactId = "1", lookupKey = "contact_a", fullName = "Alice"))
            coEvery { contactRepository.getAllContactsImmediate() } returns contacts

            val worker = createWorker()

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            coVerify(exactly = 1) { contactRepository.getAllContactsImmediate() }
            coVerify(exactly = 1) {
                notificationHelper.showBirthdayNotification(
                    contacts = contacts,
                    daysBefore = 0,
                    pendingId = 101,
                    eventType = EventType.BIRTHDAY,
                )
            }
        }

    @Test
    fun `doWork - returns failure when LOOKUP_KEYS is missing`() = runTest {
        // Arrange
        val inputData = Data.Builder().build()
        every { workerParams.inputData } returns inputData

        val worker = createWorker()

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
            coEvery { contactRepository.getAllContactsImmediate() } returns contacts

            val worker = createWorker()

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            coVerify(exactly = 1) { contactRepository.getAllContactsImmediate() }
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
        coEvery { contactRepository.getAllContactsImmediate() } returns contacts

        val worker = createWorker()

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
        coEvery { contactRepository.getAllContactsImmediate() } returns contacts

        val worker = createWorker()

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
            coEvery { contactRepository.getAllContactsImmediate() } returns contacts

            val worker = createWorker()

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
            coEvery { contactRepository.getAllContactsImmediate() } returns contacts

            val worker = createWorker()

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            coVerify(exactly = 0) {
                notificationHelper.showBirthdayNotification(any(), any(), any(), any())
            }
        }

    @Test
    fun `doWork - logs warning and returns success when showBirthdayNotification returns false`() =
        runTest {
            // Arrange
            val inputData = Data.Builder()
                .putInt(NotificationActions.EXTRA_DAYS_BEFORE, 0)
                .putInt(NotificationActions.EXTRA_PENDING_ID, 106)
                .putStringArray(NotificationActions.EXTRA_LOOKUP_KEYS, arrayOf("contact_a"))
                .build()
            every { workerParams.inputData } returns inputData

            val contacts = listOf(
                Contact(contactId = "1", lookupKey = "contact_a", fullName = "Alice")
            )
            coEvery { contactRepository.getAllContactsImmediate() } returns contacts
            coEvery {
                notificationHelper.showBirthdayNotification(any(), any(), any(), any(), any())
            } returns false

            val worker = createWorker()

            // Act
            val result = worker.doWork()

            // Assert
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
            verify(exactly = 1) { android.util.Log.w(any(), any<String>()) }
        }
}
