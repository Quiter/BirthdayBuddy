package com.heckmannch.birthdaybuddy2.ui.screens.settings.notifications.components

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.heckmannch.birthdaybuddy2.repository.ContactRepository
import com.heckmannch.birthdaybuddy2.repository.NotificationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val contactRepository: ContactRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val settings = notificationRepository.settings.first()
        if (!settings.notificationsEnabled) return Result.success()

        val rules = notificationRepository.getAllRulesImmediate()
        if (rules.isEmpty()) return Result.success()

        val now = LocalDateTime.now()
        val currentLocalTime = now.toLocalTime().withSecond(0).withNano(0)

        // Finde Regeln, die jetzt (oder in der letzten Minute) fällig sind
        val currentRules = rules.filter { 
            (it.hour == currentLocalTime.hour) && (it.minute == currentLocalTime.minute) 
        }

        if (currentRules.isNotEmpty()) {
            val allContacts = contactRepository.allContacts.first()
            val today = LocalDate.now()

            currentRules.forEach { rule ->
                val targetDate = today.plusDays(rule.daysBefore.toLong())
                val birthdays = allContacts.filter { 
                    (it.birthday.month == targetDate.month) && (it.birthday.dayOfMonth == targetDate.dayOfMonth) 
                }
                
                // Für jeden Kontakt eine eigene Benachrichtigung erstellen
                birthdays.forEach { contact ->
                    // In DB speichern für Persistenz
                    val pending = com.heckmannch.birthdaybuddy2.database.PendingNotification(
                        contactLookupKeys = listOf(contact.lookupKey),
                        daysBefore = rule.daysBefore,
                        year = today.year
                    )
                    val pendingId = notificationRepository.insertPendingNotification(pending).toInt()
                    
                    notificationHelper.showBirthdayNotification(
                        contacts = listOf(contact), 
                        daysBefore = rule.daysBefore, 
                        pendingId = pendingId
                    )
                }
            }
        }

        // Plane den nächsten Lauf
        scheduleNext(context, rules)

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "FlexibleNotificationUpdate"

        /**
         * Plant den nächsten fälligen Zeitpunkt basierend auf allen Regeln.
         */
        fun scheduleNext(context: Context, rules: List<com.heckmannch.birthdaybuddy2.database.NotificationRule>) {
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
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(requiresBatteryNotLow = true).build())
                .addTag("birthday_notification")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun cancelNotification(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
