package com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.components

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.heckmannch.birthdaybuddy.MainActivity
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.data.local.Contact
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.util.hasYear
import com.heckmannch.birthdaybuddy.util.safeNextAge
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
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

    suspend fun showBirthdayNotification(
        contacts: List<Contact>,
        daysBefore: Int,
        pendingId: Int = -1,
        eventType: String = "birthday"
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
            action = "DONE"
            putExtra("NOTIFICATION_ID", notificationId)
            putExtra("PENDING_ID", pendingId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 2000, doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dbKeys = contacts.map { contact ->
            when (eventType) {
                "anniversary" -> "anniversary:${contact.lookupKey}"
                "nameday" -> "nameday:${contact.lookupKey}"
                else -> contact.lookupKey
            }
        }.toTypedArray()

        // Action-Intent: Später erinnern (Snooze)
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "SNOOZE"
            putExtra("NOTIFICATION_ID", notificationId)
            putExtra("PENDING_ID", pendingId)
            putExtra("DAYS_BEFORE", daysBefore)
            putExtra("LOOKUP_KEYS", dbKeys)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 1000, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action-Intent: Einstellungen öffnen
        val settingsIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("NAVIGATE_TO_NOTIFICATIONS", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val settingsPendingIntent = PendingIntent.getActivity(
            context, notificationId + 4000, settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Delete-Intent: Falls weggeschoben wird -> Re-post (für echte Persistenz)
        val deleteIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "DISMISSED"
            putExtra("NOTIFICATION_ID", notificationId)
            putExtra("PENDING_ID", pendingId)
            putExtra("DAYS_BEFORE", daysBefore)
            putExtra("LOOKUP_KEYS", dbKeys)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 3000, deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isCoupleAnniversary = eventType == "anniversary" && contacts.size == 2 &&
                contacts[0].spouseLookupKey == contacts[1].lookupKey &&
                contacts[1].spouseLookupKey == contacts[0].lookupKey

        val title = if (contacts.size == 1 || isCoupleAnniversary) {
            val name = if (isCoupleAnniversary) {
                com.heckmannch.birthdaybuddy.util.mergeNames(contacts[0].fullName, contacts[1].fullName)
            } else {
                contacts.first().fullName
            }
            val contact = contacts.first()

            when (eventType) {
                "anniversary" -> {
                    val anniversary = contact.anniversary
                    val hasYear = anniversary?.hasYear ?: false
                    val nextYears = anniversary?.safeNextAge(LocalDate.now()) ?: -1

                    when (daysBefore) {
                        0 -> if (hasYear) context.getString(R.string.notif_title_today_anniversary_age, name, nextYears)
                             else context.getString(R.string.notif_title_today_anniversary, name)
                        1 -> if (hasYear) context.getString(R.string.notif_title_tomorrow_anniversary_age, name, nextYears)
                             else context.getString(R.string.notif_title_tomorrow_anniversary, name)
                        7 -> if (hasYear) context.getString(R.string.notif_title_week_anniversary_age, name, nextYears)
                             else context.getString(R.string.notif_title_week_anniversary, name)
                        else -> if (hasYear) context.getString(R.string.notif_title_days_anniversary_age, daysBefore, name, nextYears)
                                else context.getString(R.string.notif_title_days_anniversary, daysBefore, name)
                    }
                }
                "nameday" -> {
                    when (daysBefore) {
                        0 -> context.getString(R.string.notif_title_today_nameday, name)
                        1 -> context.getString(R.string.notif_title_tomorrow_nameday, name)
                        7 -> context.getString(R.string.notif_title_week_nameday, name)
                        else -> context.getString(R.string.notif_title_days_nameday, daysBefore, name)
                    }
                }
                else -> {
                    val birthday = contact.birthday
                    val hasYear = birthday?.hasYear ?: false
                    val nextAge = birthday?.safeNextAge(LocalDate.now()) ?: -1

                    when (daysBefore) {
                        0 -> if (hasYear) context.resources.getQuantityString(
                            R.plurals.notif_title_today_age,
                            nextAge,
                            name,
                            nextAge
                        )
                        else context.getString(R.string.notif_title_today_named, name)

                        1 -> if (hasYear) context.resources.getQuantityString(
                            R.plurals.notif_title_tomorrow_age,
                            nextAge,
                            name,
                            nextAge
                        )
                        else context.getString(R.string.notif_title_tomorrow_named, name)

                        7 -> if (hasYear) context.getString(R.string.notif_title_week_age, name, nextAge)
                        else context.getString(R.string.notif_title_week_named, name)

                        else -> if (hasYear) context.resources.getQuantityString(
                            R.plurals.notif_title_days_age,
                            daysBefore,
                            daysBefore,
                            name,
                            nextAge
                        )
                        else context.resources.getQuantityString(
                            R.plurals.notif_title_days_named,
                            daysBefore,
                            daysBefore,
                            name
                        )
                    }
                }
            }
        } else {
            when (eventType) {
                "anniversary" -> {
                    when (daysBefore) {
                        0 -> context.getString(R.string.notif_title_today_anniversary_plural, contacts.size)
                        1 -> context.getString(R.string.notif_title_tomorrow_anniversary_plural, contacts.size)
                        7 -> context.getString(R.string.notif_title_week_anniversary_plural, contacts.size)
                        else -> context.getString(R.string.notif_title_days_anniversary_plural, daysBefore, contacts.size)
                    }
                }
                "nameday" -> {
                    when (daysBefore) {
                        0 -> context.getString(R.string.notif_title_today_nameday_plural, contacts.size)
                        1 -> context.getString(R.string.notif_title_tomorrow_nameday_plural, contacts.size)
                        7 -> context.getString(R.string.notif_title_week_nameday_plural, contacts.size)
                        else -> context.getString(R.string.notif_title_days_nameday_plural, daysBefore, contacts.size)
                    }
                }
                else -> {
                    when (daysBefore) {
                        0 -> context.resources.getQuantityString(
                            R.plurals.notif_title_today_plural,
                            contacts.size,
                            contacts.size
                        )

                        1 -> context.resources.getQuantityString(
                            R.plurals.notif_title_tomorrow_plural,
                            contacts.size,
                            contacts.size
                        )

                        7 -> context.resources.getQuantityString(
                            R.plurals.notif_title_week_plural,
                            contacts.size,
                            contacts.size
                        )

                        else -> context.resources.getQuantityString(
                            R.plurals.notif_title_days_plural,
                            daysBefore,
                            daysBefore,
                            contacts.size
                        )
                    }
                }
            }
        }

        val contentText = if (contacts.size == 1 || isCoupleAnniversary) {
            val defaultDesc = when (eventType) {
                "anniversary" -> context.getString(R.string.notif_desc_anniversary)
                "nameday" -> context.getString(R.string.notif_desc_nameday)
                else -> context.getString(R.string.notif_desc_named)
            }
            if (showHint) context.getString(R.string.notif_hint_persistent)
            else defaultDesc
        } else {
            val list = contacts.joinToString(", ") { it.fullName }
            if (showHint) "${context.getString(R.string.notif_hint_persistent)} ($list)"
            else list
        }

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
