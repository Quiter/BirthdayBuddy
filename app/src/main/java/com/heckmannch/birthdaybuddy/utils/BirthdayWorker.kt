package com.heckmannch.birthdaybuddy.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.glance.appwidget.updateAll
import androidx.work.*
import com.heckmannch.birthdaybuddy.data.BirthdayRepository
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

        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure()
        }

        // 1. DB synchronisieren
        repository.refreshBirthdays()

        // 2. Benachrichtigungen
        val canNotify = ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ||
                android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU

        if (canNotify) {
            val allBirthdays = repository.allBirthdays.first()
            val daysToNotify = filterManager.notificationDaysFlow.first()
            val excludedLabels = filterManager.excludedLabelsFlow.first()

            allBirthdays.filter { contact ->
                !contact.labels.any { excludedLabels.contains(it) } &&
                daysToNotify.contains(contact.remainingDays.toString())
            }.forEach { contact ->
                showNotification(applicationContext, contact.name, contact.age, contact.remainingDays)
            }
        }

        // 3. Glance Widget zur Aktualisierung zwingen
        BirthdayGlanceWidget().updateAll(applicationContext)

        return Result.success()
    }

    private fun showNotification(context: Context, name: String, age: Int, days: Int) {
        val title = if (days == 0) "Geburtstag heute! \uD83C\uDF82" else "Geburtstag in $days Tagen"
        val text = if (days == 0) "$name wird heute $age Jahre alt!" else "$name wird $age Jahre alt."

        val builder = NotificationCompat.Builder(context, "birthday_channel")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ||
                android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                notify(name.hashCode(), builder.build())
            }
        }
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
