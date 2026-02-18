package com.heckmannch.birthdaybuddy.utils

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy.MainActivity
import com.heckmannch.birthdaybuddy.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Verantwortlich für das Management des Homescreen-Widgets.
 * Aktualisiert die Anzeige der nächsten Geburtstage direkt auf dem Startbildschirm.
 */
class BirthdayWidgetProvider : AppWidgetProvider() {

    /**
     * Wird aufgerufen, wenn das Widget aktualisiert werden soll (entweder durch das System
     * im festgelegten Intervall oder manuell durch [updateWidget]).
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // goAsync ermöglicht Hintergrundarbeit in einem BroadcastReceiver
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // RemoteViews wird benötigt, um die UI eines anderen Prozesses (System-Launcher) zu steuern
                val views = RemoteViews(context.packageName, R.layout.widget_layout)

                // Prüfen, ob wir Zugriff auf die Kontakte haben
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    val filterManager = FilterManager(context)

                    // Laden der Filter-Einstellungen für das Widget
                    val selected = filterManager.widgetSelectedLabelsFlow.first()
                    val excluded = filterManager.widgetExcludedLabelsFlow.first()
                    val itemCount = filterManager.widgetItemCountFlow.first()

                    // Alle Geburtstage laden
                    val contacts = fetchBirthdays(context)

                    // Filterung der Kontakte speziell für das Widget
                    val nextBirthdays = contacts.filter { contact ->
                        // 1. Blacklist: Ist eines der Labels des Kontakts blockiert?
                        val isExcluded = contact.labels.any { excluded.contains(it) }
                        // 2. Whitelist: Ist eines der Labels explizit erlaubt? (Oder Whitelist ist leer = alle erlaubt)
                        val isSelected = selected.isEmpty() || contact.labels.any { selected.contains(it) }
                        !isExcluded && isSelected
                    }.sortedBy { it.remainingDays }.take(itemCount)

                    // Standardmäßig alle Zeilen im Widget verstecken
                    views.setViewVisibility(R.id.row_1, android.view.View.GONE)
                    views.setViewVisibility(R.id.row_2, android.view.View.GONE)
                    views.setViewVisibility(R.id.row_3, android.view.View.GONE)

                    if (nextBirthdays.isNotEmpty()) {
                        // Titel anpassen
                        views.setTextViewText(R.id.widget_title, if (nextBirthdays.size > 1) "Nächste Geburtstage" else "Nächster Geburtstag")

                        // Die gefilterten Kontakte in die Widget-Zeilen füllen
                        nextBirthdays.forEachIndexed { index, contact ->
                            val nameId = when(index) { 0 -> R.id.widget_name_1; 1 -> R.id.widget_name_2; else -> R.id.widget_name_3 }
                            val daysId = when(index) { 0 -> R.id.widget_days_1; 1 -> R.id.widget_days_2; else -> R.id.widget_days_3 }
                            val rowId = when(index) { 0 -> R.id.row_1; 1 -> R.id.row_2; else -> R.id.row_3 }

                            views.setViewVisibility(rowId, android.view.View.VISIBLE)
                            views.setTextViewText(nameId, contact.name)
                            
                            val daysText = when (contact.remainingDays) {
                                0 -> "Heute! \uD83C\uDF82"
                                1 -> "Morgen (wird ${contact.age})"
                                else -> "In ${contact.remainingDays} Tagen (wird ${contact.age})"
                            }
                            views.setTextViewText(daysId, daysText)
                        }
                    } else {
                        // Anzeige, wenn keine Geburtstage den Filterkriterien entsprechen
                        views.setViewVisibility(R.id.row_1, android.view.View.VISIBLE)
                        views.setTextViewText(R.id.widget_name_1, "Keine Geburtstage")
                        views.setTextViewText(R.id.widget_days_1, "Filter prüfen")
                    }
                } else {
                    // Fehlermeldung, wenn die Berechtigung fehlt
                    views.setViewVisibility(R.id.row_1, android.view.View.VISIBLE)
                    views.setTextViewText(R.id.widget_name_1, "App öffnen")
                    views.setTextViewText(R.id.widget_days_1, "Berechtigung fehlt")
                }

                // Klick auf das Widget öffnet die Haupt-App
                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                // Alle aktiven Instanzen des Widgets aktualisieren
                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, views)
                }
            } finally {
                // Signalisiert dem System den Abschluss der Hintergrundarbeit
                pendingResult.finish()
            }
        }
    }
}

/**
 * Hilfsfunktion, um eine sofortige Aktualisierung aller Widgets zu erzwingen.
 * Wird aufgerufen, wenn sich z.B. Filter-Einstellungen in der App ändern.
 */
fun updateWidget(context: Context) {
    val intent = Intent(context, BirthdayWidgetProvider::class.java).apply {
        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    }
    val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
        android.content.ComponentName(context, BirthdayWidgetProvider::class.java)
    )
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
    context.sendBroadcast(intent)
}
