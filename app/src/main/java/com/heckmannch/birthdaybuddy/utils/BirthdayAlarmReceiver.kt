package com.heckmannch.birthdaybuddy.utils

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Der BirthdayAlarmReceiver wird vom System aufgerufen, wenn ein geplanter Wecker (Alarm) abläuft.
 * Er ist dafür zuständig, die Kontakte zu prüfen und ggf. Benachrichtigungen anzuzeigen.
 */
class BirthdayAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // goAsync() teilt dem System mit, dass dieser Receiver noch im Hintergrund arbeitet,
        // auch wenn die onReceive-Methode technisch gesehen sofort fertig ist.
        val pendingResult = goAsync()
        val filterManager = FilterManager(context)

        // Startet eine Coroutine im IO-Thread, um Daten zu laden ohne die UI zu blockieren
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Berechtigungs-Check: Dürfen wir überhaupt Benachrichtigungen senden?
                // Ab Android 13 (Tiramisu) ist dafür eine explizite Laufzeit-Berechtigung nötig.
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ||
                    android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {

                    // Lädt die Benutzereinstellungen für die Benachrichtigungen
                    val daysToNotify = filterManager.notificationDaysFlow.first()
                    val excluded = filterManager.excludedLabelsFlow.first()

                    // 2. Kontakte aus dem Telefonbuch laden und filtern
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        val contacts = fetchBirthdays(context)

                        // Kontakte ignorieren, deren Labels in den Einstellungen blockiert wurden
                        val filteredContacts = contacts.filter { contact ->
                            !contact.labels.any { label -> excluded.contains(label) }
                        }

                        // 3. Prüfung: Steht heute oder in X Tagen ein Geburtstag an?
                        filteredContacts.forEach { contact ->
                            if (daysToNotify.contains(contact.remainingDays.toString())) {
                                showNotification(context, contact.name, contact.age, contact.remainingDays)
                            }
                        }
                    }
                }

                // 4. Den Wecker für den nächsten Tag (zur selben Uhrzeit) neu planen.
                // Da Alarme in Android nach dem Auslösen gelöscht werden, müssen wir sie manuell "wiederholen".
                val hour = filterManager.notificationHourFlow.first()
                val minute = filterManager.notificationMinuteFlow.first()
                scheduleDailyAlarm(context, hour, minute)

            } finally {
                // WICHTIG: Signalisiert dem System, dass die Hintergrundarbeit abgeschlossen ist.
                pendingResult.finish()
            }
        }
    }

    /**
     * Erstellt und zeigt eine System-Benachrichtigung für einen Geburtstag an.
     * 
     * @param name Name des Geburtstagskindes.
     * @param age Das Alter, das erreicht wird.
     * @param days In wie vielen Tagen der Geburtstag ist (0 = heute).
     */
    private fun showNotification(context: Context, name: String, age: Int, days: Int) {
        val title = if (days == 0) "Geburtstag heute! \uD83C\uDF82" else "Geburtstag in $days Tagen"
        val text = if (days == 0) "$name wird heute $age Jahre alt!" else "$name wird $age Jahre alt."

        val builder = NotificationCompat.Builder(context, "birthday_channel")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Benachrichtigung verschwindet beim Anklicken

        with(NotificationManagerCompat.from(context)) {
            // Nutzt den HashCode des Namens als ID, damit mehrere Geburtstage 
            // separate Benachrichtigungen erzeugen und sich nicht gegenseitig überschreiben.
            notify(name.hashCode(), builder.build())
        }
    }
}

/**
 * Plant einen exakten Alarm beim Android-System.
 * Dieser wird genutzt, um die App täglich zu einer bestimmten Zeit aufzuwecken.
 * 
 * @param hour Die Stunde (0-23).
 * @param minute Die Minute (0-59).
 */
fun scheduleDailyAlarm(context: Context, hour: Int, minute: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, BirthdayAlarmReceiver::class.java)

    // Ein PendingIntent ist eine "Vollmacht" für das System, den Receiver später auszuführen.
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Zeitpunkt für den Alarm berechnen
    val calendar = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }

    // Falls die gewählte Zeit für heute schon in der Vergangenheit liegt -> auf morgen verschieben.
    if (calendar.timeInMillis <= System.currentTimeMillis()) {
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    try {
        // setExactAndAllowWhileIdle stellt sicher, dass der Wecker auch dann klingelt,
        // wenn das Handy im Doze-Mode (Energiesparmodus) ist.
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    } catch (e: SecurityException) {
        // Kann passieren, wenn die App keine Berechtigung für "Exakte Alarme" hat (Android 12+).
    }
}
