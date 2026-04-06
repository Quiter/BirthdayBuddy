package com.heckmannch.birthdaybuddy.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.heckmannch.birthdaybuddy.data.preferences.FilterManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ScreenOnReceiverEntryPoint {
    fun filterManager(): FilterManager
}

/**
 * Reagiert auf das Einschalten des Displays, um das Widget beim ersten Mal am Tag zu aktualisieren.
 */
class ScreenOnReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_ON) {
            val entryPoint = EntryPointAccessors.fromApplication(context, ScreenOnReceiverEntryPoint::class.java)
            val filterManager = entryPoint.filterManager()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = filterManager.preferencesFlow.first()
                    val today = LocalDate.now().toString()

                    if (prefs.lastWidgetUpdateDate != today) {
                        Log.d("ScreenOnReceiver", "Erstes Aufwecken heute. Triggere Widget-Update.")
                        
                        // Einmaliges Update via WorkManager triggern
                        triggerOneTimeWidgetUpdate(context)
                    }
                } catch (e: Exception) {
                    Log.e("ScreenOnReceiver", "Fehler beim Prüfen des Widget-Updates", e)
                }
            }
        }
    }

    private fun triggerOneTimeWidgetUpdate(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<BirthdayWorker>()
            .setInputData(workDataOf(BirthdayWorker.KEY_AFFECTS_NOTIFICATIONS to false))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("widget_update_on_wake")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "widget_update_on_wake",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
