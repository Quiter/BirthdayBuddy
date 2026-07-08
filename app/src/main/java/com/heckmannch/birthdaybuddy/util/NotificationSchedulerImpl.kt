package com.heckmannch.birthdaybuddy.util

import android.content.Context
import com.heckmannch.birthdaybuddy.data.local.NotificationRule
import com.heckmannch.birthdaybuddy.notification.NotificationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.heckmannch.birthdaybuddy.notification.SnoozeWorker
import java.util.concurrent.TimeUnit

@Singleton
class NotificationSchedulerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NotificationScheduler {
    override fun scheduleNext(rules: List<NotificationRule>) {
        NotificationWorker.scheduleNext(context, rules)
    }

    override fun cancelNotification() {
        NotificationWorker.cancelNotification(context)
    }

    override fun snoozeNotification(pendingId: Int, daysBefore: Int, lookupKeys: List<String>) {
        val data = Data.Builder()
            .putInt("DAYS_BEFORE", daysBefore)
            .putInt("PENDING_ID", pendingId)
            .putStringArray("LOOKUP_KEYS", lookupKeys.toTypedArray())
            .build()

        val snoozeRequest = OneTimeWorkRequestBuilder<SnoozeWorker>()
            .setInitialDelay(2, TimeUnit.HOURS)
            .setInputData(data)
            .addTag("notification_snooze")
            .build()

        WorkManager.getInstance(context).enqueue(snoozeRequest)
    }
}
