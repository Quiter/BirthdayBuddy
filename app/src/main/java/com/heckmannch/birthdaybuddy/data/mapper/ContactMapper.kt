package com.heckmannch.birthdaybuddy.data.mapper

import android.text.format.DateFormat
import com.heckmannch.birthdaybuddy.data.local.Contact
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
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
 * Hilfsklasse zur Umwandlung von Datenbank-Modellen in UI-Modelle.
 */
@Reusable
class ContactMapper @Inject constructor() {
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

    // Erzeugt das beste Format NUR für Tag/Monat basierend auf der Sprache des Nutzers (z.B. "12. Mai" vs "May 12")
    private val dayMonthFormatter = DateTimeFormatter.ofPattern(
        DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMM"),
        Locale.getDefault(),
    )

    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())

    fun toUiModel(contact: Contact, today: LocalDate): ContactUiModel {
        return toUiModelForEvent(contact, today, "birthday")
    }

    fun toUiModelForEvent(contact: Contact, today: LocalDate, eventType: String): ContactUiModel {
        val eventDate = when (eventType) {
            "anniversary" -> contact.anniversary
            "name_day" -> contact.nameDay
            else -> contact.birthday
        }
        val hasYear = eventDate?.hasYear ?: false
        val daysLeft = eventDate?.safeDaysUntilNext(today) ?: Long.MAX_VALUE
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
            initials = contact.fullName.trim()
                .split(WHITESPACE_REGEX)
                .filter { it.isNotBlank() }
                .let { parts ->
                    when {
                        parts.isEmpty() -> "?"
                        parts.size == 1 -> parts.first().take(1).uppercase()
                        else -> "${parts.first().take(1)}${parts.last().take(1)}".uppercase()
                    }
                },
            nextAge = nextAgeValue,
            daysUntilNext = daysLeft,
            isToday = eventDate?.isBirthdayToday(today) ?: false,
            hasWhatsApp = contact.hasWhatsApp,
            hasSignal = contact.hasSignal,
            labels = contact.labels,
            giftIdeas = contact.giftIdeas,
            birthday = contact.birthday,
        )
    }

    companion object {
        private val WHITESPACE_REGEX = "\\s+".toRegex()
    }
}
