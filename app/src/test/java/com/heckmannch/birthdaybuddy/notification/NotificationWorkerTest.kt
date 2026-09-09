package com.heckmannch.birthdaybuddy.notification

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.domain.usecase.CleanupOldNotificationsUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.GetPendingNotificationsUseCase
import com.heckmannch.birthdaybuddy.util.AlarmScheduler
import com.heckmannch.birthdaybuddy.util.Clock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class NotificationWorkerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val contactRepository = mockk<ContactRepository>(relaxed = true)
    private val notificationRepository = mockk<NotificationRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val notificationHelper = mockk<NotificationHelper>(relaxed = true)
    private val cleanupOldNotificationsUseCase = mockk<CleanupOldNotificationsUseCase>(relaxed = true)
    private val getPendingNotificationsUseCase = mockk<GetPendingNotificationsUseCase>(relaxed = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
    private val clock = mockk<Clock>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any()) } returns workManager
        mockkStatic(android.util.Log::class)
        every { android.util.Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }


    @Test
    fun `doWork - success - cleans up, syncs, shows notifications, and enqueues next with APPEND_OR_REPLACE`() = runTest {
        val rules = listOf(
            NotificationRule(daysBefore = 0, hour = 9, minute = 0)
        )
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(notificationsEnabled = true)
        coEvery { notificationRepository.getAllRulesImmediate() } returns rules
        coEvery { getPendingNotificationsUseCase(any()) } returns emptyList()

        val worker = NotificationWorker(
            context = context,
            workerParameters = workerParameters,
            contactRepository = contactRepository,
            notificationRepository = notificationRepository,
            settingsRepository = settingsRepository,
            notificationHelper = notificationHelper,
            cleanupOldNotificationsUseCase = cleanupOldNotificationsUseCase,
            getPendingNotificationsUseCase = getPendingNotificationsUseCase,
            alarmScheduler = alarmScheduler,
            clock = clock,
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify(exactly = 1) { cleanupOldNotificationsUseCase(any()) }
        coVerify(exactly = 1) { contactRepository.syncContacts() }
        verify(exactly = 1) { alarmScheduler.scheduleNextNotificationAlarm(rules) }
    }

    @Test
    fun `doWork - notifications disabled - cancels unique work and does not schedule next`() = runTest {
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(notificationsEnabled = false)
        coEvery { getPendingNotificationsUseCase(any()) } returns emptyList()

        val worker = NotificationWorker(
            context = context,
            workerParameters = workerParameters,
            contactRepository = contactRepository,
            notificationRepository = notificationRepository,
            settingsRepository = settingsRepository,
            notificationHelper = notificationHelper,
            cleanupOldNotificationsUseCase = cleanupOldNotificationsUseCase,
            getPendingNotificationsUseCase = getPendingNotificationsUseCase,
            alarmScheduler = alarmScheduler,
            clock = clock,
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(exactly = 1) { workManager.cancelUniqueWork("FlexibleNotificationUpdate") }
        verify(exactly = 1) { alarmScheduler.cancelNotificationAlarm() }
        verify(exactly = 0) {
            workManager.enqueueUniqueWork(
                any(),
                any(),
                any<OneTimeWorkRequest>()
            )
        }
    }

    @Test
    fun `doWork - cancellation exception - rethrows CancellationException`() = runTest {
        coEvery { contactRepository.syncContacts() } throws CancellationException("Sync cancelled")
        val worker = NotificationWorker(
            context = context,
            workerParameters = workerParameters,
            contactRepository = contactRepository,
            notificationRepository = notificationRepository,
            settingsRepository = settingsRepository,
            notificationHelper = notificationHelper,
            cleanupOldNotificationsUseCase = cleanupOldNotificationsUseCase,
            getPendingNotificationsUseCase = getPendingNotificationsUseCase,
            alarmScheduler = alarmScheduler,
            clock = clock,
        )

        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking {
                worker.doWork()
            }
        }
    }

    @Test
    fun `doWork - exception in syncContacts - schedules next run and returns retry`() = runTest {
        val rules = listOf(
            NotificationRule(daysBefore = 0, hour = 9, minute = 0)
        )
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(notificationsEnabled = true)
        coEvery { notificationRepository.getAllRulesImmediate() } returns rules
        coEvery { contactRepository.syncContacts() } throws RuntimeException("Network/DB error during contact sync")

        val worker = NotificationWorker(
            context = context,
            workerParameters = workerParameters,
            contactRepository = contactRepository,
            notificationRepository = notificationRepository,
            settingsRepository = settingsRepository,
            notificationHelper = notificationHelper,
            cleanupOldNotificationsUseCase = cleanupOldNotificationsUseCase,
            getPendingNotificationsUseCase = getPendingNotificationsUseCase,
            alarmScheduler = alarmScheduler,
            clock = clock,
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        verify(exactly = 1) { alarmScheduler.scheduleNextNotificationAlarm(rules) }
    }

    @Test
    fun `doWork - exception in deleteOldNotifications - schedules next run and returns retry`() = runTest {
        val rules = listOf(
            NotificationRule(daysBefore = 0, hour = 9, minute = 0)
        )
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(notificationsEnabled = true)
        coEvery { notificationRepository.getAllRulesImmediate() } returns rules
        coEvery { cleanupOldNotificationsUseCase(any()) } throws RuntimeException("Database error")

        val worker = NotificationWorker(
            context = context,
            workerParameters = workerParameters,
            contactRepository = contactRepository,
            notificationRepository = notificationRepository,
            settingsRepository = settingsRepository,
            notificationHelper = notificationHelper,
            cleanupOldNotificationsUseCase = cleanupOldNotificationsUseCase,
            getPendingNotificationsUseCase = getPendingNotificationsUseCase,
            alarmScheduler = alarmScheduler,
            clock = clock,
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        verify(exactly = 1) { alarmScheduler.scheduleNextNotificationAlarm(rules) }
    }

    @Test
    fun `doWork - exception in getPendingNotificationsUseCase - schedules next run and returns retry`() = runTest {
        val rules = listOf(
            NotificationRule(daysBefore = 0, hour = 9, minute = 0)
        )
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(notificationsEnabled = true)
        coEvery { notificationRepository.getAllRulesImmediate() } returns rules
        coEvery { getPendingNotificationsUseCase(any()) } throws RuntimeException("UseCase error")

        val worker = NotificationWorker(
            context = context,
            workerParameters = workerParameters,
            contactRepository = contactRepository,
            notificationRepository = notificationRepository,
            settingsRepository = settingsRepository,
            notificationHelper = notificationHelper,
            cleanupOldNotificationsUseCase = cleanupOldNotificationsUseCase,
            getPendingNotificationsUseCase = getPendingNotificationsUseCase,
            alarmScheduler = alarmScheduler,
            clock = clock,
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        verify(exactly = 1) { alarmScheduler.scheduleNextNotificationAlarm(rules) }
    }

    @Test
    fun `doWork - exception and rescheduling fails - returns retry without throwing`() = runTest {
        coEvery { contactRepository.syncContacts() } throws RuntimeException("Initial failure")
        coEvery { settingsRepository.getSettingsImmediate() } throws RuntimeException("DB offline")

        val worker = NotificationWorker(
            context = context,
            workerParameters = workerParameters,
            contactRepository = contactRepository,
            notificationRepository = notificationRepository,
            settingsRepository = settingsRepository,
            notificationHelper = notificationHelper,
            cleanupOldNotificationsUseCase = cleanupOldNotificationsUseCase,
            getPendingNotificationsUseCase = getPendingNotificationsUseCase,
            alarmScheduler = alarmScheduler,
            clock = clock,
        )

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }
}
