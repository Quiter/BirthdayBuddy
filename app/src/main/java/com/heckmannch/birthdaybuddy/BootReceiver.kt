package com.heckmannch.birthdaybuddy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.heckmannch.birthdaybuddy.di.ApplicationScope
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import com.heckmannch.birthdaybuddy.util.AlarmScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Empfängt BOOT_COMPLETED, MY_PACKAGE_REPLACED, TIMEZONE_CHANGED, TIME_SET und DATE_CHANGED,
 * um den WorkManager-Notification-Job nach einem Geräteneustart, App-Update oder Zeitanpassungen neu zu planen.
 *
 * Hintergrund: OneTimeWorkRequests werden vom Android-System beim Neustart gelöscht.
 * Zudem verbleibt bei Zeitzonen- oder Uhrzeitwechseln der geplante Job auf der alten absoluten Zeit.
 * Dieser Receiver stellt sicher, dass Benachrichtigungen stets zur korrekten lokalen Uhrzeit ausgelöst werden.
 *
 * Widget-Update-Strategie (Single-Path):
 * 1. Sofortiges Rendern: [WidgetUpdater.updateWidget] aktualisiert bestehende Glance-Homescreen-Widgets
 *    unmittelbar mit dem aktuellen Datenbestand, sodass keine veralteten Zustände nach Boot/Zeitwechsel sichtbar sind.
 * 2. Deterministisches Mitternachts-Scheduling: [AlarmScheduler.scheduleNextWidgetUpdateAlarm] setzt einen
 *    exakten Alarm ([android.app.AlarmManager.setExactAndAllowWhileIdle]) für Mitternacht (00:01 Uhr).
 *    Dadurch wird der Datumswechsel auch im Doze-Modus pünktlich vollzogen.
 * 3. WorkManager-Rolle: WorkManager wird hier bewusst NICHT mit verzögerten Jobs ([androidx.work.OneTimeWorkRequest.Builder.setInitialDelay])
 *    eingeplant, da dieser im Doze-Modus bis zu Wartungsfenstern verschoben werden kann. Stattdessen dient
 *    WorkManager ausschließlich zur asynchronen Ausführung bei Auslösen des AlarmReceivers sowie als
 *    Retry-Mechanismus (Backoff) bei transienten Fehlern.
 *
 * Hinweis zur Hilt-Injection:
 * Der Receiver verwendet [EntryPointAccessors] anstelle von @AndroidEntryPoint mit try-catch,
 * um Abstürze während automatisierter Testläufe (z. B. connectedDebugAndroidTest) zu verhindern,
 * wenn das System den Broadcast beim Installieren des Test-APKs auslöst, bevor der HiltTestRunner
 * die Test-Komponente initialisiert hat.
 */
class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootReceiverEntryPoint {
        @ApplicationScope
        fun applicationScope(): CoroutineScope
        fun notificationRepository(): NotificationRepository
        fun widgetUpdater(): WidgetUpdater
        fun alarmScheduler(): AlarmScheduler
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIMEZONE_CHANGED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_DATE_CHANGED
        ) return

        val entryPoint = try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                BootReceiverEntryPoint::class.java
            )
        } catch (e: Exception) {
            Log.w(
                "BootReceiver",
                "Hilt-Abhängigkeiten konnten nicht bezogen werden (z. B. während eines Testlaufs)",
                e
            )
            return
        }

        val pendingResult = goAsync()
        entryPoint.applicationScope().launch {
            try {
                try {
                    entryPoint.notificationRepository().syncScheduling()
                } catch (e: Exception) {
                    // Safeguard: Scheduler-Fehler dürfen den Boot-Prozess nicht blockieren.
                    Log.w(TAG, "Fehler bei der Benachrichtigungsplanung nach Boot/Zeitänderung", e)
                }

                try {
                    entryPoint.widgetUpdater().updateWidget()
                    entryPoint.alarmScheduler().scheduleNextWidgetUpdateAlarm()
                } catch (e: Exception) {
                    // Safeguard: Fehler beim Widget-Update dürfen die Benachrichtigungsplanung und den Boot-Prozess nicht blockieren.
                    Log.w(TAG, "Fehler beim Widget-Update nach Boot/Zeitänderung", e)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
