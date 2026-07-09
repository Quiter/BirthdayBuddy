package com.heckmannch.birthdaybuddy.data.mapper

import android.text.format.DateFormat
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.ui.model.BirthdayTier
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.util.getInitials
import com.heckmannch.birthdaybuddy.util.hasYear
import com.heckmannch.birthdaybuddy.util.isBirthdayToday
import com.heckmannch.birthdaybuddy.util.safeDaysUntilNext
import com.heckmannch.birthdaybuddy.util.safeNextAge
import dagger.Reusable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Helper class for converting database models into UI models.
 */
@Reusable
class ContactMapper @Inject constructor() {
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
            hasWhatsApp = contact.hasWhatsApp,
            hasSignal = contact.hasSignal,
            labels = contact.labels,
            giftIdeas = contact.giftIdeas,
            birthday = contact.birthday,
            birthdayTier = BirthdayTier.from(nextAgeValue),
        )
    }
}
