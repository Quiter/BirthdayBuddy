package com.heckmannch.birthdaybuddy.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.heckmannch.birthdaybuddy.di.ApplicationScope
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.model.PendingNotification
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.usecase.GetPendingNotificationsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

/**
 * Worker to evaluate rules and post notifications for upcoming birthdays.
 *
 * @property applicationScope Application-scoped coroutine scope for post-work scheduling.
 *   Intentionally outlives the Worker to schedule the next run after WorkManager finishes this
 *   execution, ensuring the next run is scheduled without being cancelled by the worker termination.
 */
@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val contactRepository: ContactRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationHelper: NotificationHelper,
    private val getPendingNotificationsUseCase: GetPendingNotificationsUseCase,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
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

        // Plane den nächsten Lauf sauber mit einer kurzen Verzögerung nach Beendigung dieser Ausführung.
        val rules = notificationRepository.getAllRulesImmediate()
        applicationScope.launch {
            delay(1000.milliseconds)
            scheduleNext(context, rules, forceReplace = false)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "FlexibleNotificationUpdate"

        /**
         * Plant den nächsten fälligen Zeitpunkt basierend auf allen Regeln.
         *
         * @param forceReplace Wenn true, wird ein bereits geplanter Worker ersetzt (REPLACE).
         *   Nutze true nach Settings-Änderungen. Nutze false nach einem Worker-Run, damit
         *   kein noch laufender Worker durch die Selbst-Neu-Planung abgebrochen wird.
         */
        @JvmStatic
        fun scheduleNext(
            context: Context,
            rules: List<NotificationRule>,
            forceReplace: Boolean = true
        ) {
            if (rules.isEmpty()) {
                cancelNotification(context)
                return
            }

            val now = LocalDateTime.now()
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
                .addTag("birthday_notification")
                .build()

            val policy = if (forceReplace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                policy,
                request,
            )
        }

        @JvmStatic
        fun cancelNotification(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
