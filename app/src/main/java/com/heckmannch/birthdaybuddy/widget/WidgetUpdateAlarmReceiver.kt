package com.heckmannch.birthdaybuddy.widget

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
 * BroadcastReceiver triggered by [android.app.AlarmManager] exact alarms for midnight widget updates.
 *
 * Ensures accurate date rollover on the homescreen widget at 00:01 AM without being delayed by Doze Mode.
 * When received:
 * 1. Enqueues [BirthdayWidgetWorker] via [WorkManager] for immediate background execution to refresh Glance widgets.
 * 2. Asynchronously reschedules the next midnight update alarm via [AlarmScheduler].
 *
 * Uses [EntryPointAccessors] instead of `@AndroidEntryPoint` to prevent component resolution crashes
 * during test runs or early system broadcasts before full Hilt runner initialization.
 */
class WidgetUpdateAlarmReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetUpdateAlarmReceiverEntryPoint {
        @ApplicationScope
        fun applicationScope(): CoroutineScope

        @IoDispatcher
        fun ioDispatcher(): CoroutineDispatcher

        fun alarmScheduler(): AlarmScheduler
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        Log.d(TAG, "Widget update alarm received: enqueuing BirthdayWidgetWorker and rescheduling next midnight alarm")

        // 1. Enqueue BirthdayWidgetWorker immediately to execute widget update logic
        BirthdayWidgetWorker.enqueueImmediateWork(context)

        // 2. Reschedule next midnight widget update alarm
        val entryPoint = try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetUpdateAlarmReceiverEntryPoint::class.java,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve Hilt EntryPoint dependencies", e)
            return
        }

        val pendingResult = goAsync()
        entryPoint.applicationScope().launch(entryPoint.ioDispatcher()) {
            try {
                entryPoint.alarmScheduler().scheduleNextWidgetUpdateAlarm()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule next widget update alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "WidgetUpdateAlarmRecv"
    }
}
