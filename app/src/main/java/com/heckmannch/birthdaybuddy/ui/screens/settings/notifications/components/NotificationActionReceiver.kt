package com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.components

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val notificationId = intent.getIntExtra("NOTIFICATION_ID", -1)
        val pendingId = intent.getIntExtra("PENDING_ID", -1)
        val daysBefore = intent.getIntExtra("DAYS_BEFORE", 0)
        val lookupKeys = intent.getStringArrayExtra("LOOKUP_KEYS") ?: emptyArray()

        if (intent.action == "SNOOZE") {
            // 1. Aktuelle Benachrichtigung schließen
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)

            // 2. Erneute Erinnerung in 2 Stunden planen
            val data = Data.Builder()
                .putInt("DAYS_BEFORE", daysBefore)
                .putInt("PENDING_ID", pendingId)
                .putStringArray("LOOKUP_KEYS", lookupKeys)
                .build()

            val snoozeRequest = OneTimeWorkRequestBuilder<SnoozeWorker>()
                .setInitialDelay(2, TimeUnit.HOURS)
                .setInputData(data)
                .addTag("notification_snooze")
                .build()

            WorkManager.getInstance(context).enqueue(snoozeRequest)
        } else if (intent.action == "DONE") {
            // 1. Aktuelle Benachrichtigung schließen
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)

            // 2. Als erledigt markieren
            if (pendingId != -1) {
                CoroutineScope(Dispatchers.IO).launch {
                    notificationRepository.markAsDone(pendingId)
                }
            }
        } else if (intent.action == "DISMISSED") {
            // Wenn weggeschoben wurde, aber nicht erledigt/gesnoozed -> Sofort wieder anzeigen
            // (Das erzwingt die Persistenz auch auf Android 14+)
            if (pendingId != -1) {
                CoroutineScope(Dispatchers.IO).launch {
                    // Zähler für Wisch-Versuche erhöhen
                    notificationRepository.incrementDismissCount(pendingId)

                    val allContacts = notificationRepository.getActiveNotificationsImmediate()
                    val isStillActive = allContacts.any { it.id == pendingId }
                    if (isStillActive) {
                        // Wir brauchen den NotificationHelper. Da wir in einem Receiver sind,
                        // nutzen wir am besten den Worker oder wir triggern einen schnellen Re-show.
                        // Einfachster Weg: Snooze mit 1 Sekunde Delay oder direkter Aufruf.
                        val data = Data.Builder()
                            .putInt("DAYS_BEFORE", daysBefore)
                            .putInt("PENDING_ID", pendingId)
                            .putStringArray("LOOKUP_KEYS", lookupKeys)
                            .build()

                        val reShowRequest = OneTimeWorkRequestBuilder<SnoozeWorker>()
                            .setInitialDelay(500, TimeUnit.MILLISECONDS)
                            .setInputData(data)
                            .build()
                        WorkManager.getInstance(context).enqueue(reShowRequest)
                    }
                }
            }
        }
    }
}
