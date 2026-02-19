package com.heckmannch.birthdaybuddy.utils

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.utils.BirthdayWidgetProvider
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Der BirthdayWorker übernimmt alle Hintergrundaufgaben der App.
 * Er ersetzt den alten AlarmManager-basierten Ansatz durch die modernere und 
 * akkuschonendere WorkManager API.
 * 
 * ZENTRALE VORTEILE:
 * 1. Einmaliges Laden der Kontakte für Benachrichtigungen UND Widget.
 * 2. Nutzt die intelligenten Doze-Mode Optimierungen von Android.
 * 3. Garantiert die Ausführung auch bei System-Einschränkungen.
 */
class BirthdayWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val filterManager = FilterManager(applicationContext)

        // 1. Berechtigungs-Check (Android 13+ erfordert explizite Notification-Erlaubnis)
        val canNotify = ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ||
                android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU

        // 2. Kontakte laden (Wichtig: Nur ein einziger Ladevorgang für alle Zwecke!)
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            val contacts = fetchBirthdays(applicationContext)

            // --- TEIL A: Tägliche Geburtstags-Benachrichtigungen ---
            if (canNotify) {
                val daysToNotify = filterManager.notificationDaysFlow.first()
                val excludedNotif = filterManager.excludedLabelsFlow.first()

                // Filtert nur die Kontakte, für die eine Benachrichtigung laut Einstellungen gewünscht ist
                val filteredNotifContacts = contacts.filter { contact ->
                    !contact.labels.any { label -> excludedNotif.contains(label) }
                }

                filteredNotifContacts.forEach { contact ->
                    if (daysToNotify.contains(contact.remainingDays.toString())) {
                        showNotification(applicationContext, contact.name, contact.age, contact.remainingDays)
                    }
                }
            }

            // --- TEIL B: Widget-Aktualisierung ---
            // Wir nutzen die bereits geladenen Kontakte, um das Widget zu zeichnen
            updateWidgetContent(applicationContext, filterManager, contacts)
        }

        // 3. Den nächsten Durchlauf für den folgenden Tag planen
        val hour = filterManager.notificationHourFlow.first()
        val minute = filterManager.notificationMinuteFlow.first()
        scheduleDailyBirthdayWork(applicationContext, hour, minute)

        return Result.success()
    }

    /**
     * Aktualisiert die RemoteViews des Homescreen-Widgets.
     */
    private suspend fun updateWidgetContent(context: Context, filterManager: FilterManager, contacts: List<BirthdayContact>) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, BirthdayWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        if (appWidgetIds.isEmpty()) return

        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        
        // Filter-Einstellungen für das Widget laden
        val selected = filterManager.widgetSelectedLabelsFlow.first()
        val excluded = filterManager.widgetExcludedLabelsFlow.first()
        val itemCount = filterManager.widgetItemCountFlow.first()

        // Nächste Geburtstage basierend auf Widget-Filtern ermitteln
        val nextBirthdays = contacts.filter { contact ->
            val isExcluded = contact.labels.any { excluded.contains(it) }
            val isSelected = selected.isEmpty() || contact.labels.any { selected.contains(it) }
            !isExcluded && isSelected
        }.sortedBy { it.remainingDays }.take(itemCount)

        // UI-Status des Widgets zurücksetzen
        views.setViewVisibility(R.id.row_1, View.GONE)
        views.setViewVisibility(R.id.row_2, View.GONE)
        views.setViewVisibility(R.id.row_3, View.GONE)

        if (nextBirthdays.isNotEmpty()) {
            views.setTextViewText(R.id.widget_title, if (nextBirthdays.size > 1) "Nächste Geburtstage" else "Nächster Geburtstag")

            nextBirthdays.forEachIndexed { index, contact ->
                val nameId = when(index) { 0 -> R.id.widget_name_1; 1 -> R.id.widget_name_2; else -> R.id.widget_name_3 }
                val daysId = when(index) { 0 -> R.id.widget_days_1; 1 -> R.id.widget_days_2; else -> R.id.widget_days_3 }
                val rowId = when(index) { 0 -> R.id.row_1; 1 -> R.id.row_2; else -> R.id.row_3 }

                views.setViewVisibility(rowId, View.VISIBLE)
                views.setTextViewText(nameId, contact.name)
                
                val daysText = when (contact.remainingDays) {
                    0 -> "Heute! \uD83C\uDF82"
                    1 -> "Morgen (wird ${contact.age})"
                    else -> "In ${contact.remainingDays} Tagen (wird ${contact.age})"
                }
                views.setTextViewText(daysId, daysText)
            }
        } else {
            views.setViewVisibility(R.id.row_1, View.VISIBLE)
            views.setTextViewText(R.id.widget_name_1, "Keine Geburtstage")
            views.setTextViewText(R.id.widget_days_1, "Filter prüfen")
        }

        // Alle Instanzen des Widgets informieren
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    /**
     * Erstellt eine System-Benachrichtigung für einen anstehenden Geburtstag.
     */
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

/**
 * Plant eine einmalige Ausführung des BirthdayWorkers zu einer bestimmten Zeit.
 * Der Worker plant sich am Ende seiner Ausführung selbst für den nächsten Tag neu.
 */
fun scheduleDailyBirthdayWork(context: Context, hour: Int, minute: Int) {
    val workManager = WorkManager.getInstance(context)

    // Zielzeitpunkt berechnen (heute oder morgen)
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }

    var delay = calendar.timeInMillis - System.currentTimeMillis()
    if (delay <= 0) {
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        delay = calendar.timeInMillis - System.currentTimeMillis()
    }

    val workRequest = OneTimeWorkRequestBuilder<BirthdayWorker>()
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .addTag("birthday_check")
        .build()

    // Einzigartige Arbeit einreihen (REPLACE verhindert Duplikate)
    workManager.enqueueUniqueWork(
        "birthday_daily_check",
        ExistingWorkPolicy.REPLACE,
        workRequest
    )
}
