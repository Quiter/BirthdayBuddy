package com.heckmannch.birthdaybuddy.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.heckmannch.birthdaybuddy.di.ApplicationScope
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.util.AlarmScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by [android.app.AlarmManager] exact alarms to initiate birthday notification processing.
 *
 * Replaces WorkManager's delayed executions for time-critical alerts to eliminate Doze Mode delays.
 * When received:
 * 1. Enqueues [NotificationWorker] via [WorkManager] for immediate background execution (syncing contacts,
 *    evaluating upcoming events, and posting status bar notifications).
 * 2. Asynchronously reschedules the next notification alarm via [AlarmScheduler].
 *
 * Uses [EntryPointAccessors] instead of `@AndroidEntryPoint` to prevent component resolution crashes
 * during test runs or early system broadcasts before full Hilt runner initialization.
 */
class NotificationAlarmReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationAlarmReceiverEntryPoint {
        @ApplicationScope
        fun applicationScope(): CoroutineScope

        @IoDispatcher
        fun ioDispatcher(): CoroutineDispatcher

        fun alarmScheduler(): AlarmScheduler
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        Log.d(TAG, "Notification alarm received: enqueuing NotificationWorker and rescheduling next alarm")

        // 1. Enqueue NotificationWorker immediately to execute sync and notification logic
        NotificationWorker.enqueueImmediateWork(context)

        // 2. Reschedule the next alarm asynchronously
        val entryPoint = try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                NotificationAlarmReceiverEntryPoint::class.java,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve Hilt EntryPoint dependencies", e)
            return
        }

        val pendingResult = goAsync()
        entryPoint.applicationScope().launch(entryPoint.ioDispatcher()) {
            try {
                entryPoint.alarmScheduler().rescheduleNotificationAlarm()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule next notification alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "NotifAlarmReceiver"
    }
}
