package com.heckmannch.birthdaybuddy.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.work.Data
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
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }
}

/**
 * Erzwingt eine sofortige Aktualisierung des Widgets, OHNE neue Benachrichtigungen zu senden.
 * Dies ist für manuelle Updates aus der App heraus gedacht (z.B. beim App-Start).
 */
fun updateWidget(context: Context) {
    // Wir übergeben dem Worker die Info, dass er KEINE Benachrichtigungen senden soll.
    val inputData = Data.Builder()
        .putBoolean(BirthdayWorker.KEY_AFFECTS_NOTIFICATIONS, false)
        .build()

    val workRequest = OneTimeWorkRequestBuilder<BirthdayWorker>()
        .setInputData(inputData)
        .addTag("manual_widget_update")
        .build()

    WorkManager.getInstance(context).enqueue(workRequest)
}
