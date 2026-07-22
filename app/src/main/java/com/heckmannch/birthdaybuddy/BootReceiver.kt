package com.heckmannch.birthdaybuddy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.heckmannch.birthdaybuddy.di.ApplicationScope
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Empfängt BOOT_COMPLETED und MY_PACKAGE_REPLACED, um den WorkManager-Notification-Job
 * nach einem Geräteneustart oder App-Update neu zu planen.
 *
 * Hintergrund: OneTimeWorkRequests werden vom Android-System beim Neustart gelöscht.
 * Ohne diesen Receiver würden Benachrichtigungen nach einem Neustart nicht mehr kommen,
 * bis der Nutzer die App manuell öffnet.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var notificationRepository: NotificationRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                notificationRepository.syncScheduling()
            } catch (_: Exception) {
                // Safeguard: Scheduler-Fehler dürfen den Boot-Prozess nicht blockieren.
            } finally {
                pendingResult.finish()
            }
        }
    }
}
