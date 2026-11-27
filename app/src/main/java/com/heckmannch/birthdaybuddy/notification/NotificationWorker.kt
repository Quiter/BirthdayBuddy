package com.heckmannch.birthdaybuddy.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.heckmannch.birthdaybuddy.domain.model.PendingNotification
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.domain.usecase.CleanupOldNotificationsUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.GetPendingNotificationsUseCase
import com.heckmannch.birthdaybuddy.util.AlarmScheduler
import com.heckmannch.birthdaybuddy.util.Clock
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * Worker to evaluate rules and post notifications for upcoming birthdays.
 */
@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val contactRepository: ContactRepository,
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper,
    private val cleanupOldNotificationsUseCase: CleanupOldNotificationsUseCase,
    private val getPendingNotificationsUseCase: GetPendingNotificationsUseCase,
    private val alarmScheduler: AlarmScheduler,
    private val clock: Clock,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        return try {
            // Vorjahres-Einträge bereinigen, damit die pendingId nicht unbegrenzt wächst
            // und PendingIntent-Request-Code-Kollisionen verhindert werden.
            val currentYear = clock.nowLocalDate().year
            cleanupOldNotificationsUseCase(currentYear)

            // Sync contacts before evaluating rules to make sure we work with the latest data
            contactRepository.syncContacts()

            // Evaluieren der fälligen Benachrichtigungen via Use Case
            val pendingEvents = getPendingNotificationsUseCase(clock.nowLocalDateTime())

            // Für jedes fällige Event erst die Benachrichtigung anzeigen und danach als verarbeitet persistieren,
            // damit bei einem Fehler oder Prozess-Abbruch während des Anzeigens kein Event für das ganze Jahr verloren geht.
            pendingEvents.forEach { event ->
                val pendingId = notificationRepository.getNextPendingNotificationId()
                val pending = PendingNotification(
                    contactLookupKeys = event.dbKeys,
                    daysBefore = event.daysBefore,
                    year = currentYear,
                    id = pendingId,
                )

                notificationHelper.showBirthdayNotification(
                    contacts = event.contacts,
                    daysBefore = event.daysBefore,
                    pendingId = pendingId,
                    eventType = event.eventType,
                    customEventKey = event.customEventKey,
                )

                notificationRepository.insertPendingNotification(pending)
            }

            // Plane den nächsten Lauf sauber und deterministisch über APPEND_OR_REPLACE.
            // Dadurch verkettet WorkManager die nächste Ausführung, ohne den aktuell laufenden Worker
            // abzubrechen oder auf einen in-memory Delay im applicationScope angewiesen zu sein,
            // der bei einem Prozess-Kill verloren gehen könnte.
            scheduleNextRun()

            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to evaluate notifications in NotificationWorker", e)
            try {
                scheduleNextRun()
            } catch (scheduleEx: Exception) {
                Log.e(TAG, "Failed to schedule next notification worker run", scheduleEx)
            }
            Result.retry()
        }
    }

    private suspend fun scheduleNextRun() {
        val settings = settingsRepository.getSettingsImmediate()
        if (!settings.notificationsEnabled) {
            val workManager = WorkManager.getInstance(applicationContext)
            workManager.cancelUniqueWork(WORK_NAME)
            workManager.cancelAllWorkByTag(NotificationActions.WORK_TAG_SNOOZE)
            alarmScheduler.cancelNotificationAlarm()
            return
        }

        val rules = notificationRepository.getAllRulesImmediate()
        alarmScheduler.scheduleNextNotificationAlarm(rules)
    }

    companion object {
        private const val TAG = "NotificationWorker"
        private const val WORK_NAME = NotificationActions.WORK_NAME_NOTIFICATION_UPDATE

        /**
         * Reiht den Worker sofort zur Ausführung ein (wird vom [NotificationAlarmReceiver] aufgerufen).
         */
        @JvmStatic
        fun enqueueImmediateWork(context: Context) {
            val request = OneTimeWorkRequestBuilder<NotificationWorker>()
                // Linearer Backoff (10s), um zeitkritische Benachrichtigungen bei temporären Fehlern rasch zu wiederholen
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .addTag(NotificationActions.WORK_TAG_NOTIFICATION)
                .build()

            // APPEND_OR_REPLACE verhindert, dass zeitnahe Auslöser einen laufenden syncContacts()- oder
            // Notification-Dispatch-Lauf mitten in der Ausführung abbrechen, und ersetzt dennoch fehlgeschlagene Ketten.
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request,
            )
        }
    }
}
