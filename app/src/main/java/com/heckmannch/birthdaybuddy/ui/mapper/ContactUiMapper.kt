package com.heckmannch.birthdaybuddy.ui.mapper

import android.text.format.DateFormat
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.ui.model.BirthdayTier
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.util.getInitials
import com.heckmannch.birthdaybuddy.util.hasYear
import com.heckmannch.birthdaybuddy.util.isBirthdayToday
import com.heckmannch.birthdaybuddy.util.mergeNames
import com.heckmannch.birthdaybuddy.util.safeDaysUntilNext
import com.heckmannch.birthdaybuddy.util.safeNextAge
import dagger.Reusable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Mapper for converting domain model [Contact] to [ContactUiModel].
 * Handles formatting, event type evaluation, and couple-pairing/merging logic.
 */
@Reusable
class ContactUiMapper @Inject constructor() {
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

    // Creates the best format ONLY for day/month based on the user's locale (e.g. "12. Mai" vs "May 12")
    private val dayMonthFormatter = try {
        DateTimeFormatter.ofPattern(
            DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMM"),
            Locale.getDefault(),
        )
    } catch (_: Throwable) {
        // Fallback for local JVM tests (where DateFormat is not mocked)
        if (Locale.getDefault().language == Locale.GERMAN.language) {
            DateTimeFormatter.ofPattern("d. MMMM", Locale.getDefault())
        } else {
            DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())
        }
    }

    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())

    fun toUiModel(contact: Contact, today: LocalDate): ContactUiModel {
        return toUiModelForEvent(contact, today, EventType.BIRTHDAY)
    }

    fun toUiModelForEvent(
        contact: Contact,
        today: LocalDate,
        eventType: EventType
    ): ContactUiModel {
        val eventDate = when (eventType) {
            EventType.ANNIVERSARY -> contact.anniversary
            EventType.NAME_DAY -> contact.nameDay
            EventType.BIRTHDAY -> contact.birthday
        }
        val hasYear = eventDate?.hasYear ?: false
        val daysLeft = eventDate?.safeDaysUntilNext(today)
        val nextAgeValue = eventDate?.safeNextAge(today)

        return ContactUiModel(
            id = contact.lookupKey,
            contactId = contact.contactId,
            lookupKey = contact.lookupKey,
            fullName = contact.fullName,
            dateText = when {
                eventDate == null -> "-"
                !hasYear -> eventDate.format(dayMonthFormatter)
                else -> eventDate.format(dateFormatter)
            },
            monthName = eventDate?.format(monthFormatter) ?: "",
            imageUri = contact.imageUri,
            phoneNumber = contact.phoneNumber,
            initials = contact.fullName.getInitials(),
            nextAge = nextAgeValue,
            daysUntilNext = daysLeft,
            isToday = eventDate?.isBirthdayToday(today) ?: false,
            isFavorite = contact.isFavorite,
            hasWhatsApp = contact.hasWhatsApp,
            hasSignal = contact.hasSignal,
            labels = contact.labels,
            giftIdeas = contact.giftIdeas,
            birthday = contact.birthday,
            birthdayTier = BirthdayTier.from(nextAgeValue),
        )
    }

    /**
     * Maps a filtered list of domain [Contact] models to their corresponding [ContactUiModel]s,
     * applying couple pairing/merging for anniversaries and resolving event types.
     */
    fun mapToUiModels(
        contacts: List<Contact>,
        today: LocalDate,
        selectedLabel: String?,
        labelsEnabled: Boolean,
        otherEventsEnabled: Boolean,
    ): List<ContactUiModel> {
        val uiList = if (!labelsEnabled) {
            // "No labels" path: Show birthdays, name days, and anniversaries in one flat list,
            // with couple merging for anniversaries.
            val birthdays = contacts.asSequence()
                .filter { it.birthday != null }
                .map { toUiModelForEvent(it, today, EventType.BIRTHDAY).copy(labels = emptyList()) }
                .toList()

            val nameDays = if (otherEventsEnabled) {
                contacts.asSequence()
                    .filter { it.nameDay != null }
                    .map {
                        toUiModelForEvent(
                            it,
                            today,
                            EventType.NAME_DAY
                        ).copy(labels = emptyList())
                    }
                    .toList()
            } else emptyList()

            val pairedAnniversaries = if (otherEventsEnabled) {
                buildAnniversaryList(contacts, today, mergeLabels = false)
            } else emptyList()

            // Contacts without any event are displayed as birthdays with "-" date text (visible during search)
            val contactsWithNoEvent = contacts.asSequence()
                .filter { contact ->
                    val hasNoAnniversary = !otherEventsEnabled || contact.anniversary == null
                    val hasNoNameDay = !otherEventsEnabled || contact.nameDay == null
                    contact.birthday == null && hasNoAnniversary && hasNoNameDay
                }
                .map { toUiModelForEvent(it, today, EventType.BIRTHDAY).copy(labels = emptyList()) }
                .toList()

            birthdays + nameDays + pairedAnniversaries + contactsWithNoEvent
        } else {
            // "Labels enabled" path: Event type is determined by selectedLabel.
            val displayEventType: EventType = when (selectedLabel) {
                ContactLabels.LABEL_ANNIVERSARY -> EventType.ANNIVERSARY
                ContactLabels.LABEL_NAME_DAY -> EventType.NAME_DAY
                else -> EventType.BIRTHDAY
            }

            if (displayEventType == EventType.ANNIVERSARY) {
                buildAnniversaryList(contacts, today, mergeLabels = true)
            } else {
                contacts.map { toUiModelForEvent(it, today, displayEventType) }
            }
        }

        return uiList.sortedWith(
            compareBy<ContactUiModel, Long?>(nullsLast(naturalOrder())) { it.daysUntilNext }
                .thenBy { it.fullName }
        )
    }

    private fun buildAnniversaryList(
        contacts: List<Contact>,
        today: LocalDate,
        mergeLabels: Boolean,
    ): List<ContactUiModel> {
        val processedKeys = mutableSetOf<String>()
        val list = mutableListOf<ContactUiModel>()
        val contactMap = contacts.associateBy { it.lookupKey }

        for (contact in contacts) {
            if (contact.anniversary == null) continue
            if (processedKeys.contains(contact.lookupKey)) continue

            val spouseKey = contact.spouseLookupKey
            val spouse = if (spouseKey != null) contactMap[spouseKey] else null

            if (spouse != null && spouse.anniversary != null) {
                processedKeys.add(contact.lookupKey)
                processedKeys.add(spouse.lookupKey)

                val uiModelA = toUiModelForEvent(contact, today, EventType.ANNIVERSARY)
                val uiModelB = toUiModelForEvent(spouse, today, EventType.ANNIVERSARY)

                val mergedModel = ContactUiModel(
                    id = "${contact.lookupKey}_${spouse.lookupKey}",
                    contactId = contact.contactId,
                    lookupKey = contact.lookupKey,
                    fullName = mergeNames(contact.fullName, spouse.fullName),
                    dateText = uiModelA.dateText,
                    monthName = uiModelA.monthName,
                    imageUri = contact.imageUri,
                    phoneNumber = contact.phoneNumber,
                    initials = uiModelA.initials,
                    nextAge = uiModelA.nextAge,
                    daysUntilNext = uiModelA.daysUntilNext,
                    isToday = uiModelA.isToday,
                    isFavorite = contact.isFavorite || spouse.isFavorite,
                    hasWhatsApp = contact.hasWhatsApp || spouse.hasWhatsApp,
                    hasSignal = contact.hasSignal || spouse.hasSignal,
                    labels = if (mergeLabels) {
                        (contact.labels + spouse.labels).distinct()
                    } else emptyList(),
                    giftIdeas = uiModelA.giftIdeas + uiModelB.giftIdeas,
                    birthday = contact.birthday,
                    secondImageUri = spouse.imageUri,
                    secondInitials = uiModelB.initials,
                    secondFullName = spouse.fullName,
                    isCouple = true,
                )
                list.add(mergedModel)
            } else {
                processedKeys.add(contact.lookupKey)
                val single = toUiModelForEvent(contact, today, EventType.ANNIVERSARY)
                list.add(if (mergeLabels) single else single.copy(labels = emptyList()))
            }
        }
        return list
    }
}
