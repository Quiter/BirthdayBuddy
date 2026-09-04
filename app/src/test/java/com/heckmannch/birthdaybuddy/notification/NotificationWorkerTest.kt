package com.heckmannch.birthdaybuddy.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.usecase.GetPendingNotificationsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class NotificationWorkerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val contactRepository = mockk<ContactRepository>(relaxed = true)
    private val notificationRepository = mockk<NotificationRepository>(relaxed = true)
    private val notificationHelper = mockk<NotificationHelper>(relaxed = true)
    private val getPendingNotificationsUseCase = mockk<GetPendingNotificationsUseCase>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any()) } returns workManager
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `scheduleNext - empty rules list - cancels unique work`() {
        // Act
        NotificationWorker.scheduleNext(context, emptyList())

        // Assert
        verify { workManager.cancelUniqueWork("FlexibleNotificationUpdate") }
    }

    @Test
    fun `scheduleNext - default policy - enqueues with REPLACE`() {
        val testNow = LocalDateTime.of(2026, 7, 9, 12, 0, 0)
        val rules = listOf(
            NotificationRule(id = 1, daysBefore = 0, hour = 15, minute = 0)
        )

        NotificationWorker.scheduleNext(context, rules, now = testNow)

        verify {
            workManager.enqueueUniqueWork(
                "FlexibleNotificationUpdate",
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>()
            )
        }
    }

    @Test
    fun `scheduleNext - rule in future today - schedules correct delay`() {
        val testNow = LocalDateTime.of(2026, 7, 9, 12, 0, 0)

        val rules = listOf(
            NotificationRule(id = 1, daysBefore = 0, hour = 15, minute = 0), // 3 hours in future
            NotificationRule(id = 2, daysBefore = 0, hour = 8, minute = 0)   // in the past today
        )

        val requestSlot = slot<OneTimeWorkRequest>()

        // Act
        NotificationWorker.scheduleNext(
            context = context,
            rules = rules,
            existingWorkPolicy = ExistingWorkPolicy.REPLACE,
            now = testNow
        )

        // Assert
        verify {
            workManager.enqueueUniqueWork(
                "FlexibleNotificationUpdate",
                ExistingWorkPolicy.REPLACE,
                capture(requestSlot)
            )
        }

        val capturedRequest = requestSlot.captured
        assertThat(capturedRequest.tags).contains("birthday_notification")
        // Expected delay is from 12:00:00 to 15:00:00 today = 3 hours = 10,800,000 ms
        assertThat(capturedRequest.workSpec.initialDelay).isEqualTo(10_800_000L)
    }

    @Test
    fun `scheduleNext - all rules in past today - schedules first rule for tomorrow with APPEND_OR_REPLACE`() {
        val testNow = LocalDateTime.of(2026, 7, 9, 12, 0, 0)

        val rules = listOf(
            NotificationRule(id = 1, daysBefore = 0, hour = 10, minute = 0), // past
            NotificationRule(id = 2, daysBefore = 0, hour = 8, minute = 0)   // past
        )

        val requestSlot = slot<OneTimeWorkRequest>()

        // Act
        NotificationWorker.scheduleNext(
            context = context,
            rules = rules,
            existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE,
            now = testNow
        )

        // Assert
        verify {
            workManager.enqueueUniqueWork(
                "FlexibleNotificationUpdate",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                capture(requestSlot)
            )
        }

        val capturedRequest = requestSlot.captured
        assertThat(capturedRequest.tags).contains("birthday_notification")
        // Earliest unique rule is 08:00 (since 08:00 < 10:00).
        // Target is 2026-07-10T08:00:00.
        // Delay from 2026-07-09T12:00:00 to 2026-07-10T08:00:00 is 20 hours = 72,000,000 ms
        assertThat(capturedRequest.workSpec.initialDelay).isEqualTo(72_000_000L)
    }

    @Test
    fun `doWork - success - cleans up, syncs, shows notifications, and enqueues next with APPEND_OR_REPLACE`() = runTest {
        val rules = listOf(
            NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0)
        )
        coEvery { notificationRepository.getSettingsImmediate() } returns AppSettings(notificationsEnabled = true)
        coEvery { notificationRepository.getAllRulesImmediate() } returns rules
        coEvery { getPendingNotificationsUseCase(any()) } returns emptyList()

        val worker = NotificationWorker(
            context = context,
            workerParameters = workerParameters,
            contactRepository = contactRepository,
            notificationRepository = notificationRepository,
            notificationHelper = notificationHelper,
            getPendingNotificationsUseCase = getPendingNotificationsUseCase
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify(exactly = 1) { notificationRepository.deleteOldNotifications(any()) }
        coVerify(exactly = 1) { contactRepository.syncContacts() }
        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                "FlexibleNotificationUpdate",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                any<OneTimeWorkRequest>()
            )
        }
    }

    @Test
    fun `doWork - notifications disabled - cancels unique work and does not schedule next`() = runTest {
        coEvery { notificationRepository.getSettingsImmediate() } returns AppSettings(notificationsEnabled = false)
        coEvery { getPendingNotificationsUseCase(any()) } returns emptyList()

        val worker = NotificationWorker(
            context = context,
            workerParameters = workerParameters,
            contactRepository = contactRepository,
            notificationRepository = notificationRepository,
            notificationHelper = notificationHelper,
            getPendingNotificationsUseCase = getPendingNotificationsUseCase
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(exactly = 1) { workManager.cancelUniqueWork("FlexibleNotificationUpdate") }
        verify(exactly = 0) {
            workManager.enqueueUniqueWork(
                any(),
                any(),
                any<OneTimeWorkRequest>()
            )
        }
    }
}
