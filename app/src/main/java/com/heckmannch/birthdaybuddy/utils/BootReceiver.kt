package com.heckmannch.birthdaybuddy.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

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
            Intent.ACTION_TIMEZONE_CHANGED -> {
                // Triggert ein sofortiges Widget-Update via WorkManager
                triggerImmediateWidgetUpdate(context)
            }
        }
    }
}
