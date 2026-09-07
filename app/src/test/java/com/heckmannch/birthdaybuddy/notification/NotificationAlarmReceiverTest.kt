package com.heckmannch.birthdaybuddy.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.heckmannch.birthdaybuddy.util.AlarmScheduler
import dagger.hilt.android.EntryPointAccessors
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NotificationAlarmReceiverTest {

    private val context = mockk<Context>(relaxed = true)
    private val appContext = mockk<Context>(relaxed = true)
    private val entryPoint = mockk<NotificationAlarmReceiver.NotificationAlarmReceiverEntryPoint>()
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val pendingResult = mockk<BroadcastReceiver.PendingResult>(relaxed = true)
    private val testScope = CoroutineScope(Dispatchers.Unconfined)
    private val testDispatcher = Dispatchers.Unconfined

    private lateinit var receiver: NotificationAlarmReceiver

    @Before
    fun setUp() {
        receiver = spyk(NotificationAlarmReceiver())
        every { receiver.goAsync() } returns pendingResult
        every { context.applicationContext } returns appContext

        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any()) } returns workManager

        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0

        mockkStatic(EntryPointAccessors::class)
        every {
            EntryPointAccessors.fromApplication(
                appContext,
                NotificationAlarmReceiver.NotificationAlarmReceiverEntryPoint::class.java,
            )
        } returns entryPoint

        every { entryPoint.applicationScope() } returns testScope
        every { entryPoint.ioDispatcher() } returns testDispatcher
        every { entryPoint.alarmScheduler() } returns alarmScheduler
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createIntent(): Intent {
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns AlarmScheduler.ACTION_TRIGGER_NOTIFICATION_ALARM
        return intent
    }

    @Test
    fun `onReceive enqueues NotificationWorker immediately with REPLACE and reschedules alarm`() {
        val intent = createIntent()

        receiver.onReceive(context, intent)

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                NotificationActions.WORK_NAME_NOTIFICATION_UPDATE,
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>(),
            )
        }
        coVerify(exactly = 1) { alarmScheduler.rescheduleNotificationAlarm() }
        verify(exactly = 1) { pendingResult.finish() }
    }

    @Test
    fun `onReceive with null context or null intent does nothing`() {
        receiver.onReceive(null, createIntent())
        receiver.onReceive(context, null)

        verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
        coVerify(exactly = 0) { alarmScheduler.rescheduleNotificationAlarm() }
        verify(exactly = 0) { receiver.goAsync() }
    }

    @Test
    fun `onReceive handles EntryPoint resolution failure gracefully`() {
        every {
            EntryPointAccessors.fromApplication(
                appContext,
                NotificationAlarmReceiver.NotificationAlarmReceiverEntryPoint::class.java,
            )
        } throws IllegalStateException("Hilt EntryPoint not found")

        val intent = createIntent()
        receiver.onReceive(context, intent)

        // Worker should still be enqueued even if EntryPoint resolution fails
        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                NotificationActions.WORK_NAME_NOTIFICATION_UPDATE,
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>(),
            )
        }
        coVerify(exactly = 0) { alarmScheduler.rescheduleNotificationAlarm() }
        verify(exactly = 0) { receiver.goAsync() }
    }

    @Test
    fun `onReceive handles rescheduling exception gracefully and still finishes pending result`() {
        coEvery { alarmScheduler.rescheduleNotificationAlarm() } throws RuntimeException("Alarm scheduling failure")

        val intent = createIntent()
        receiver.onReceive(context, intent)

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                NotificationActions.WORK_NAME_NOTIFICATION_UPDATE,
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>(),
            )
        }
        coVerify(exactly = 1) { alarmScheduler.rescheduleNotificationAlarm() }
        verify(exactly = 1) { pendingResult.finish() }
    }
}
