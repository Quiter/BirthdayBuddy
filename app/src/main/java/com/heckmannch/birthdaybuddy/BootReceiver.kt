package com.heckmannch.birthdaybuddy

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.heckmannch.birthdaybuddy.di.ApplicationScope
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import com.heckmannch.birthdaybuddy.domain.usecase.ScheduleDailyWidgetUpdateUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.SyncNotificationSchedulingUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Empfängt `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED`, `TIME_SET`, `DATE_CHANGED`
 * sowie ab Android 12 (API 31) [AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED],
 * um Benachrichtigungs- und Widget-Alarme nach einem Geräteneustart, App-Update, Zeitanpassungen
 * oder nachträglicher Erteilung der `SCHEDULE_EXACT_ALARM`-Berechtigung sauber neu zu planen.
 *
 * Hintergrund:
 * - OneTimeWorkRequests und geplante Alarme werden vom Android-System beim Neustart gelöscht.
 * - Bei Zeitzonen- oder Uhrzeitwechseln verbleiben bestehende Jobs/Alarme auf der alten absoluten Zeit.
 * - Wird `SCHEDULE_EXACT_ALARM` vom Nutzer nachträglich in den Systemeinstellungen erteilt,
 *   löst das System [AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED] aus.
 *   Durch die sofortige Neuplanung via [SyncNotificationSchedulingUseCase] und
 *   [ScheduleDailyWidgetUpdateUseCase] werden zuvor gesetzte ungenaue Fallback-Alarme
 *   (`setAndAllowWhileIdle`) unmittelbar durch exakte Alarme (`setExactAndAllowWhileIdle`) ersetzt.
 *
 * Widget-Update-Strategie (Single-Path):
 * 1. Sofortiges Rendern: [WidgetUpdater.updateWidget] aktualisiert bestehende Glance-Homescreen-Widgets
 *    unmittelbar mit dem aktuellen Datenbestand, sodass keine veralteten Zustände nach Boot/Zeitwechsel sichtbar sind.
 * 2. Deterministisches Mitternachts-Scheduling: [ScheduleDailyWidgetUpdateUseCase] delegiert an den
 *    AlarmScheduler, um einen exakten Alarm ([android.app.AlarmManager.setExactAndAllowWhileIdle]) für Mitternacht (00:01 Uhr) zu setzen.
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
        fun syncNotificationSchedulingUseCase(): SyncNotificationSchedulingUseCase
        fun scheduleDailyWidgetUpdateUseCase(): ScheduleDailyWidgetUpdateUseCase
        fun widgetUpdater(): WidgetUpdater
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        val isSupportedAction = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
        if (!isSupportedAction) return

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
                    entryPoint.syncNotificationSchedulingUseCase()()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    // Safeguard: Scheduler-Fehler dürfen den Boot-Prozess nicht blockieren.
                    Log.w(TAG, "Fehler bei der Benachrichtigungsplanung nach Boot/Zeitänderung", e)
                }

                try {
                    entryPoint.widgetUpdater().updateWidget()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Safeguard: Fehler beim Widget-Update dürfen die weitere Boot-Verarbeitung nicht blockieren.
                    Log.w(TAG, "Fehler beim Widget-Update nach Boot/Zeitänderung", e)
                }

                try {
                    entryPoint.scheduleDailyWidgetUpdateUseCase()()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Safeguard: Fehler beim Widget-Scheduling dürfen den Boot-Prozess nicht blockieren.
                    Log.w(TAG, "Fehler bei der Widget-Zeitplanung nach Boot/Zeitänderung", e)
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
