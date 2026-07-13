package com.heckmannch.birthdaybuddy.widget

import androidx.annotation.VisibleForTesting
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.heckmannch.birthdaybuddy.di.ApplicationScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

/**
 * Worker to trigger updating the app widget.
 *
 * @property applicationScope Application-scoped coroutine scope for post-work scheduling.
 *   Intentionally outlives the Worker to schedule the next run after WorkManager finishes this
 *   execution, ensuring the next run is scheduled without being cancelled by the worker termination.
 */
@HiltWorker
class BirthdayWidgetWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        try {
            BirthdayWidget().updateAll(context)
            // Schedule the next run for tomorrow midnight cleanly with a short delay.
            // This prevents a race condition where the currently running worker cancels itself through REPLACE.
            applicationScope.launch {
                delay(1000.milliseconds)
                enqueueNextUpdate(context)
            }
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "DailyWidgetUpdateSingle"

        fun enqueueNextUpdate(context: Context) {
            val request = OneTimeWorkRequestBuilder<BirthdayWidgetWorker>()
                .setInitialDelay(calculateDelayUntilMidnight(), TimeUnit.MILLISECONDS)
                .addTag("daily_widget_update")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
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
