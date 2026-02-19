package com.heckmannch.birthdaybuddy.utils

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.heckmannch.birthdaybuddy.MainActivity
import com.heckmannch.birthdaybuddy.R

/**
 * Hilfsklasse für die Erstellung und Anzeige von Benachrichtigungen.
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "birthday_channel"
    }

    /**
     * Zeigt eine Geburtstagsbenachrichtigung an.
     */
    fun showBirthdayNotification(name: String, age: Int, days: Int) {
        if (!canShowNotifications()) return

        val title = if (days == 0) {
            context.getString(R.string.notification_title_today)
        } else {
            context.getString(R.string.notification_title_days, days)
        }

        val text = if (days == 0) {
            context.getString(R.string.notification_text_today, name, age)
        } else {
            context.getString(R.string.notification_text_days, name, age)
        }

        // Intent zum Öffnen der App beim Klick auf die Benachrichtigung
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cake)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                // ID basiert auf dem Hashcode des Namens, um mehrere Benachrichtigungen am selben Tag zu ermöglichen
                notify(name.hashCode(), builder.build())
            }
        } catch (e: SecurityException) {
            // Falls Berechtigungen zur Laufzeit doch fehlen
        }
    }

    /**
     * Test-Funktion, um sofort eine Benachrichtigung zu triggern.
     */
    fun triggerTestNotification() {
        showBirthdayNotification("Test Kontakt", 25, 0)
    }

    private fun canShowNotifications(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
