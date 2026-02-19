package com.heckmannch.birthdaybuddy.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
     * Nutzt String-Ressourcen für die Formatierung.
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

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            // ID basiert auf dem Hashcode des Namens, um mehrere Benachrichtigungen am selben Tag zu ermöglichen
            notify(name.hashCode(), builder.build())
        }
    }

    private fun canShowNotifications(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
