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
        // goAsync erlaubt uns das Auslesen der Datenbank im Hintergrund
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Wir holen uns das Layout, das wir gerade in XML gebaut haben
                val views = RemoteViews(context.packageName, R.layout.widget_layout)

                // Dürfen wir Kontakte lesen?
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    val filterManager = FilterManager(context)
                    val excluded = filterManager.excludedLabelsFlow.first()

                    val contacts = fetchBirthdays(context)

                    // Wir filtern geblockte Labels raus und schnappen uns den, der als erstes dran ist!
                    val nextBirthday = contacts.filter { contact ->
                        !contact.labels.any { label -> excluded.contains(label) }
                    }.minByOrNull { it.remainingDays }

                    if (nextBirthday != null) {
                        views.setTextViewText(R.id.widget_name, nextBirthday.name)

                        val daysText = when (nextBirthday.remainingDays) {
                            0 -> "Heute! \uD83C\uDF82"
                            1 -> "Morgen (wird ${nextBirthday.age})"
                            else -> "In ${nextBirthday.remainingDays} Tagen (wird ${nextBirthday.age})"
                        }
                        views.setTextViewText(R.id.widget_days, daysText)
                    } else {
                        views.setTextViewText(R.id.widget_name, "Keine Geburtstage")
                        views.setTextViewText(R.id.widget_days, "")
                    }
                } else {
                    views.setTextViewText(R.id.widget_name, "App öffnen")
                    views.setTextViewText(R.id.widget_days, "Berechtigung fehlt")
                }

                // Wenn man das Widget antippt, soll sich die App öffnen
                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                // Das fertige Bild an den Android-Homescreen schicken
                appWidgetManager.updateAppWidget(appWidgetIds, views)
            } finally {
                pendingResult.finish()
            }
        }
    }
}