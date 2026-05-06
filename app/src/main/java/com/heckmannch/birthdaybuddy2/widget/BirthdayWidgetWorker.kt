package com.heckmannch.birthdaybuddy2.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class BirthdayWidgetWorker(
    private val context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        try {
            BirthdayWidget().updateAll(context)
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    companion object {
        fun enqueueDailyUpdate(context: Context) {
            val request = PeriodicWorkRequestBuilder<BirthdayWidgetWorker>(
                24, TimeUnit.HOURS, // Explizit 24 Stunden
                15, TimeUnit.MINUTES // Flex Interval für System-Optimierung
            ).setInitialDelay(calculateDelayUntilMidnight(), TimeUnit.MILLISECONDS)
                .addTag("daily_widget_update")
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "DailyBirthdayUpdate",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        private fun calculateDelayUntilMidnight(): Long {
            val now = LocalDateTime.now()
            val midnight = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT)
            return Duration.between(now, midnight).toMillis()
        }
    }
}
