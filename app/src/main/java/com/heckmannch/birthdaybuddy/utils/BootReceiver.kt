package com.heckmannch.birthdaybuddy.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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
interface BootReceiverEntryPoint {
    fun filterManager(): FilterManager
}

/**
 * Reagiert auf Systemereignisse, um das Widget aktuell zu halten,
 * auch wenn die App nicht im Vordergrund läuft.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "System-Event empfangen: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                // Triggert ein sofortiges Widget-Update via WorkManager
                triggerImmediateWidgetUpdate(context)
            }
            Intent.ACTION_USER_PRESENT -> {
                // Beim Entsperren prüfen, ob heute schon aktualisiert wurde
                val entryPoint = EntryPointAccessors.fromApplication(context, BootReceiverEntryPoint::class.java)
                val filterManager = entryPoint.filterManager()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val prefs = filterManager.preferencesFlow.first()
                        val today = LocalDate.now().toString()

                        if (prefs.lastWidgetUpdateDate != today) {
                            Log.d("BootReceiver", "Erstes Entsperren heute ($today). Triggere Widget-Update.")
                            triggerImmediateWidgetUpdate(context)
                        }
                    } catch (e: Exception) {
                        Log.e("BootReceiver", "Fehler bei USER_PRESENT Update", e)
                    }
                }
            }
        }
    }
}
