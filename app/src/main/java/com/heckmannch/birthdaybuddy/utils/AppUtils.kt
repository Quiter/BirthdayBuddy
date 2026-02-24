package com.heckmannch.birthdaybuddy.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Hilfsfunktionen für allgemeine App-Aufgaben wie Versionsabfrage und Widget-Updates.
 */

/**
 * Holt die Versionsnummer der App (z.B. "1.0.5") aus dem System.
 * Diese wird in den Einstellungen angezeigt.
 */
@Composable
fun getAppVersionName(context: Context = LocalContext.current): String {
    return remember {
        try {
            // PackageInfo enthält Metadaten der installierten App
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }
}

/**
 * Erzwingt eine sofortige Aktualisierung des Widgets.
 * Nutzt den WorkManager, um den BirthdayWorker im Hintergrund auszuführen.
 * Dies sollte immer aufgerufen werden, wenn sich Daten ändern (z.B. Labels oder Filter).
 */
fun updateWidget(context: Context) {
    val workRequest = OneTimeWorkRequestBuilder<BirthdayWorker>()
        .addTag("manual_widget_update")
        .build()

    WorkManager.getInstance(context).enqueue(workRequest)
}
