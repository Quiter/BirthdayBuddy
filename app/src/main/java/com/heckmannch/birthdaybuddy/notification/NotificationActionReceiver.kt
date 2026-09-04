package com.heckmannch.birthdaybuddy.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.getSystemService
import com.heckmannch.birthdaybuddy.di.ApplicationScope
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.usecase.ReshowNotificationUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.SnoozeNotificationUseCase
import com.heckmannch.birthdaybuddy.domain.util.NotificationKeyUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationActionReceiver"
    }

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var snoozeNotificationUseCase: SnoozeNotificationUseCase

    @Inject
    lateinit var reshowNotificationUseCase: ReshowNotificationUseCase

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val notificationId = intent.getIntExtra(NotificationActions.EXTRA_NOTIFICATION_ID, -1)
        val pendingId = intent.getIntExtra(NotificationActions.EXTRA_PENDING_ID, -1)
        val daysBefore = intent.getIntExtra(NotificationActions.EXTRA_DAYS_BEFORE, 0)
        val lookupKeys = intent.getStringArrayExtra(NotificationActions.EXTRA_LOOKUP_KEYS) ?: emptyArray()

        when (intent.action) {
            NotificationActions.ACTION_SNOOZE -> {
                // 1. Aktuelle Benachrichtigung schließen
                val notificationManager = context.getSystemService<NotificationManager>()
                notificationManager?.cancel(notificationId)

                // 2. Erneute Erinnerung in 2 Stunden planen (via Use Case)
                try {
                    snoozeNotificationUseCase(
                        pendingId = pendingId,
                        daysBefore = daysBefore,
                        lookupKeys = lookupKeys.toList()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Fehler beim Planen der Schlummer-Erinnerung (pendingId=$pendingId)", e)
                }
            }

            NotificationActions.ACTION_DONE -> {
                // 1. Dismiss the current notification
                val notificationManager = context.getSystemService<NotificationManager>()
                notificationManager?.cancel(notificationId)

                // 2. Mark as done using goAsync() to prevent process termination before database write completes
                if (pendingId != -1) {
                    val pendingResult = goAsync()
                    applicationScope.launch {
                        try {
                            notificationRepository.markAsDone(pendingId)
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            Log.e(
                                TAG,
                                "Fehler beim Markieren der Benachrichtigung als erledigt (pendingId=$pendingId)",
                                e
                            )
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            NotificationActions.ACTION_DISMISSED -> {
                // Wenn weggeschoben wurde, aber nicht erledigt/gesnoozed -> Sofort wieder anzeigen
                // (Das erzwingt die Persistenz auch auf Android 14+)
                if (pendingId != -1) {
                    val pendingResult = goAsync()
                    applicationScope.launch {
                        try {
                            // Zähler für Wisch-Versuche erhöhen
                            notificationRepository.incrementDismissCount(pendingId)

                            val allContacts =
                                notificationRepository.getActiveNotificationsImmediate()
                            val isStillActive = allContacts.any { it.id == pendingId }
                            if (isStillActive) {
                                // Wir brauchen den NotificationHelper. Da wir in einem Receiver sind,
                                // nutzen wir am besten den Worker oder wir triggern einen schnellen Re-show.
                                // Einfachster Weg: Snooze mit 1 Sekunde Delay oder direkter Aufruf.
                                val firstKey = lookupKeys.firstOrNull() ?: ""
                                val eventType = NotificationKeyUtils.extractEventType(firstKey)
                                reshowNotificationUseCase(
                                    pendingId = pendingId,
                                    daysBefore = daysBefore,
                                    lookupKeys = lookupKeys.toList(),
                                    eventType = eventType
                                )
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            Log.e(
                                TAG,
                                "Fehler beim erneuten Anzeigen der verworfenen Benachrichtigung (pendingId=$pendingId)",
                                e
                            )
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }
}

