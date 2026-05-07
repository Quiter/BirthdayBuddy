package com.heckmannch.birthdaybuddy2.ui.screens.settings.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.heckmannch.birthdaybuddy2.MainActivity
import com.heckmannch.birthdaybuddy2.R
import com.heckmannch.birthdaybuddy2.database.Contact

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "birthday_reminders"
        const val CHANNEL_NAME = "Geburtstags-Erinnerungen"
    }

    fun showBirthdayNotification(contacts: List<Contact>) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (contacts.size == 1) {
            "Heute hat jemand Geburtstag!"
        } else {
            "Heute haben ${contacts.size} Personen Geburtstag!"
        }

        val contentText = contacts.joinToString(", ") { it.fullName }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) 
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
