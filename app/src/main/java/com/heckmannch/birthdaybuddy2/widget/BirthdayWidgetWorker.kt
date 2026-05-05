package com.heckmannch.birthdaybuddy2.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.*
import java.util.concurrent.TimeUnit

class BirthdayWidgetWorker(
    private val context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        BirthdayWidget().updateAll(context)
        return Result.success()
    }

    companion object {
        fun enqueueDailyUpdate(context: Context) {
            val request = PeriodicWorkRequestBuilder<BirthdayWidgetWorker>(
                1,
                TimeUnit.DAYS,
            ).setInitialDelay(calculateDelayUntilMidnight(), TimeUnit.MILLISECONDS)
                .addTag("daily_widget_update")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "DailyBirthdayUpdate",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        private fun calculateDelayUntilMidnight(): Long {
            // Vereinfachte Berechnung: In einer echten App würde man 
            // die Zeit bis exakt 00:00:01 Uhr berechnen.
            return TimeUnit.HOURS.toMillis(1) // Hier als Platzhalter
        }
    }
}
