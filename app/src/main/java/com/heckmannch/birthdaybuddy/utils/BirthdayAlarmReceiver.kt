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

class BirthdayAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // goAsync erlaubt es uns, im Hintergrund auf die Datenbank zuzugreifen
        val pendingResult = goAsync()
        val filterManager = FilterManager(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Dürfen wir Benachrichtigungen senden?
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ||
                    android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {

                    val daysToNotify = filterManager.notificationDaysFlow.first()
                    val excluded = filterManager.excludedLabelsFlow.first()

                    // 2. Kontakte holen und filtern
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        val contacts = fetchBirthdays(context)

                        val filteredContacts = contacts.filter { contact ->
                            !contact.labels.any { label -> excluded.contains(label) }
                        }

                        // 3. Wenn die Resttage passen -> Benachrichtigung feuern!
                        filteredContacts.forEach { contact ->
                            if (daysToNotify.contains(contact.remainingDays.toString())) {
                                showNotification(context, contact.name, contact.age, contact.remainingDays)
                            }
                        }
                    }
                }

                // 4. Den Wecker automatisch für morgen zur selben Zeit neu stellen
                val hour = filterManager.notificationHourFlow.first()
                val minute = filterManager.notificationMinuteFlow.first()
                scheduleDailyAlarm(context, hour, minute)

            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, name: String, age: Int, days: Int) {
        val title = if (days == 0) "Geburtstag heute! \uD83C\uDF82" else "Geburtstag in $days Tagen"
        val text = if (days == 0) "$name wird heute $age Jahre alt!" else "$name wird $age Jahre alt."

        val builder = NotificationCompat.Builder(context, "birthday_channel")
            .setSmallIcon(android.R.drawable.ic_popup_reminder) // Nutzt ein Android-Standard-Icon
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            // Eindeutige ID generieren, damit nicht eine Nachricht die andere überschreibt
            notify(name.hashCode(), builder.build())
        }
    }
}

// Hilfsfunktion: Stellt den exakten Android-Wecker
fun scheduleDailyAlarm(context: Context, hour: Int, minute: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, BirthdayAlarmReceiver::class.java)

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }

    // Wenn die Uhrzeit für heute schon vorbei ist, stelle den Wecker auf morgen
    if (calendar.timeInMillis <= System.currentTimeMillis()) {
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    try {
        // Weckt das Handy auch im Standby-Modus exakt auf
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    } catch (e: SecurityException) {
        // Falls die Berechtigung für Exact Alarms fehlt, fangen wir das hier ab
    }
}