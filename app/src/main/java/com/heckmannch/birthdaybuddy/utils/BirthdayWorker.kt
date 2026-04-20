package com.heckmannch.birthdaybuddy.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.heckmannch.birthdaybuddy.data.repository.BirthdayRepository
import com.heckmannch.birthdaybuddy.data.preferences.FilterManager
import com.heckmannch.birthdaybuddy.widget.BirthdayGlanceWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Der BirthdayWorker übernimmt die tägliche Synchronisierung und Benachrichtigung.
 * Er kann auch manuell für ein reines Widget-Update getriggert werden.
 */
@HiltWorker
class BirthdayWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: BirthdayRepository,
    private val filterManager: FilterManager,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    companion object {
        // Dieser Schlüssel steuert, ob der Worker Benachrichtigungen senden darf.
        const val KEY_AFFECTS_NOTIFICATIONS = "AFFECTS_NOTIFICATIONS"
    }

    override suspend fun doWork(): Result {
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure()
        }

        // 1. DB synchronisieren (immer notwendig)
        repository.refreshBirthdays()

        // 2. Benachrichtigungen prüfen (nur wenn erlaubt)
        val affectsNotifications = inputData.getBoolean(KEY_AFFECTS_NOTIFICATIONS, true)
        if (affectsNotifications) {
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
        }

        // 3. Glance Widget zur Aktualisierung zwingen (immer notwendig)
        BirthdayGlanceWidget().updateAll(applicationContext)
        
        // 4. Datum der letzten Aktualisierung speichern
        filterManager.saveLastWidgetUpdateDate(LocalDate.now().toString())

        return Result.success()
    }
}

fun scheduleDailyBirthdayWork(context: Context, hour: Int, minute: Int) {
    val workManager = WorkManager.getInstance(context)
    
    // Berechnung des initialen Delays, um zur Wunschuhrzeit zu starten
    val now = LocalDateTime.now()
    val targetTime = LocalTime.of(hour, minute)
    var targetDateTime = LocalDateTime.of(now.toLocalDate(), targetTime)

    // Falls die Uhrzeit heute schon vorbei ist, planen wir für morgen
    if (now.isAfter(targetDateTime)) {
        targetDateTime = targetDateTime.plusDays(1)
    }

    val initialDelay = Duration.between(now, targetDateTime)

    val constraints = Constraints.Builder()
        .build()

    // 24-Stunden-Intervall, damit die Benachrichtigung genau 1x am Tag kommt
    val workRequest = PeriodicWorkRequestBuilder<BirthdayWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
        .setConstraints(constraints)
        .addTag("birthday_daily_sync")
        .build()

    workManager.enqueueUniquePeriodicWork(
        "birthday_daily_sync",
        ExistingPeriodicWorkPolicy.UPDATE,
        workRequest
    )
}

fun triggerImmediateWidgetUpdate(context: Context) {
    val workRequest = OneTimeWorkRequestBuilder<BirthdayWorker>()
        .setInputData(workDataOf(BirthdayWorker.KEY_AFFECTS_NOTIFICATIONS to false))
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .addTag("immediate_widget_update")
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        "immediate_widget_update",
        ExistingWorkPolicy.REPLACE,
        workRequest
    )
}
