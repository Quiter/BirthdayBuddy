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
            // Plane den nächsten Lauf für morgen Mitternacht
            enqueueNextUpdate(context)
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
