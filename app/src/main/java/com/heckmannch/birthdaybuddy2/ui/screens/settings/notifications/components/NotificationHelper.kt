package com.heckmannch.birthdaybuddy2.ui.screens.settings.notifications.components

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.heckmannch.birthdaybuddy2.MainActivity
import com.heckmannch.birthdaybuddy2.R
import com.heckmannch.birthdaybuddy2.database.Contact
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "birthday_reminders"
    }

    fun showBirthdayNotification(contacts: List<Contact>, daysBefore: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        // Haupt-Intent: App öffnen
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntentId = 200 + daysBefore
        val pendingIntent = PendingIntent.getActivity(
            context, pendingIntentId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action-Intent: Später erinnern (Snooze)
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "SNOOZE"
            putExtra("NOTIFICATION_ID", pendingIntentId)
            putExtra("DAYS_BEFORE", daysBefore)
            putExtra("LOOKUP_KEYS", contacts.map { it.lookupKey }.toTypedArray())
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, pendingIntentId + 1000, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (daysBefore) {
            0 -> if (contacts.size == 1) {
                context.getString(R.string.notif_title_today_singular)
            } else {
                context.getString(R.string.notif_title_today_plural, contacts.size)
            }
            1 -> if (contacts.size == 1) {
                context.getString(R.string.notif_title_tomorrow_singular)
            } else {
                context.getString(R.string.notif_title_tomorrow_plural, contacts.size)
            }
            7 -> if (contacts.size == 1) {
                context.getString(R.string.notif_title_week_singular)
            } else {
                context.getString(R.string.notif_title_week_plural, contacts.size)
            }
            else -> if (contacts.size == 1) {
                context.resources.getQuantityString(R.plurals.notif_title_days_singular, daysBefore, daysBefore)
            } else {
                context.resources.getQuantityString(R.plurals.notif_title_days_plural, daysBefore, daysBefore, contacts.size)
            }
        }

        val contentText = contacts.joinToString(", ") { it.fullName }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .addAction(0, context.getString(R.string.notif_action_snooze), snoozePendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(pendingIntentId, notification)
    }
}
