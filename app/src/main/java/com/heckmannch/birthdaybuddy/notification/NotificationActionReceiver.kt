package com.heckmannch.birthdaybuddy.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.getSystemService
import com.heckmannch.birthdaybuddy.di.ApplicationScope
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.usecase.DismissNotificationUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.MarkNotificationAsDoneUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.ReshowNotificationUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.SnoozeNotificationUseCase
import com.heckmannch.birthdaybuddy.domain.util.NotificationKeyUtils
import com.heckmannch.birthdaybuddy.util.IntentExtras
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver handling action clicks from notification status bar alerts (Snooze, Done, Dismissed).
 *
 * Uses [EntryPointAccessors] instead of `@AndroidEntryPoint` to prevent component resolution crashes
 * during test runs or system broadcasts before full Hilt runner initialization.
 */
class NotificationActionReceiver internal constructor(
    private val entryPointProvider: ((Context) -> NotificationActionReceiverEntryPoint)? = null,
) : BroadcastReceiver() {

    constructor() : this(null)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationActionReceiverEntryPoint {
        @ApplicationScope
        fun applicationScope(): CoroutineScope

        fun notificationRepository(): NotificationRepository

        fun markNotificationAsDoneUseCase(): MarkNotificationAsDoneUseCase

        fun dismissNotificationUseCase(): DismissNotificationUseCase

        fun snoozeNotificationUseCase(): SnoozeNotificationUseCase

        fun reshowNotificationUseCase(): ReshowNotificationUseCase
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        if (action != NotificationActions.ACTION_SNOOZE &&
            action != NotificationActions.ACTION_DONE &&
            action != NotificationActions.ACTION_DISMISSED
        ) {
            return
        }

        val entryPoint = try {
            entryPointProvider?.invoke(context)
                ?: EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    NotificationActionReceiverEntryPoint::class.java,
                )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve Hilt EntryPoint dependencies", e)
            return
        }

        val notificationId = IntentExtras.safeGetIntExtra(intent, NotificationActions.EXTRA_NOTIFICATION_ID, -1)
        val pendingId = IntentExtras.safeGetIntExtra(intent, NotificationActions.EXTRA_PENDING_ID, -1)
        val daysBefore = IntentExtras.safeGetIntExtra(intent, NotificationActions.EXTRA_DAYS_BEFORE, 0)
        val lookupKeys = IntentExtras.safeGetStringArrayExtra(intent, NotificationActions.EXTRA_LOOKUP_KEYS)

        when (action) {
            NotificationActions.ACTION_SNOOZE -> {
                // 1. Aktuelle Benachrichtigung schließen
                val notificationManager = context.getSystemService<NotificationManager>()
                notificationManager?.cancel(notificationId)

                // 2. Erneute Erinnerung in 2 Stunden planen (via Use Case)
                try {
                    entryPoint.snoozeNotificationUseCase()(
                        pendingId = pendingId,
                        daysBefore = daysBefore,
                        lookupKeys = lookupKeys.toList(),
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
                    entryPoint.applicationScope().launch {
                        try {
                            entryPoint.markNotificationAsDoneUseCase()(pendingId)
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            Log.e(
                                TAG,
                                "Fehler beim Markieren der Benachrichtigung als erledigt (pendingId=$pendingId)",
                                e,
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
                    entryPoint.applicationScope().launch {
                        try {
                            // Zähler für Wisch-Versuche erhöhen
                            entryPoint.dismissNotificationUseCase()(pendingId)

                            val pendingNotification =
                                entryPoint.notificationRepository().getPendingNotificationById(pendingId)
                            val isStillActive = pendingNotification != null && !pendingNotification.isDone
                            if (isStillActive) {
                                val firstKey = lookupKeys.firstOrNull() ?: ""
                                val eventType = NotificationKeyUtils.extractEventType(firstKey)
                                entryPoint.reshowNotificationUseCase()(
                                    pendingId = pendingId,
                                    daysBefore = daysBefore,
                                    lookupKeys = lookupKeys.toList(),
                                    eventType = eventType,
                                )
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            Log.e(
                                TAG,
                                "Fehler beim erneuten Anzeigen der verworfenen Benachrichtigung (pendingId=$pendingId)",
                                e,
                            )
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "NotificationActionReceiver"
    }
}
