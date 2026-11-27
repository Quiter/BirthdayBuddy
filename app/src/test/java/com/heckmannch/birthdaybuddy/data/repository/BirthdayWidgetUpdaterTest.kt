package com.heckmannch.birthdaybuddy.data.repository

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.util.AlarmScheduler
import com.heckmannch.birthdaybuddy.widget.BirthdayWidget
import com.heckmannch.birthdaybuddy.widget.BirthdayWidgetWorker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [BirthdayWidgetUpdater] verifying:
 * - [BirthdayWidgetUpdater.updateWidget]: updates all glance widgets and handles/rethrows exceptions.
 * - [BirthdayWidgetUpdater.scheduleDailyUpdate]: schedules exact midnight alarm via [AlarmScheduler].
 *
 * Note on WorkManager:
 * Per the project's Doze-resistant Single-Path widget strategy (see KDoc on [BirthdayWidgetUpdater]),
 * [BirthdayWidgetUpdater.scheduleDailyUpdate] sets an exact AlarmManager alarm for 00:01 via [AlarmScheduler].
 * WorkManager is not dispatched directly in advance here, but triggered when the alarm fires
 * in WidgetUpdateAlarmReceiver.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BirthdayWidgetUpdaterTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = mockk(relaxed = true)
    private val alarmScheduler: AlarmScheduler = mockk(relaxed = true)

    private lateinit var updater: BirthdayWidgetUpdater

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        mockkStatic("androidx.glance.appwidget.GlanceAppWidgetKt")

        updater = BirthdayWidgetUpdater(
            context = context,
            alarmScheduler = alarmScheduler,
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // =========================================================================
    // updateWidget Tests
    // =========================================================================

    @Test
    fun `updateWidget - success - updates BirthdayWidget on context`() = runTest {
        coEvery { any<BirthdayWidget>().updateAll(context) } returns Unit

        updater.updateWidget()

        coVerify(exactly = 1) { any<BirthdayWidget>().updateAll(context) }
    }

    @Test
    fun `updateWidget - generic exception - logs error and rethrows exception`() = runTest {
        coEvery { any<BirthdayWidget>().updateAll(context) } throws RuntimeException("Glance rendering crashed")

        var thrown: RuntimeException? = null
        try {
            updater.updateWidget()
        } catch (e: RuntimeException) {
            thrown = e
        }

        assertThat(thrown).isNotNull()
        assertThat(thrown).hasMessageThat().contains("Glance rendering crashed")
        verify(exactly = 1) {
            Log.e("BirthdayWidgetUpdater", "Widget update failed", any<RuntimeException>())
        }
    }

    @Test
    fun `updateWidget - failing updateAll causes BirthdayWidgetWorker doWork to return retry`() = runTest {
        coEvery { any<BirthdayWidget>().updateAll(context) } throws RuntimeException("Glance rendering crashed")
        val workerParameters: WorkerParameters = mockk(relaxed = true)
        val worker = BirthdayWidgetWorker(context, workerParameters, updater, alarmScheduler)

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        verify(exactly = 0) { alarmScheduler.scheduleNextWidgetUpdateAlarm() }
    }

    @Test
    fun `updateWidget - cancellation exception - rethrows CancellationException for coroutine safety`() = runTest {
        coEvery { any<BirthdayWidget>().updateAll(context) } throws CancellationException("Coroutines job cancelled")

        var thrown: CancellationException? = null
        try {
            updater.updateWidget()
        } catch (e: CancellationException) {
            thrown = e
        }
        assertThat(thrown).isNotNull()
        assertThat(thrown).hasMessageThat().contains("Coroutines job cancelled")
    }

    // =========================================================================
    // scheduleDailyUpdate Tests
    // =========================================================================

    @Test
    fun `scheduleDailyUpdate - success - delegates to AlarmScheduler scheduleNextWidgetUpdateAlarm`() {
        every { alarmScheduler.scheduleNextWidgetUpdateAlarm() } returns 123456789L

        updater.scheduleDailyUpdate()

        verify(exactly = 1) { alarmScheduler.scheduleNextWidgetUpdateAlarm() }
    }

    @Test
    fun `scheduleDailyUpdate - exception - catches exception and logs error without crashing`() {
        every { alarmScheduler.scheduleNextWidgetUpdateAlarm() } throws SecurityException("Exact alarm permission denied")

        updater.scheduleDailyUpdate()

        verify(exactly = 1) {
            Log.e("BirthdayWidgetUpdater", "Widget scheduling failed", any<SecurityException>())
        }
    }
}
