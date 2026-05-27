package com.heckmannch.birthdaybuddy.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

@HiltWorker
class BirthdayWidgetWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        try {
            BirthdayWidget().updateAll(context)
            // Plane den nächsten Lauf für morgen Mitternacht sauber mit einer kurzen Verzögerung auf dem Main-Thread.
            // Dies verhindert eine Race-Condition, bei der sich der aktuell laufende Worker durch REPLACE selbst abbricht.
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                enqueueNextUpdate(context)
            }, 1000)
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

        private fun calculateDelayUntilMidnight(): Long {
            val now = LocalDateTime.now()
            // Wir planen für 00:01 Uhr, um sicherzustellen, dass das Datum wirklich umgesprungen ist
            val midnight = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.of(0, 1))
            return Duration.between(now, midnight).toMillis()
        }
    }
}
