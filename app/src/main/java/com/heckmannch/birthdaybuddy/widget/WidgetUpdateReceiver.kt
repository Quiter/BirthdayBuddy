package com.heckmannch.birthdaybuddy.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.heckmannch.birthdaybuddy.data.FilterManager
import com.heckmannch.birthdaybuddy.utils.updateWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Dieser Receiver reagiert auf das Entsperren des Geräts (ACTION_USER_PRESENT).
 * Er stellt sicher, dass das Widget mindestens einmal am Tag aktualisiert wird,
 * sobald der Nutzer sein Handy das erste Mal benutzt.
 */
@AndroidEntryPoint
class WidgetUpdateReceiver : BroadcastReceiver() {

    @Inject
    lateinit var filterManager: FilterManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = filterManager.preferencesFlow.first()
                    val today = LocalDate.now().toString()
                    
                    // Nur aktualisieren, wenn heute noch kein Update stattgefunden hat
                    if (prefs.lastWidgetUpdateDate != today) {
                        updateWidget(context)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
