package com.heckmannch.birthdaybuddy.data.repository

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.repository.NotificationScheduler
import com.heckmannch.birthdaybuddy.notification.NotificationActions
import com.heckmannch.birthdaybuddy.notification.NotificationWorker
import com.heckmannch.birthdaybuddy.notification.SnoozeWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

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
        val firstKey = lookupKeys.firstOrNull() ?: ""
        val eventType = when {
            firstKey.startsWith("anniversary:") -> EventType.ANNIVERSARY
            firstKey.startsWith("nameday:") -> EventType.NAME_DAY
            else -> EventType.BIRTHDAY
        }
        val data = Data.Builder()
            .putInt(NotificationActions.EXTRA_DAYS_BEFORE, daysBefore)
            .putInt(NotificationActions.EXTRA_PENDING_ID, pendingId)
            .putStringArray(NotificationActions.EXTRA_LOOKUP_KEYS, lookupKeys.toTypedArray())
            .putString(NotificationActions.EXTRA_EVENT_TYPE, eventType.name)
            .build()

        val snoozeRequest = OneTimeWorkRequestBuilder<SnoozeWorker>()
            .setInitialDelay(2, TimeUnit.HOURS)
            .setInputData(data)
            .addTag(NotificationActions.WORK_TAG_SNOOZE)
            .build()

        WorkManager.getInstance(context).enqueue(snoozeRequest)
    }

    override fun reshowNotification(
        pendingId: Int,
        daysBefore: Int,
        lookupKeys: List<String>,
        eventType: EventType,
        delayMillis: Long
    ) {
        val data = Data.Builder()
            .putInt(NotificationActions.EXTRA_DAYS_BEFORE, daysBefore)
            .putInt(NotificationActions.EXTRA_PENDING_ID, pendingId)
            .putStringArray(NotificationActions.EXTRA_LOOKUP_KEYS, lookupKeys.toTypedArray())
            .putString(NotificationActions.EXTRA_EVENT_TYPE, eventType.name)
            .build()

        val reShowRequest = OneTimeWorkRequestBuilder<SnoozeWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(reShowRequest)
    }
}
