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
import com.heckmannch.birthdaybuddy2.repository.NotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val notificationRepository: NotificationRepository
) {

    companion object {
        const val CHANNEL_ID = "birthday_reminders_v2"
    }

    fun showBirthdayNotification(contacts: List<Contact>, daysBefore: Int, pendingId: Int = -1) {
        val settings = runBlocking { notificationRepository.settings.first() }
        val isPersistent = settings.persistentNotifications

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH // Höhere Wichtigkeit für persistente Erinnnerungen
        )
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        // Wir nutzen die Datenbank-ID (pendingId) als eindeutige System-Notification-ID
        // Falls keine pendingId da ist (Snooze-Fallback), nutzen wir den Standard-Algorithmus
        val notificationId = if (pendingId != -1) pendingId else (200 + daysBefore)

        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action-Intent: Erledigt
        val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "DONE"
            putExtra("NOTIFICATION_ID", notificationId)
            putExtra("PENDING_ID", pendingId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 2000, doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action-Intent: Später erinnern (Snooze)
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "SNOOZE"
            putExtra("NOTIFICATION_ID", notificationId)
            putExtra("PENDING_ID", pendingId)
            putExtra("DAYS_BEFORE", daysBefore)
            putExtra("LOOKUP_KEYS", contacts.map { it.lookupKey }.toTypedArray())
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 1000, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Delete-Intent: Falls weggeschoben wird -> Re-post (für echte Persistenz)
        val deleteIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "DISMISSED"
            putExtra("NOTIFICATION_ID", notificationId)
            putExtra("PENDING_ID", pendingId)
            putExtra("DAYS_BEFORE", daysBefore)
            putExtra("LOOKUP_KEYS", contacts.map { it.lookupKey }.toTypedArray())
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 3000, deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (contacts.size == 1) {
            val name = contacts.first().fullName
            when (daysBefore) {
                0 -> context.getString(R.string.notif_title_today_named, name)
                1 -> context.getString(R.string.notif_title_tomorrow_named, name)
                7 -> context.getString(R.string.notif_title_week_named, name)
                else -> context.resources.getQuantityString(R.plurals.notif_title_days_named, daysBefore, daysBefore, name)
            }
        } else {
            when (daysBefore) {
                0 -> context.getString(R.string.notif_title_today_plural, contacts.size)
                1 -> context.getString(R.string.notif_title_tomorrow_plural, contacts.size)
                7 -> context.getString(R.string.notif_title_week_plural, contacts.size)
                else -> context.resources.getQuantityString(R.plurals.notif_title_days_plural, daysBefore, daysBefore, contacts.size)
            }
        }

        val contentText = if (contacts.size == 1) {
            context.getString(R.string.notif_desc_named)
        } else {
            contacts.joinToString(", ") { it.fullName }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOngoing(isPersistent) // Bedingt persistent!
            .apply {
                if (isPersistent) {
                    setDeleteIntent(deletePendingIntent)
                    addAction(0, context.getString(R.string.notif_action_done), donePendingIntent)
                }
            }
            .addAction(0, context.getString(R.string.notif_action_snooze), snoozePendingIntent)
            .setAutoCancel(!isPersistent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
