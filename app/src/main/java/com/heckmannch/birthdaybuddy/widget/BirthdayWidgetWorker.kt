package com.heckmannch.birthdaybuddy.widget

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
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
 * @property widgetUpdater Abstraction for updating the application widget.
 */
@HiltWorker
class BirthdayWidgetWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val widgetUpdater: WidgetUpdater,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        return try {
            widgetUpdater.updateWidget()
            // Schedule the next run for tomorrow midnight cleanly and deterministically.
            // Using APPEND_OR_REPLACE chains the next execution without canceling the currently
            // running worker or relying on an in-memory delayed coroutine susceptible to process kills.
            enqueueNextUpdate(context, ExistingWorkPolicy.APPEND_OR_REPLACE)
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
         * Enqueues the next daily widget update worker.
         *
         * @param context Application or component context.
         * @param existingWorkPolicy Policy for handling conflicts with existing work.
         *   Defaults to [ExistingWorkPolicy.KEEP] when scheduled externally (e.g. on app launch)
         *   to preserve any already scheduled update.
         */
        fun enqueueNextUpdate(
            context: Context,
            existingWorkPolicy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
        ) {
            val request = OneTimeWorkRequestBuilder<BirthdayWidgetWorker>()
                .setInitialDelay(calculateDelayUntilMidnight(), TimeUnit.MILLISECONDS)
                .addTag(WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                existingWorkPolicy,
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
