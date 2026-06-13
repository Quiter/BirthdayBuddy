package com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.components

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.heckmannch.birthdaybuddy.data.local.Contact
import com.heckmannch.birthdaybuddy.data.local.NotificationRule
import com.heckmannch.birthdaybuddy.data.local.PendingNotification
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
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

        // Sync contacts before evaluating rules to make sure we work with the latest data
        contactRepository.syncContacts()

        val now = LocalDateTime.now()
        val currentLocalTime = now.toLocalTime().withSecond(0).withNano(0)

        // Finde Regeln, die in den letzten 15 Minuten fällig geworden sind (robust gegen WorkManager Verzögerungen)
        val currentRules = rules.filter { rule ->
            val ruleTime = LocalTime.of(rule.hour, rule.minute)
            val diffMinutes = Duration.between(ruleTime, currentLocalTime).toMinutes()
            diffMinutes in 0..14
        }

        if (currentRules.isNotEmpty()) {
            val allContacts = contactRepository.allContacts.first()
            val today = LocalDate.now()

            currentRules.forEach { rule ->
                val targetDate = today.plusDays(rule.daysBefore.toLong())
                val birthdays = allContacts.filter { contact ->
                    contact.birthday?.let { bday ->
                        (bday.month == targetDate.month) && (bday.dayOfMonth == targetDate.dayOfMonth)
                    } ?: false
                }

                // 1. Geburtstage verarbeiten
                birthdays.forEach { contact ->
                    scheduleEvent(contact, "birthday", contact.lookupKey, rule, today)
                }

                // 2. Weitere Ereignisse verarbeiten (sofern aktiviert)
                if (settings.otherEventsEnabled) {
                    val anniversaries = allContacts.filter { contact ->
                        contact.anniversary?.let { anniv ->
                            (anniv.month == targetDate.month) && (anniv.dayOfMonth == targetDate.dayOfMonth)
                        } ?: false
                    }
                    val processedAnniversaries = HashSet<String>()
                    anniversaries.forEach { contact ->
                        if (processedAnniversaries.contains(contact.lookupKey)) return@forEach

                        val spouseKey = contact.spouseLookupKey
                        val spouse =
                            if (spouseKey != null) anniversaries.find { it.lookupKey == spouseKey } else null

                        if (spouse != null) {
                            scheduleJointEvent(
                                contacts = listOf(contact, spouse),
                                eventType = "anniversary",
                                dbKeys = listOf(
                                    "anniversary:${contact.lookupKey}",
                                    "anniversary:${spouse.lookupKey}"
                                ),
                                rule = rule,
                                today = today
                            )
                            processedAnniversaries.add(contact.lookupKey)
                            processedAnniversaries.add(spouse.lookupKey)
                        } else {
                            scheduleEvent(
                                contact,
                                "anniversary",
                                "anniversary:${contact.lookupKey}",
                                rule,
                                today
                            )
                            processedAnniversaries.add(contact.lookupKey)
                        }
                    }

                    val nameDays = allContacts.filter { contact ->
                        contact.nameDay?.let { nd ->
                            (nd.month == targetDate.month) && (nd.dayOfMonth == targetDate.dayOfMonth)
                        } ?: false
                    }
                    nameDays.forEach { contact ->
                        scheduleEvent(
                            contact,
                            "nameday",
                            "nameday:${contact.lookupKey}",
                            rule,
                            today
                        )
                    }
                }
            }
        }

        // Plane den nächsten Lauf sauber mit einer kurzen Verzögerung nach Beendigung dieser Ausführung auf dem Main-Thread.
        // Dies verhindert eine Race-Condition, bei der sich der aktuell laufende Worker durch REPLACE selbst abbricht.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            scheduleNext(context, rules)
        }, 1000)

        return Result.success()
    }

    private suspend fun scheduleEvent(
        contact: Contact,
        eventType: String,
        dbKey: String,
        rule: NotificationRule,
        today: LocalDate
    ) {
        val alreadyScheduled = notificationRepository.hasNotificationBeenScheduled(
            today.year, rule.daysBefore, dbKey
        )
        if (alreadyScheduled) return

        val pending = PendingNotification(
            contactLookupKeys = listOf(dbKey),
            daysBefore = rule.daysBefore,
            year = today.year
        )
        val pendingId = notificationRepository.insertPendingNotification(pending).toInt()

        notificationHelper.showBirthdayNotification(
            contacts = listOf(contact),
            daysBefore = rule.daysBefore,
            pendingId = pendingId,
            eventType = eventType
        )
    }

    private suspend fun scheduleJointEvent(
        contacts: List<Contact>,
        eventType: String,
        dbKeys: List<String>,
        rule: NotificationRule,
        today: LocalDate
    ) {
        val anyScheduled = dbKeys.any { dbKey ->
            notificationRepository.hasNotificationBeenScheduled(
                today.year, rule.daysBefore, dbKey
            )
        }
        if (anyScheduled) return

        val pending = PendingNotification(
            contactLookupKeys = dbKeys,
            daysBefore = rule.daysBefore,
            year = today.year
        )
        val pendingId = notificationRepository.insertPendingNotification(pending).toInt()

        notificationHelper.showBirthdayNotification(
            contacts = contacts,
            daysBefore = rule.daysBefore,
            pendingId = pendingId,
            eventType = eventType
        )
    }

    companion object {
        private const val WORK_NAME = "FlexibleNotificationUpdate"

        /**
         * Plant den nächsten fälligen Zeitpunkt basierend auf allen Regeln.
         */
        @JvmStatic
        fun scheduleNext(context: Context, rules: List<NotificationRule>) {
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

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        @JvmStatic
        fun cancelNotification(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
