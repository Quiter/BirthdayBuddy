package com.heckmannch.birthdaybuddy.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.heckmannch.birthdaybuddy.MainActivity
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.util.NotificationKeyUtils
import com.heckmannch.birthdaybuddy.util.IntentExtras
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val notificationRepository: NotificationRepository,
    private val notificationTextFormatter: NotificationTextFormatter
) {

    companion object {
        const val CHANNEL_ID = NotificationActions.CHANNEL_ID
    }

    suspend fun showBirthdayNotification(
        contacts: List<Contact>,
        daysBefore: Int,
        pendingId: Int = -1,
        eventType: EventType = EventType.BIRTHDAY
    ) {
        val settings = notificationRepository.settings.first()
        val isPersistent = settings.persistentNotifications

        // Wisch-Zähler prüfen für Hilfetext
        val pendingNotification =
            if (pendingId != -1) notificationRepository.getPendingNotificationById(pendingId) else null
        val dismissCount = pendingNotification?.dismissCount ?: 0
        val showHint = isPersistent && dismissCount >= 3

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
            action = NotificationActions.ACTION_DONE
            putExtra(NotificationActions.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationActions.EXTRA_PENDING_ID, pendingId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context, notificationId * 10 + 2, doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dbKeys = contacts.map { contact ->
            NotificationKeyUtils.encodeKey(contact.lookupKey, eventType)
        }.toTypedArray()

        // Action-Intent: Später erinnern (Snooze)
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActions.ACTION_SNOOZE
            putExtra(NotificationActions.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationActions.EXTRA_PENDING_ID, pendingId)
            putExtra(NotificationActions.EXTRA_DAYS_BEFORE, daysBefore)
            putExtra(NotificationActions.EXTRA_LOOKUP_KEYS, dbKeys)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, notificationId * 10 + 1, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action-Intent: Einstellungen öffnen
        val settingsIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val settingsPendingIntent = PendingIntent.getActivity(
            context, notificationId * 10 + 4, settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Delete-Intent: Falls weggeschoben wird -> Re-post (für echte Persistenz)
        val deleteIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActions.ACTION_DISMISSED
            putExtra(NotificationActions.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationActions.EXTRA_PENDING_ID, pendingId)
            putExtra(NotificationActions.EXTRA_DAYS_BEFORE, daysBefore)
            putExtra(NotificationActions.EXTRA_LOOKUP_KEYS, dbKeys)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context, notificationId * 10 + 3, deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = notificationTextFormatter.buildTitle(
            contacts = contacts,
            daysBefore = daysBefore,
            eventType = eventType
        )

        val contentText = notificationTextFormatter.buildContentText(
            contacts = contacts,
            eventType = eventType,
            showHint = showHint
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
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
                if (showHint) {
                    addAction(
                        0,
                        context.getString(R.string.notif_action_settings),
                        settingsPendingIntent
                    )
                }
            }
            .addAction(0, context.getString(R.string.notif_action_snooze), snoozePendingIntent)
            .setAutoCancel(!isPersistent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
