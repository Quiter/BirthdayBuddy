package com.heckmannch.birthdaybuddy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.heckmannch.birthdaybuddy.di.ApplicationScope
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Empfängt BOOT_COMPLETED und MY_PACKAGE_REPLACED, um den WorkManager-Notification-Job
 * nach einem Geräteneustart oder App-Update neu zu planen.
 *
 * Hintergrund: OneTimeWorkRequests werden vom Android-System beim Neustart gelöscht.
 * Ohne diesen Receiver würden Benachrichtigungen nach einem Neustart nicht mehr kommen,
 * bis der Nutzer die App manuell öffnet.
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
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val entryPoint = try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                BootReceiverEntryPoint::class.java
            )
        } catch (e: Exception) {
            Log.w("BootReceiver", "Hilt-Abhängigkeiten konnten nicht bezogen werden (z. B. während eines Testlaufs)", e)
            return
        }

        val pendingResult = goAsync()
        entryPoint.applicationScope().launch {
            try {
                entryPoint.notificationRepository().syncScheduling()
            } catch (_: Exception) {
                // Safeguard: Scheduler-Fehler dürfen den Boot-Prozess nicht blockieren.
            } finally {
                pendingResult.finish()
            }
        }
    }
}
