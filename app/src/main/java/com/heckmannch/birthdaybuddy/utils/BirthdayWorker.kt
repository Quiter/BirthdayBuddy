package com.heckmannch.birthdaybuddy.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.heckmannch.birthdaybuddy.data.BirthdayRepository
import kotlinx.coroutines.flow.first
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Der BirthdayWorker übernimmt die tägliche Synchronisierung und Benachrichtigung.
 * Er nutzt das Repository, um die Datenbank aktuell zu halten.
 */
class BirthdayWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val repository = BirthdayRepository(applicationContext)
        val filterManager = FilterManager(applicationContext)

        // 1. Berechtigungen prüfen
        val hasReadContacts = ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (!hasReadContacts) return Result.failure()

        // 2. Datenbank im Hintergrund synchronisieren
        // Das sorgt dafür, dass auch das Widget und die App immer aktuell sind
        repository.refreshBirthdays()

        // 3. Benachrichtigungen prüfen
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

        // 4. Widget-Update anstoßen (nutzt die frische DB)
        updateWidget(applicationContext)

        return Result.success()
    }

    private fun showNotification(context: Context, name: String, age: Int, days: Int) {
        val title = if (days == 0) "Geburtstag heute! \uD83C\uDF82" else "Geburtstag in $days Tagen"
        val text = if (days == 0) "$name wird heute $age Jahre alt!" else "$name wird $age Jahre alt."

        val builder = NotificationCompat.Builder(context, "birthday_channel")
            .setSmallIcon(android.R.drawable.ic_popup_reminder) // Ersetze dies ggf. durch dein App-Icon
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

/**
 * Plant die tägliche Prüfung. Wir nutzen PeriodicWork für maximale Zuverlässigkeit.
 */
fun scheduleDailyBirthdayWork(context: Context, hour: Int, minute: Int) {
    val workManager = WorkManager.getInstance(context)

    // Constraints: Nur ausführen, wenn der Akku nicht fast leer ist
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        .setRequiresBatteryNotLow(true)
        .build()

    // Wir nutzen ein tägliches Intervall
    val workRequest = PeriodicWorkRequestBuilder<BirthdayWorker>(24, TimeUnit.HOURS)
        .setConstraints(constraints)
        .addTag("birthday_daily_sync")
        .build()

    // UNIQUE_PERIODIC_WORK sorgt dafür, dass wir nicht mehrere Worker gleichzeitig haben
    workManager.enqueueUniquePeriodicWork(
        "birthday_daily_sync",
        ExistingPeriodicWorkPolicy.UPDATE, // UPDATE statt REPLACE, um Verzögerungen zu minimieren
        workRequest
    )
}
