package com.heckmannch.birthdaybuddy.widget

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import com.heckmannch.birthdaybuddy.util.AlarmScheduler
import com.heckmannch.birthdaybuddy.util.Clock
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Worker to trigger updating the app widget.
 *
 * Widget-Update-Strategie (Single-Path):
 * - Ausführung & Retry-Mechanismus: [BirthdayWidgetWorker] wird asynchron durch [WidgetUpdateAlarmReceiver.enqueueImmediateWork]
 *   angestoßen, sobald der exakte Mitternachts-Alarm feuert. Bei temporären Ausfällen oder Ressourcenengpässen
 *   liefert [doWork] [Result.retry] zurück, sodass WorkManager das Update mit linearem Backoff zuverlässig wiederholt.
 * - Deterministisches Scheduling: Das tägliche Scheduling für Mitternacht erfolgt primär über [AlarmScheduler]
 *   mittels [android.app.AlarmManager.setExactAndAllowWhileIdle]. WorkManager wird NICHT für verzögerte 24h-Dauerläufe
 *   genutzt, um Doze-Mode-Verzögerungen zu vermeiden.
 *
 * @property widgetUpdater Abstraction for updating the application widget.
 * @property alarmScheduler Scheduler for setting exact alarms.
 * @property clock Abstraction for system time.
 */
@HiltWorker
class BirthdayWidgetWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val widgetUpdater: WidgetUpdater,
    private val alarmScheduler: AlarmScheduler,
    private val clock: Clock,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        return try {
            widgetUpdater.updateWidget()
            // Schedule the next run for tomorrow midnight cleanly and deterministically via AlarmScheduler.
            // Uses AlarmManager exact alarm to wake up even in Doze Mode.
            alarmScheduler.scheduleNextWidgetUpdateAlarm()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update birthday widget", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "BirthdayWidgetWorker"
        private const val WORK_NAME = "DailyWidgetUpdateSingle"
        private const val WORK_TAG = "daily_widget_update"

        /**
         * Enqueues the widget worker immediately to update Glance widgets (called by [WidgetUpdateAlarmReceiver]).
         */
        @JvmStatic
        fun enqueueImmediateWork(context: Context) {
            val request = OneTimeWorkRequestBuilder<BirthdayWidgetWorker>()
                // Linearer Backoff (10s), um zeitkritische Widget-Aktualisierungen bei temporären Fehlern rasch zu wiederholen
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .addTag(WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        @VisibleForTesting
        internal fun calculateDelayUntilMidnight(now: LocalDateTime = LocalDateTime.now()): Long {
            // We plan for 00:01 AM to ensure the date has actually rolled over.
            val midnight = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.of(0, 1))
            return Duration.between(now, midnight).toMillis()
        }
    }
}
