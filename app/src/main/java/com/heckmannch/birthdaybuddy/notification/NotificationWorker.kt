package com.heckmannch.birthdaybuddy.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.model.PendingNotification
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.usecase.GetPendingNotificationsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
    private val notificationHelper: NotificationHelper,
    private val getPendingNotificationsUseCase: GetPendingNotificationsUseCase,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        // Vorjahres-Einträge bereinigen, damit die pendingId nicht unbegrenzt wächst
        // und PendingIntent-Request-Code-Kollisionen verhindert werden.
        val currentYear = LocalDate.now().year
        notificationRepository.deleteOldNotifications(currentYear)

        // Sync contacts before evaluating rules to make sure we work with the latest data
        contactRepository.syncContacts()

        // Evaluieren der fälligen Benachrichtigungen via Use Case
        val pendingEvents = getPendingNotificationsUseCase(LocalDateTime.now())

        // Für jedes fällige Event eine PendingNotification einfügen und anzeigen
        pendingEvents.forEach { event ->
            val pending = PendingNotification(
                contactLookupKeys = event.dbKeys,
                daysBefore = event.daysBefore,
                year = currentYear
            )
            val pendingId = notificationRepository.insertPendingNotification(pending).toInt()

            notificationHelper.showBirthdayNotification(
                contacts = event.contacts,
                daysBefore = event.daysBefore,
                pendingId = pendingId,
                eventType = event.eventType
            )
        }

        // Plane den nächsten Lauf sauber und deterministisch über APPEND_OR_REPLACE.
        // Dadurch verkettet WorkManager die nächste Ausführung, ohne den aktuell laufenden Worker
        // abzubrechen oder auf einen in-memory Delay im applicationScope angewiesen zu sein,
        // der bei einem Prozess-Kill verloren gehen könnte.
        val settings = notificationRepository.getSettingsImmediate()
        if (!settings.notificationsEnabled) {
            WorkManager.getInstance(applicationContext).cancelUniqueWork(WORK_NAME)
            return Result.success()
        }

        val rules = notificationRepository.getAllRulesImmediate()
        scheduleNext(applicationContext, rules, ExistingWorkPolicy.APPEND_OR_REPLACE)

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = NotificationActions.WORK_NAME_NOTIFICATION_UPDATE

        /**
         * Plant den nächsten fälligen Zeitpunkt basierend auf allen Regeln.
         *
         * @param existingWorkPolicy Richtlinie für die Behandlung von Arbeitskonflikten (standardmäßig [ExistingWorkPolicy.REPLACE]).
         *   Nutze [ExistingWorkPolicy.REPLACE] nach Settings-Änderungen.
         *   Nutze [ExistingWorkPolicy.APPEND_OR_REPLACE] für die Folgeplanung aus dem laufenden Worker,
         *   damit die nächste Ausführung verkettet wird, ohne den aktuellen Worker abzubrechen oder
         *   ignoriert zu werden.
         * @param now Der aktuelle Zeitpunkt für die Berechnung (standardmäßig [LocalDateTime.now]).
         */
        @JvmStatic
        @JvmOverloads
        fun scheduleNext(
            context: Context,
            rules: List<NotificationRule>,
            existingWorkPolicy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE,
            now: LocalDateTime = LocalDateTime.now()
        ) {
            if (rules.isEmpty()) {
                cancelNotification(context)
                return
            }

            val uniqueTimes = rules.asSequence()
                .map { LocalTime.of(it.hour, it.minute) }
                .distinct()
                .sorted()
                .toList()

            // Finde die nächste Zeit heute oder die erste Zeit morgen
            val nextTime = uniqueTimes.firstOrNull { it.isAfter(now.toLocalTime()) }
                ?: uniqueTimes.first()

            var targetDateTime = LocalDateTime.of(now.toLocalDate(), nextTime)
            if (!targetDateTime.isAfter(now)) {
                targetDateTime = targetDateTime.plusDays(1)
            }

            val delay = Duration.between(now, targetDateTime).toMillis()

            val request = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(NotificationActions.WORK_TAG_NOTIFICATION)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                existingWorkPolicy,
                request,
            )
        }

        @JvmStatic
        fun cancelNotification(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
