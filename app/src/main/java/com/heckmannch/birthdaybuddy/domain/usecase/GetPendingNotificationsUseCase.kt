package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.util.toYear
import dagger.Reusable
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/**
 * Evaluates active notification rules for the given time and returns the list of events
 * (birthdays, anniversaries, name days) that should be notified to the user.
 * It considers any rules whose scheduled time has arrived today and filters out already
 * scheduled notifications via the database to prevent duplicates even when execution is delayed.
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
        val eventType: EventType,
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

        // Find rules that are due today up to the current time.
        // Tolerates arbitrary WorkManager / Doze mode wake-up delays without skipping
        // notifications, relying on hasNotificationBeenScheduled below for deduplication.
        val currentRules = rules.filter { rule ->
            val ruleTime = LocalTime.of(rule.hour, rule.minute)
            ruleTime <= currentLocalTime
        }

        if (currentRules.isEmpty()) return emptyList()

        val labelsEnabled = contactRepository.labelsEnabled.first()
        val disabledNotificationLabels =
            if (!labelsEnabled) emptySet() else contactRepository.labelConfigs.first()
                .asSequence()
                .filter { it.isIgnored || !it.notificationsEnabled }
                .map { it.name }
                .toSet()

        val allContacts = contactRepository.allContacts.first().filter { contact ->
            !labelsEnabled || contact.labels.none { it in disabledNotificationLabels }
        }
        val pendingEvents = mutableListOf<PendingNotificationEvent>()

        for (rule in currentRules) {
            val targetDate = today.plusDays(rule.daysBefore.toLong())

            // 1. Birthdays
            val birthdays = allContacts.filter { contact ->
                contact.birthday?.let { bday ->
                    bday.toYear(targetDate.year) == targetDate
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
                            eventType = EventType.BIRTHDAY,
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
                        anniv.toYear(targetDate.year) == targetDate
                    } ?: false
                }
                val processedAnniversaries = mutableSetOf<String>()
                for (contact in anniversaries) {
                    if (processedAnniversaries.contains(contact.lookupKey)) continue

                    val spouseKey = contact.spouseLookupKey
                    val spouse =
                        if (spouseKey != null) anniversaries.find { it.lookupKey == spouseKey } else null

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
                                    eventType = EventType.ANNIVERSARY,
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
                                    eventType = EventType.ANNIVERSARY,
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
                        nd.toYear(targetDate.year) == targetDate
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
                                eventType = EventType.NAME_DAY,
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
