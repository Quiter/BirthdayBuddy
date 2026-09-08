package com.heckmannch.birthdaybuddy.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Provider

class AlarmSchedulerTest {

    private val context = mockk<Context>(relaxed = true)
    private val alarmManager = mockk<AlarmManager>(relaxed = true)
    private val notificationRepository = mockk<NotificationRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val repositoryProvider = Provider { notificationRepository }
    private val settingsRepositoryProvider = Provider { settingsRepository }
    private val testDispatcher: CoroutineDispatcher = Dispatchers.Unconfined

    private lateinit var scheduler: AlarmScheduler

    @Before
    fun setUp() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0

        mockkStatic(PendingIntent::class)
        every {
            PendingIntent.getBroadcast(
                any(),
                any(),
                any(),
                any(),
            )
        } returns mockk(relaxed = true)

        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        every { context.getSystemService(AlarmManager::class.java) } returns alarmManager

        scheduler = AlarmScheduler(
            context = context,
            notificationRepositoryProvider = repositoryProvider,
            settingsRepositoryProvider = settingsRepositoryProvider,
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // =========================================================================
    // Notification Alarm Tests
    // =========================================================================

    @Test
    fun `scheduleNextNotificationAlarm - empty rules - cancels alarm and returns null`() {
        val result = scheduler.scheduleNextNotificationAlarm(emptyList())

        assertThat(result).isNull()
        verify(exactly = 1) { alarmManager.cancel(any<PendingIntent>()) }
    }

    @Test
    fun `scheduleNextNotificationAlarm - rule in future today - schedules for today`() {
        // Arrange: 12:00:00, rule at 15:00:00
        val now = LocalDateTime.of(2026, 9, 7, 12, 0, 0)
        val rules = listOf(
            NotificationRule(daysBefore = 0, hour = 15, minute = 0),
            NotificationRule(daysBefore = 0, hour = 8, minute = 0),
        )

        val expectedTarget = LocalDateTime.of(2026, 9, 7, 15, 0, 0)
        val expectedMillis = expectedTarget.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Act
        val result = scheduler.scheduleNextNotificationAlarm(rules, now = now)

        // Assert
        assertThat(result).isEqualTo(expectedMillis)
        verify(atLeast = 1) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                expectedMillis,
                any<PendingIntent>(),
            )
        }
    }

    @Test
    fun `scheduleNextNotificationAlarm - all rules in past today - schedules earliest rule for tomorrow`() {
        // Arrange: 20:00:00, rules at 09:00:00 and 18:00:00
        val now = LocalDateTime.of(2026, 9, 7, 20, 0, 0)
        val rules = listOf(
            NotificationRule(daysBefore = 0, hour = 18, minute = 0),
            NotificationRule(daysBefore = 0, hour = 9, minute = 0),
        )

        val expectedTarget = LocalDateTime.of(2026, 9, 8, 9, 0, 0)
        val expectedMillis = expectedTarget.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Act
        val result = scheduler.scheduleNextNotificationAlarm(rules, now = now)

        // Assert
        assertThat(result).isEqualTo(expectedMillis)
        verify(atLeast = 1) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                expectedMillis,
                any<PendingIntent>(),
            )
        }
    }

    @Test
    fun `cancelNotificationAlarm - cancels pending intent and alarm in manager`() {
        val pendingIntent = mockk<PendingIntent>(relaxed = true)
        every {
            PendingIntent.getBroadcast(
                any(),
                AlarmScheduler.NOTIFICATION_ALARM_REQUEST_CODE,
                any(),
                any(),
            )
        } returns pendingIntent

        scheduler.cancelNotificationAlarm()

        verify(exactly = 1) { alarmManager.cancel(pendingIntent) }
        verify(exactly = 1) { pendingIntent.cancel() }
    }

    // =========================================================================
    // Widget Update Alarm Tests
    // =========================================================================

    @Test
    fun `scheduleNextWidgetUpdateAlarm - before midnight - schedules for tomorrow 00_01`() {
        val now = LocalDateTime.of(2026, 9, 7, 14, 30, 0)
        val expectedTarget = LocalDateTime.of(2026, 9, 8, 0, 1, 0)
        val expectedMillis = expectedTarget.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val result = scheduler.scheduleNextWidgetUpdateAlarm(now = now)

        assertThat(result).isEqualTo(expectedMillis)
        verify(atLeast = 1) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                expectedMillis,
                any<PendingIntent>(),
            )
        }
    }

    @Test
    fun `scheduleNextWidgetUpdateAlarm - just after midnight before 00_01 - schedules for today 00_01`() {
        val now = LocalDateTime.of(2026, 9, 7, 0, 0, 30)
        val expectedTarget = LocalDateTime.of(2026, 9, 7, 0, 1, 0)
        val expectedMillis = expectedTarget.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val result = scheduler.scheduleNextWidgetUpdateAlarm(now = now)

        assertThat(result).isEqualTo(expectedMillis)
        verify(atLeast = 1) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                expectedMillis,
                any<PendingIntent>(),
            )
        }
    }

    @Test
    fun `cancelWidgetUpdateAlarm - cancels pending intent and alarm in manager`() {
        val pendingIntent = mockk<PendingIntent>(relaxed = true)
        every {
            PendingIntent.getBroadcast(
                any(),
                AlarmScheduler.WIDGET_UPDATE_ALARM_REQUEST_CODE,
                any(),
                any(),
            )
        } returns pendingIntent

        scheduler.cancelWidgetUpdateAlarm()

        verify(exactly = 1) { alarmManager.cancel(pendingIntent) }
        verify(exactly = 1) { pendingIntent.cancel() }
    }

    // =========================================================================
    // Graceful Fallback & Exception Handling Tests
    // =========================================================================

    @Test
    fun `canScheduleExactAlarms - returns true on pre-S`() {
        val result = scheduler.canScheduleExactAlarms()
        assertThat(result).isTrue()
    }

    @Test
    fun `scheduleNextNotificationAlarm - SecurityException falls back to setAndAllowWhileIdle`() {
        val now = LocalDateTime.of(2026, 9, 7, 12, 0, 0)
        val rules = listOf(NotificationRule(daysBefore = 0, hour = 15, minute = 0))

        every {
            alarmManager.setExactAndAllowWhileIdle(any(), any(), any())
        } throws SecurityException("SCHEDULE_EXACT_ALARM permission revoked")

        scheduler.scheduleNextNotificationAlarm(rules, now = now)

        verify(atLeast = 1) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                any(),
                any<PendingIntent>(),
            )
        }
    }

    // =========================================================================
    // Reschedule Methods (Repository integration)
    // =========================================================================

    @Test
    fun `rescheduleNotificationAlarm - notifications enabled with rules - schedules next alarm`() = runTest {
        val rules = listOf(NotificationRule(daysBefore = 0, hour = 9, minute = 0))
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(notificationsEnabled = true)
        coEvery { notificationRepository.getAllRulesImmediate() } returns rules

        scheduler.rescheduleNotificationAlarm()

        verify(atLeast = 1) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                any(),
                any<PendingIntent>(),
            )
        }
    }

    @Test
    fun `rescheduleNotificationAlarm - notifications disabled - cancels alarm`() = runTest {
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(notificationsEnabled = false)

        scheduler.rescheduleNotificationAlarm()

        verify(atLeast = 1) { alarmManager.cancel(any<PendingIntent>()) }
    }

    @Test
    fun `rescheduleNotificationAlarm - empty rules - cancels alarm`() = runTest {
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(notificationsEnabled = true)
        coEvery { notificationRepository.getAllRulesImmediate() } returns emptyList()

        scheduler.rescheduleNotificationAlarm()

        verify(atLeast = 1) { alarmManager.cancel(any<PendingIntent>()) }
    }

    @Test
    fun `rescheduleAllAlarms - reschedules both notification and widget update alarms`() = runTest {
        val rules = listOf(NotificationRule(daysBefore = 0, hour = 9, minute = 0))
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(notificationsEnabled = true)
        coEvery { notificationRepository.getAllRulesImmediate() } returns rules

        scheduler.rescheduleAllAlarms()

        verify(atLeast = 2) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                any(),
                any<PendingIntent>(),
            )
        }
    }
}
