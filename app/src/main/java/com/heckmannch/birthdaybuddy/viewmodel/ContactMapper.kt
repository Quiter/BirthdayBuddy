package com.heckmannch.birthdaybuddy.viewmodel

import com.heckmannch.birthdaybuddy.database.Contact
import com.heckmannch.birthdaybuddy.util.toNextOccurrence
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Hilfsklasse zur Umwandlung von Datenbank-Modellen in UI-Modelle.
 */
class ContactMapper @Inject constructor() {
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    private val dayMonthFormatter = DateTimeFormatter.ofPattern("d. MMMM")
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM")

    fun toUiModel(contact: Contact, today: LocalDate): ContactUiModel {
        val hasYear = contact.birthday.year != 1900
        val nextBirthday = contact.birthday.toNextOccurrence(today)
        val daysLeft = ChronoUnit.DAYS.between(today, nextBirthday)
        val nextAgeValue = if (hasYear) nextBirthday.year - contact.birthday.year else null

        return ContactUiModel(
            id = contact.lookupKey, 
            contactId = contact.contactId,
            lookupKey = contact.lookupKey,
            fullName = contact.fullName,
            dateText = if (!hasYear) contact.birthday.format(dayMonthFormatter) else contact.birthday.format(dateFormatter),
            monthName = contact.birthday.format(monthFormatter),
            imageUri = contact.imageUri,
            initials = contact.fullName.take(1).uppercase(),
            nextAge = nextAgeValue,
            daysUntilNext = daysLeft,
            isToday = daysLeft == 0L,
            labels = contact.labels,
            giftIdeas = GiftIdea.fromString(contact.giftIdeas),
        )
    }
}
