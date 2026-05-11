package com.heckmannch.birthdaybuddy2.ui.screens.settings.notifications.components

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val notificationId = intent.getIntExtra("NOTIFICATION_ID", -1)
        val daysBefore = intent.getIntExtra("DAYS_BEFORE", 0)
        val lookupKeys = intent.getStringArrayExtra("LOOKUP_KEYS") ?: emptyArray()

        if (intent.action == "SNOOZE") {
            // 1. Aktuelle Benachrichtigung schließen
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)

            // 2. Erneute Erinnerung in 2 Stunden planen
            val data = Data.Builder()
                .putInt("DAYS_BEFORE", daysBefore)
                .putStringArray("LOOKUP_KEYS", lookupKeys)
                .build()

            val snoozeRequest = OneTimeWorkRequestBuilder<SnoozeWorker>()
                .setInitialDelay(2, TimeUnit.HOURS)
                .setInputData(data)
                .addTag("notification_snooze")
                .build()

            WorkManager.getInstance(context).enqueue(snoozeRequest)
        }
    }
}
