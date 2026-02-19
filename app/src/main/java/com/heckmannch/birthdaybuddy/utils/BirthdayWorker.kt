package com.heckmannch.birthdaybuddy.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.glance.appwidget.updateAll
import androidx.work.*
import com.heckmannch.birthdaybuddy.data.BirthdayRepository
import com.heckmannch.birthdaybuddy.data.FilterManager
import com.heckmannch.birthdaybuddy.widget.BirthdayGlanceWidget
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Der BirthdayWorker übernimmt die tägliche Synchronisierung und Benachrichtigung.
 */
class BirthdayWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val repository = BirthdayRepository(applicationContext)
        val filterManager = FilterManager(applicationContext)
        val notificationHelper = NotificationHelper(applicationContext)

        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure()
        }

        // 1. DB synchronisieren
        repository.refreshBirthdays()

        // 2. Benachrichtigungen prüfen und ggf. anzeigen
        val allBirthdays = repository.allBirthdays.first()
        val daysToNotify = filterManager.notificationDaysFlow.first()
        val excludedLabels = filterManager.excludedLabelsFlow.first()

        allBirthdays.filter { contact ->
            !contact.labels.any { excludedLabels.contains(it) } &&
            daysToNotify.contains(contact.remainingDays.toString())
        }.forEach { contact ->
            notificationHelper.showBirthdayNotification(
                name = contact.name, 
                age = contact.age, 
                days = contact.remainingDays
            )
        }

        // 3. Glance Widget zur Aktualisierung zwingen
        BirthdayGlanceWidget().updateAll(applicationContext)

        return Result.success()
    }
}

fun scheduleDailyBirthdayWork(context: Context, hour: Int, minute: Int) {
    val workManager = WorkManager.getInstance(context)
    val constraints = Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .build()

    val workRequest = PeriodicWorkRequestBuilder<BirthdayWorker>(24, TimeUnit.HOURS)
        .setConstraints(constraints)
        .addTag("birthday_daily_sync")
        .build()

    workManager.enqueueUniquePeriodicWork(
        "birthday_daily_sync",
        ExistingPeriodicWorkPolicy.UPDATE,
        workRequest
    )
}
