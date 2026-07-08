package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.data.local.Contact
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
import dagger.Reusable
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/**
 * Evaluates active notification rules for the given time and returns the list of events
 * (birthdays, anniversaries, name days) that should be notified to the user.
 * It filters out already scheduled notifications to avoid duplicates.
 */
@Reusable
class GetPendingNotificationsUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val notificationRepository: NotificationRepository,
) {
    /**
     * Represents a single notification event that should be scheduled/shown.
     */
    data class PendingNotificationEvent(
        val contacts: List<Contact>,
        val eventType: String, // "birthday", "anniversary", "nameday"
        val dbKeys: List<String>,
        val daysBefore: Int
    )

    suspend operator fun invoke(now: LocalDateTime): List<PendingNotificationEvent> {
        val settings = notificationRepository.settings.first()
        if (!settings.notificationsEnabled) return emptyList()

        val rules = notificationRepository.getAllRulesImmediate()
        if (rules.isEmpty()) return emptyList()

        val currentLocalTime = now.toLocalTime().withSecond(0).withNano(0)
        val today = now.toLocalDate()

        // Find rules that are active/due in the last 45 minutes of the current time.
        // A robust buffer against WorkManager delays.
        val currentRules = rules.filter { rule ->
            val ruleTime = LocalTime.of(rule.hour, rule.minute)
            val diffMinutes = Duration.between(ruleTime, currentLocalTime).toMinutes()
            diffMinutes in 0..44
        }

        if (currentRules.isEmpty()) return emptyList()

        val allContacts = contactRepository.allContacts.first()
        val pendingEvents = mutableListOf<PendingNotificationEvent>()

        for (rule in currentRules) {
            val targetDate = today.plusDays(rule.daysBefore.toLong())

            // 1. Birthdays
            val birthdays = allContacts.filter { contact ->
                contact.birthday?.let { bday ->
                    (bday.month == targetDate.month) && (bday.dayOfMonth == targetDate.dayOfMonth)
                } ?: false
            }
            for (contact in birthdays) {
                val dbKey = contact.lookupKey
                val alreadyScheduled = notificationRepository.hasNotificationBeenScheduled(
                    today.year, rule.daysBefore, dbKey
                )
                if (!alreadyScheduled) {
                    pendingEvents.add(
                        PendingNotificationEvent(
                            contacts = listOf(contact),
                            eventType = "birthday",
                            dbKeys = listOf(dbKey),
                            daysBefore = rule.daysBefore
                        )
                    )
                }
            }

            // 2. Further events (only if otherEventsEnabled is active)
            if (settings.otherEventsEnabled) {
                // Anniversaries with spouse coupling
                val anniversaries = allContacts.filter { contact ->
                    contact.anniversary?.let { anniv ->
                        (anniv.month == targetDate.month) && (anniv.dayOfMonth == targetDate.dayOfMonth)
                    } ?: false
                }
                val processedAnniversaries = mutableSetOf<String>()
                for (contact in anniversaries) {
                    if (processedAnniversaries.contains(contact.lookupKey)) continue

                    val spouseKey = contact.spouseLookupKey
                    val spouse = if (spouseKey != null) anniversaries.find { it.lookupKey == spouseKey } else null

                    if (spouse != null) {
                        val dbKeys = listOf(
                            "anniversary:${contact.lookupKey}",
                            "anniversary:${spouse.lookupKey}"
                        )
                        val anyScheduled = dbKeys.any { dbKey ->
                            notificationRepository.hasNotificationBeenScheduled(
                                today.year, rule.daysBefore, dbKey
                            )
                        }
                        if (!anyScheduled) {
                            pendingEvents.add(
                                PendingNotificationEvent(
                                    contacts = listOf(contact, spouse),
                                    eventType = "anniversary",
                                    dbKeys = dbKeys,
                                    daysBefore = rule.daysBefore
                                )
                            )
                        }
                        processedAnniversaries.add(contact.lookupKey)
                        processedAnniversaries.add(spouse.lookupKey)
                    } else {
                        val dbKey = "anniversary:${contact.lookupKey}"
                        val alreadyScheduled = notificationRepository.hasNotificationBeenScheduled(
                            today.year, rule.daysBefore, dbKey
                        )
                        if (!alreadyScheduled) {
                            pendingEvents.add(
                                PendingNotificationEvent(
                                    contacts = listOf(contact),
                                    eventType = "anniversary",
                                    dbKeys = listOf(dbKey),
                                    daysBefore = rule.daysBefore
                                )
                            )
                        }
                        processedAnniversaries.add(contact.lookupKey)
                    }
                }

                // Name days
                val nameDays = allContacts.filter { contact ->
                    contact.nameDay?.let { nd ->
                        (nd.month == targetDate.month) && (nd.dayOfMonth == targetDate.dayOfMonth)
                    } ?: false
                }
                for (contact in nameDays) {
                    val dbKey = "nameday:${contact.lookupKey}"
                    val alreadyScheduled = notificationRepository.hasNotificationBeenScheduled(
                        today.year, rule.daysBefore, dbKey
                    )
                    if (!alreadyScheduled) {
                        pendingEvents.add(
                            PendingNotificationEvent(
                                contacts = listOf(contact),
                                eventType = "nameday",
                                dbKeys = listOf(dbKey),
                                daysBefore = rule.daysBefore
                            )
                        )
                    }
                }
            }
        }

        return pendingEvents
    }
}
