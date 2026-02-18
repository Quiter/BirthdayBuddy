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

class BirthdayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_layout)

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    val filterManager = FilterManager(context)

                    // Korrektur: Wir nutzen jetzt die Blacklist-Flows
                    val hidden = filterManager.widgetHiddenLabelsFlow.first()
                    val excluded = filterManager.widgetExcludedLabelsFlow.first()
                    val itemCount = filterManager.widgetItemCountFlow.first()

                    val contacts = fetchBirthdays(context)

                    // Korrektur: Filterung nach Blacklist-Logik
                    val nextBirthdays = contacts.filter { contact ->
                        val isHidden = contact.labels.any { hidden.contains(it) }
                        val isExcluded = contact.labels.any { excluded.contains(it) }
                        !isHidden && !isExcluded
                    }.sortedBy { it.remainingDays }.take(itemCount)

                    // UI zurücksetzen
                    views.setViewVisibility(R.id.row_1, android.view.View.GONE)
                    views.setViewVisibility(R.id.row_2, android.view.View.GONE)
                    views.setViewVisibility(R.id.row_3, android.view.View.GONE)

                    if (nextBirthdays.isNotEmpty()) {
                        views.setTextViewText(R.id.widget_title, if (nextBirthdays.size > 1) "Nächste Geburtstage" else "Nächster Geburtstag")

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
                        views.setViewVisibility(R.id.row_1, android.view.View.VISIBLE)
                        views.setTextViewText(R.id.widget_name_1, "Keine Geburtstage")
                        views.setTextViewText(R.id.widget_days_1, "Filter prüfen")
                    }
                } else {
                    views.setViewVisibility(R.id.row_1, android.view.View.VISIBLE)
                    views.setTextViewText(R.id.widget_name_1, "App öffnen")
                    views.setTextViewText(R.id.widget_days_1, "Berechtigung fehlt")
                }

                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

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
