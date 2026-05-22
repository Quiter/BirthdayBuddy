package com.heckmannch.birthdaybuddy.util

import android.content.Context
import com.heckmannch.birthdaybuddy.data.local.NotificationRule
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.components.NotificationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotificationScheduler {
    override fun scheduleNext(rules: List<NotificationRule>) {
        NotificationWorker.scheduleNext(context, rules)
    }

    override fun cancelNotification() {
        NotificationWorker.cancelNotification(context)
    }
}
