package com.heckmannch.birthdaybuddy.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.heckmannch.birthdaybuddy.util.AlarmScheduler
import dagger.hilt.android.EntryPointAccessors
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
class WidgetUpdateAlarmReceiverTest {

    private val context = mockk<Context>(relaxed = true)
    private val appContext = mockk<Context>(relaxed = true)
    private val entryPoint = mockk<WidgetUpdateAlarmReceiver.WidgetUpdateAlarmReceiverEntryPoint>()
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val pendingResult = mockk<BroadcastReceiver.PendingResult>(relaxed = true)
    private val testScope = CoroutineScope(Dispatchers.Unconfined)
    private val testDispatcher = Dispatchers.Unconfined

    private lateinit var receiver: WidgetUpdateAlarmReceiver

    @Before
    fun setUp() {
        receiver = spyk(WidgetUpdateAlarmReceiver())
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
                WidgetUpdateAlarmReceiver.WidgetUpdateAlarmReceiverEntryPoint::class.java,
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
        every { intent.action } returns AlarmScheduler.ACTION_TRIGGER_WIDGET_UPDATE_ALARM
        return intent
    }

    @Test
    fun `onReceive enqueues BirthdayWidgetWorker immediately with REPLACE and reschedules midnight alarm`() {
        val intent = createIntent()

        receiver.onReceive(context, intent)

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                "DailyWidgetUpdateSingle",
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>(),
            )
        }
        verify(exactly = 1) { alarmScheduler.scheduleNextWidgetUpdateAlarm() }
        verify(exactly = 1) { pendingResult.finish() }
    }

    @Test
    fun `onReceive with null context or null intent does nothing`() {
        receiver.onReceive(null, createIntent())
        receiver.onReceive(context, null)

        verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
        verify(exactly = 0) { alarmScheduler.scheduleNextWidgetUpdateAlarm() }
        verify(exactly = 0) { receiver.goAsync() }
    }

    @Test
    fun `onReceive handles EntryPoint resolution failure gracefully`() {
        every {
            EntryPointAccessors.fromApplication(
                appContext,
                WidgetUpdateAlarmReceiver.WidgetUpdateAlarmReceiverEntryPoint::class.java,
            )
        } throws IllegalStateException("Hilt EntryPoint not found")

        val intent = createIntent()
        receiver.onReceive(context, intent)

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                "DailyWidgetUpdateSingle",
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>(),
            )
        }
        verify(exactly = 0) { alarmScheduler.scheduleNextWidgetUpdateAlarm() }
        verify(exactly = 0) { receiver.goAsync() }
    }

    @Test
    fun `onReceive handles rescheduling exception gracefully and still finishes pending result`() {
        every { alarmScheduler.scheduleNextWidgetUpdateAlarm() } throws RuntimeException("Widget alarm scheduling failure")

        val intent = createIntent()
        receiver.onReceive(context, intent)

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                "DailyWidgetUpdateSingle",
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>(),
            )
        }
        verify(exactly = 1) { alarmScheduler.scheduleNextWidgetUpdateAlarm() }
        verify(exactly = 1) { pendingResult.finish() }
    }
}
