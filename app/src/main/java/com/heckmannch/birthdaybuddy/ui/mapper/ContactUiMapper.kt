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
 * Handles formatting, event type evaluation, custom date-field events, and couple-pairing/merging logic.
 */
@Reusable
class ContactUiMapper @Inject constructor() {
    private data class LocaleFormatters(
        val locale: Locale,
        val dateFormatter: DateTimeFormatter,
        val dayMonthFormatter: DateTimeFormatter,
        val monthFormatter: DateTimeFormatter,
    )

    @Volatile
    private var cachedFormatters: LocaleFormatters? = null

    private fun getFormatters(): LocaleFormatters {
        val currentLocale = Locale.getDefault()
        val cached = cachedFormatters
        if (cached != null && cached.locale == currentLocale) {
            return cached
        }
        return createFormatters(currentLocale).also { cachedFormatters = it }
    }

    private fun createFormatters(locale: Locale): LocaleFormatters = LocaleFormatters(
        locale = locale,
        dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale),
        // Creates the best format ONLY for day/month based on the user's locale (e.g. "12. Mai" vs "May 12")
        dayMonthFormatter = try {
            DateTimeFormatter.ofPattern(
                DateFormat.getBestDateTimePattern(locale, "dMMMM"),
                locale,
            )
        } catch (_: Throwable) {
            // Fallback for local JVM tests (where DateFormat is not mocked)
            if (locale.language == Locale.GERMAN.language) {
                DateTimeFormatter.ofPattern("d. MMMM", locale)
            } else {
                DateTimeFormatter.ofPattern("d MMMM", locale)
            }
        },
        monthFormatter = DateTimeFormatter.ofPattern("MMMM", locale),
    )

    fun toUiModel(contact: Contact, today: LocalDate): ContactUiModel {
        return toUiModelForEvent(contact, today, EventType.BIRTHDAY)
    }

    fun toUiModelForEvent(
        contact: Contact,
        today: LocalDate,
        eventType: EventType,
        customEventKey: String? = null,
    ): ContactUiModel {
        val eventDate = when (eventType) {
            EventType.ANNIVERSARY -> contact.anniversary
            EventType.NAME_DAY -> contact.nameDay
            EventType.CUSTOM -> customEventKey?.let { contact.customEvents[it] }
            EventType.BIRTHDAY -> contact.birthday
        }
        return buildUiModel(
            contact = contact,
            today = today,
            eventDate = eventDate,
            id = contact.lookupKey,
        )
    }

    private fun buildUiModel(
        contact: Contact,
        today: LocalDate,
        eventDate: LocalDate?,
        id: String,
    ): ContactUiModel {
        val formatters = getFormatters()
        val hasYear = eventDate?.hasYear ?: false
        val daysLeft = eventDate?.safeDaysUntilNext(today)
        val nextAgeValue = eventDate?.safeNextAge(today)

        return ContactUiModel(
            id = id,
            contactId = contact.contactId,
            lookupKey = contact.lookupKey,
            fullName = contact.fullName,
            dateText = when {
                eventDate == null -> "-"
                !hasYear -> eventDate.format(formatters.dayMonthFormatter)
                else -> eventDate.format(formatters.dateFormatter)
            },
            monthName = eventDate?.format(formatters.monthFormatter) ?: "",
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
     * applying couple pairing/merging for anniversaries and resolving standard or custom event types.
     */
    fun mapToUiModels(
        contacts: List<Contact>,
        today: LocalDate,
        selectedLabel: String?,
        labelsEnabled: Boolean,
        otherEventsEnabled: Boolean,
        disabledOtherEventLabels: Set<String> = emptySet(),
    ): List<ContactUiModel> {
        val uiList = if (!labelsEnabled) {
            // "No labels" path: Show birthdays, enabled name days, anniversaries, and custom events
            // in one flat list, with couple merging for anniversaries.
            val anniversaryEnabled = otherEventsEnabled &&
                ContactLabels.LABEL_ANNIVERSARY !in disabledOtherEventLabels
            val nameDayEnabled = otherEventsEnabled &&
                ContactLabels.LABEL_NAME_DAY !in disabledOtherEventLabels

            val birthdays = contacts.asSequence()
                .filter { it.birthday != null }
                .map { toUiModelForEvent(it, today, EventType.BIRTHDAY).copy(labels = emptyList()) }
                .toList()

            val nameDays = if (nameDayEnabled) {
                contacts.asSequence()
                    .filter { it.nameDay != null }
                    .map {
                        buildUiModel(
                            contact = it,
                            today = today,
                            eventDate = it.nameDay,
                            id = "${it.lookupKey}_${ContactLabels.LABEL_NAME_DAY}",
                        ).copy(labels = emptyList())
                    }
                    .toList()
            } else emptyList()

            val pairedAnniversaries = if (anniversaryEnabled) {
                buildAnniversaryList(contacts, today, mergeLabels = false, uniqueIdSuffix = true)
            } else emptyList()

            val customEvents = if (otherEventsEnabled) {
                contacts.asSequence()
                    .flatMap { contact ->
                        contact.customEvents.entries.asSequence()
                            .filter { (key, _) -> key !in disabledOtherEventLabels }
                            .map { (key, date) ->
                                buildUiModel(
                                    contact = contact,
                                    today = today,
                                    eventDate = date,
                                    id = "${contact.lookupKey}_$key",
                                ).copy(labels = emptyList())
                            }
                    }
                    .toList()
            } else emptyList()

            // Contacts without any enabled event are displayed as birthdays with "-" date text (visible during search)
            val contactsWithNoEvent = contacts.asSequence()
                .filter { contact ->
                    val hasNoAnniversary = !anniversaryEnabled || contact.anniversary == null
                    val hasNoNameDay = !nameDayEnabled || contact.nameDay == null
                    val hasNoCustomEvents = !otherEventsEnabled ||
                        contact.customEvents.keys.none { it !in disabledOtherEventLabels }
                    contact.birthday == null && hasNoAnniversary && hasNoNameDay && hasNoCustomEvents
                }
                .map { toUiModelForEvent(it, today, EventType.BIRTHDAY).copy(labels = emptyList()) }
                .toList()

            birthdays + nameDays + pairedAnniversaries + customEvents + contactsWithNoEvent
        } else {
            // "Labels enabled" path: Event type is determined by selectedLabel.
            when {
                selectedLabel == ContactLabels.LABEL_ANNIVERSARY -> {
                    buildAnniversaryList(contacts, today, mergeLabels = true, uniqueIdSuffix = false)
                }

                selectedLabel == ContactLabels.LABEL_NAME_DAY -> {
                    contacts.map { toUiModelForEvent(it, today, EventType.NAME_DAY) }
                }

                selectedLabel != null && ContactLabels.isDynamicCustomEventKey(selectedLabel) -> {
                    contacts.map {
                        toUiModelForEvent(
                            contact = it,
                            today = today,
                            eventType = EventType.CUSTOM,
                            customEventKey = selectedLabel,
                        )
                    }
                }

                else -> {
                    contacts.map { toUiModelForEvent(it, today, EventType.BIRTHDAY) }
                }
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
        uniqueIdSuffix: Boolean,
    ): List<ContactUiModel> {
        val processedKeys = mutableSetOf<String>()
        val list = mutableListOf<ContactUiModel>()
        val contactMap = contacts.associateBy { it.lookupKey }

        for (contact in contacts) {
            if (contact.anniversary == null) continue
            if (processedKeys.contains(contact.lookupKey)) continue

            val spouseKey = contact.spouseLookupKey
            val spouse = if (spouseKey != null) contactMap[spouseKey] else null

            if (spouse?.anniversary != null) {
                processedKeys.add(contact.lookupKey)
                processedKeys.add(spouse.lookupKey)

                val uiModelA = toUiModelForEvent(contact, today, EventType.ANNIVERSARY)
                val uiModelB = toUiModelForEvent(spouse, today, EventType.ANNIVERSARY)

                val mergedId = if (uniqueIdSuffix) {
                    "${contact.lookupKey}_${spouse.lookupKey}_${ContactLabels.LABEL_ANNIVERSARY}"
                } else {
                    "${contact.lookupKey}_${spouse.lookupKey}"
                }

                val mergedModel = ContactUiModel(
                    id = mergedId,
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
                    secondLookupKey = spouse.lookupKey,
                    secondImageUri = spouse.imageUri,
                    secondInitials = uiModelB.initials,
                    secondFullName = spouse.fullName,
                    isCouple = true,
                )
                list.add(mergedModel)
            } else {
                processedKeys.add(contact.lookupKey)
                val single = toUiModelForEvent(contact, today, EventType.ANNIVERSARY).let { model ->
                    if (uniqueIdSuffix) {
                        model.copy(id = "${contact.lookupKey}_${ContactLabels.LABEL_ANNIVERSARY}")
                    } else {
                        model
                    }
                }
                list.add(if (mergeLabels) single else single.copy(labels = emptyList()))
            }
        }
        return list
    }
}
