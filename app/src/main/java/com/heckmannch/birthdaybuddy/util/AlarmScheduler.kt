package com.heckmannch.birthdaybuddy.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.notification.NotificationAlarmReceiver
import com.heckmannch.birthdaybuddy.widget.WidgetUpdateAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Central utility class for scheduling exact background alarms using [AlarmManager].
 *
 * Replaces WorkManager's delayed executions ([androidx.work.OneTimeWorkRequest.Builder.setInitialDelay])
 * for time-critical triggers (daily notifications and midnight widget updates) to circumvent Doze Mode
 * execution delays. Uses [AlarmManager.setExactAndAllowWhileIdle] when exact alarm permissions are granted,
 * and gracefully falls back to [AlarmManager.setAndAllowWhileIdle] if permissions are revoked,
 * complying with Google Play policies for `SCHEDULE_EXACT_ALARM`.
 *
 * @property context The application context.
 * @property notificationRepositoryProvider Lazy provider for [NotificationRepository] to prevent cyclic Hilt dependencies.
 * @property ioDispatcher Injected dispatcher for background I/O operations.
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val notificationRepositoryProvider: Provider<NotificationRepository>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Checks whether exact alarms can be scheduled on the current device.
     *
     * On Android 12 (API 31) and higher, verifies [AlarmManager.canScheduleExactAlarms].
     * On earlier versions, always returns `true`.
     *
     * @return `true` if exact alarms are allowed, `false` otherwise.
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService<AlarmManager>()
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    /**
     * Schedules the next exact notification alarm based on the configured notification rules at the current time.
     *
     * @param rules The list of active [NotificationRule] objects.
     * @return The epoch millisecond timestamp for which the alarm was scheduled, or `null` if cancelled/unavailable.
     */
    fun scheduleNextNotificationAlarm(rules: List<NotificationRule>): Long? =
        scheduleNextNotificationAlarm(rules, LocalDateTime.now())

    /**
     * Schedules the next exact notification alarm based on the configured notification rules and reference time.
     *
     * Finds the next upcoming execution time either later today or at the earliest rule tomorrow.
     * If the rule list is empty, cancels any currently pending notification alarm.
     *
     * @param rules The list of active [NotificationRule] objects.
     * @param now The current date and time for calculation.
     * @return The epoch millisecond timestamp for which the alarm was scheduled, or `null` if cancelled/unavailable.
     */
    fun scheduleNextNotificationAlarm(
        rules: List<NotificationRule>,
        now: LocalDateTime,
    ): Long? {
        if (rules.isEmpty()) {
            cancelNotificationAlarm()
            return null
        }

        val uniqueTimes = rules.asSequence()
            .map { LocalTime.of(it.hour, it.minute) }
            .distinct()
            .sorted()
            .toList()

        // Find the next time today, or fall back to the earliest time tomorrow
        val nextTime = uniqueTimes.firstOrNull { it.isAfter(now.toLocalTime()) }
            ?: uniqueTimes.first()

        var targetDateTime = LocalDateTime.of(now.toLocalDate(), nextTime)
        if (!targetDateTime.isAfter(now)) {
            targetDateTime = targetDateTime.plusDays(1)
        }

        val triggerAtMillis = targetDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val alarmManager = context.getSystemService<AlarmManager>() ?: return null
        val pendingIntent = getNotificationAlarmPendingIntent()

        setAlarm(alarmManager, triggerAtMillis, pendingIntent, "NotificationAlarm")
        return triggerAtMillis
    }

    /**
     * Cancels any pending exact notification alarm.
     */
    fun cancelNotificationAlarm() {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val pendingIntent = getNotificationAlarmPendingIntent()
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Notification alarm cancelled")
    }

    /**
     * Schedules the next exact widget update alarm for midnight (00:01 AM) based on the current time.
     *
     * @return The epoch millisecond timestamp for which the widget alarm was scheduled, or `null` if unavailable.
     */
    fun scheduleNextWidgetUpdateAlarm(): Long? =
        scheduleNextWidgetUpdateAlarm(LocalDateTime.now())

    /**
     * Schedules the next exact widget update alarm for midnight (00:01 AM) based on a reference time.
     *
     * Ensures the date rollover has completed so that homescreen widgets display
     * correct countdowns and birthday highlights for the new day.
     *
     * @param now The current date and time for calculation.
     * @return The epoch millisecond timestamp for which the widget alarm was scheduled, or `null` if unavailable.
     */
    fun scheduleNextWidgetUpdateAlarm(now: LocalDateTime): Long? {
        val targetTime = LocalTime.of(0, 1)
        var targetDateTime = LocalDateTime.of(now.toLocalDate(), targetTime)
        if (!targetDateTime.isAfter(now)) {
            targetDateTime = targetDateTime.plusDays(1)
        }

        val triggerAtMillis = targetDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val alarmManager = context.getSystemService<AlarmManager>() ?: return null
        val pendingIntent = getWidgetUpdateAlarmPendingIntent()

        setAlarm(alarmManager, triggerAtMillis, pendingIntent, "WidgetUpdateAlarm")
        return triggerAtMillis
    }

    /**
     * Cancels any pending widget update alarm.
     */
    fun cancelWidgetUpdateAlarm() {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val pendingIntent = getWidgetUpdateAlarmPendingIntent()
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Widget update alarm cancelled")
    }

    /**
     * Queries notification settings and rules from [NotificationRepository] and reschedules the notification alarm.
     *
     * If notifications are disabled or rules are empty, cancels the alarm.
     * Main-safe operation executed on [ioDispatcher].
     */
    suspend fun rescheduleNotificationAlarm(): Unit = withContext(ioDispatcher) {
        try {
            val repository = notificationRepositoryProvider.get()
            val settings = repository.getSettingsImmediate()
            if (!settings.notificationsEnabled) {
                cancelNotificationAlarm()
                return@withContext
            }

            val rules = repository.getAllRulesImmediate()
            if (rules.isEmpty()) {
                cancelNotificationAlarm()
                return@withContext
            }

            scheduleNextNotificationAlarm(rules)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule notification alarm from repository", e)
        }
    }

    /**
     * Reschedules all background alarms (notification and widget update).
     *
     * Typically invoked after device reboot (`BOOT_COMPLETED`), application updates (`MY_PACKAGE_REPLACED`),
     * timezone changes (`TIMEZONE_CHANGED`), or manual time adjustments (`TIME_SET`, `DATE_CHANGED`).
     * Main-safe operation executed on [ioDispatcher].
     */
    suspend fun rescheduleAllAlarms(): Unit = withContext(ioDispatcher) {
        rescheduleNotificationAlarm()
        scheduleNextWidgetUpdateAlarm()
    }

    private fun setAlarm(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        alarmTag: String,
    ) {
        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
                Log.d(TAG, "Exact alarm ($alarmTag) scheduled for $triggerAtMillis")
            } else {
                Log.w(
                    TAG,
                    "SCHEDULE_EXACT_ALARM permission not granted, falling back to setAndAllowWhileIdle for $alarmTag",
                )
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling exact alarm for $alarmTag, falling back", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Failed to set fallback alarm for $alarmTag", fallbackEx)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for $alarmTag", e)
        }
    }

    private fun getNotificationAlarmPendingIntent(): PendingIntent {
        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_NOTIFICATION_ALARM
        }
        return PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun getWidgetUpdateAlarmPendingIntent(): PendingIntent {
        val intent = Intent(context, WidgetUpdateAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_WIDGET_UPDATE_ALARM
        }
        return PendingIntent.getBroadcast(
            context,
            WIDGET_UPDATE_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val TAG = "AlarmScheduler"

        const val ACTION_TRIGGER_NOTIFICATION_ALARM =
            "com.heckmannch.birthdaybuddy.action.TRIGGER_NOTIFICATION_ALARM"
        const val ACTION_TRIGGER_WIDGET_UPDATE_ALARM =
            "com.heckmannch.birthdaybuddy.action.TRIGGER_WIDGET_UPDATE_ALARM"

        const val NOTIFICATION_ALARM_REQUEST_CODE = 1001
        const val WIDGET_UPDATE_ALARM_REQUEST_CODE = 1002
    }
}
