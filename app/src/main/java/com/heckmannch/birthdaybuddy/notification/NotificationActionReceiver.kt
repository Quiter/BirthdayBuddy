package com.heckmannch.birthdaybuddy.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.heckmannch.birthdaybuddy.di.ApplicationScope
import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.usecase.SnoozeNotificationUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var snoozeNotificationUseCase: SnoozeNotificationUseCase

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val notificationId = intent.getIntExtra("NOTIFICATION_ID", -1)
        val pendingId = intent.getIntExtra("PENDING_ID", -1)
        val daysBefore = intent.getIntExtra("DAYS_BEFORE", 0)
        val lookupKeys = intent.getStringArrayExtra("LOOKUP_KEYS") ?: emptyArray()

        when (intent.action) {
            NotificationActions.ACTION_SNOOZE -> {
                // 1. Aktuelle Benachrichtigung schließen
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)

                // 2. Erneute Erinnerung in 2 Stunden planen (via Use Case)
                snoozeNotificationUseCase(
                    pendingId = pendingId,
                    daysBefore = daysBefore,
                    lookupKeys = lookupKeys.toList()
                )
            }
            NotificationActions.ACTION_DONE -> {
                // 1. Dismiss the current notification
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)

                // 2. Mark as done using goAsync() to prevent process termination before database write completes
                if (pendingId != -1) {
                    val pendingResult = goAsync()
                    applicationScope.launch {
                        try {
                            notificationRepository.markAsDone(pendingId)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
            NotificationActions.ACTION_DISMISSED -> {
                // Wenn weggeschoben wurde, aber nicht erledigt/gesnoozed -> Sofort wieder anzeigen
                // (Das erzwingt die Persistenz auch auf Android 14+)
                if (pendingId != -1) {
                    val pendingResult = goAsync()
                    applicationScope.launch {
                        try {
                            // Zähler für Wisch-Versuche erhöhen
                            notificationRepository.incrementDismissCount(pendingId)

                            val allContacts = notificationRepository.getActiveNotificationsImmediate()
                            val isStillActive = allContacts.any { it.id == pendingId }
                            if (isStillActive) {
                                // Wir brauchen den NotificationHelper. Da wir in einem Receiver sind,
                                // nutzen wir am besten den Worker oder wir triggern einen schnellen Re-show.
                                // Einfachster Weg: Snooze mit 1 Sekunde Delay oder direkter Aufruf.
                                val firstKey = lookupKeys.firstOrNull() ?: ""
                                val eventType = when {
                                    firstKey.startsWith("anniversary:") -> EventType.ANNIVERSARY
                                    firstKey.startsWith("nameday:") -> EventType.NAME_DAY
                                    else -> EventType.BIRTHDAY
                                }
                                val data = Data.Builder()
                                    .putInt("DAYS_BEFORE", daysBefore)
                                    .putInt("PENDING_ID", pendingId)
                                    .putStringArray("LOOKUP_KEYS", lookupKeys)
                                    .putString("EVENT_TYPE", eventType.name)
                                    .build()

                                val reShowRequest = OneTimeWorkRequestBuilder<SnoozeWorker>()
                                    .setInitialDelay(500, TimeUnit.MILLISECONDS)
                                    .setInputData(data)
                                    .build()
                                WorkManager.getInstance(context).enqueue(reShowRequest)
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }
}
