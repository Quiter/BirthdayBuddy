package com.heckmannch.birthdaybuddy.utils

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Verantwortlich für das Management des Homescreen-Widgets.
 * 
 * OPTIMIERUNG: Die eigentliche Datenverarbeitung wurde in den [BirthdayWorker] ausgelagert.
 * Dadurch wird sichergestellt, dass das Widget akkuschonend und konsistent mit den 
 * Benachrichtigungen aktualisiert wird, ohne das System unnötig oft aufzuwecken.
 */
class BirthdayWidgetProvider : AppWidgetProvider() {

    /**
     * Wird aufgerufen, wenn das System oder ein Intervall eine Aktualisierung anfordert.
     * Statt die Kontakte hier direkt zu laden (was teuer ist), delegieren wir die Arbeit
     * an den WorkManager.
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val workRequest = OneTimeWorkRequestBuilder<BirthdayWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}

/**
 * Erzwingt eine sofortige Aktualisierung aller Widgets via WorkManager.
 * Wird aufgerufen, wenn sich Filter oder Einstellungen in der App ändern.
 */
fun updateWidget(context: Context) {
    val workRequest = OneTimeWorkRequestBuilder<BirthdayWorker>().build()
    WorkManager.getInstance(context).enqueue(workRequest)
}
