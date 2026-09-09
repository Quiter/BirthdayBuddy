package com.heckmannch.birthdaybuddy.data.repository

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.util.AlarmScheduler
import com.heckmannch.birthdaybuddy.widget.BirthdayWidget
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
@OptIn(ExperimentalCoroutinesApi::class)
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
    fun `updateWidget - generic exception - catches error and logs without rethrowing`() = runTest {
        coEvery { any<BirthdayWidget>().updateAll(context) } throws RuntimeException("Glance rendering crashed")

        updater.updateWidget()

        // Should not throw, verify exception was caught and logged
        verify(exactly = 1) {
            Log.e("BirthdayWidgetUpdater", "Widget update failed", any<RuntimeException>())
        }
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
