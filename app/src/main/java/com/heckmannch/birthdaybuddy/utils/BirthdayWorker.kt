package com.heckmannch.birthdaybuddy.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.heckmannch.birthdaybuddy.data.BirthdayRepository
import com.heckmannch.birthdaybuddy.data.FilterManager
import com.heckmannch.birthdaybuddy.widget.BirthdayGlanceWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Der BirthdayWorker übernimmt die tägliche Synchronisierung und Benachrichtigung.
 * Dank @HiltWorker werden die Abhängigkeiten nun sauber injiziert.
 */
@HiltWorker
class BirthdayWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: BirthdayRepository,
    private val filterManager: FilterManager,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure()
        }

        // 1. DB synchronisieren
        repository.refreshBirthdays()

        // 2. Benachrichtigungen prüfen und ggf. anzeigen
        val allBirthdays = repository.allBirthdays.first()
        val prefs = filterManager.preferencesFlow.first()
        val daysToNotify = prefs.notificationDays
        
        val selectedLabels = prefs.notificationSelectedLabels
        val excludedLabels = prefs.notificationExcludedLabels
        val globalExcludedLabels = prefs.excludedLabels

        allBirthdays.filter { contact ->
            val isExcluded = contact.labels.any { excludedLabels.contains(it) || globalExcludedLabels.contains(it) }
            val isIncluded = if (selectedLabels.isEmpty()) true else contact.labels.any { selectedLabels.contains(it) }
            
            !isExcluded && isIncluded && daysToNotify.contains(contact.remainingDays.toString())
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
