package com.heckmannch.birthdaybuddy.data.mapper

import com.heckmannch.birthdaybuddy.data.local.Contact
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.GiftIdea
import com.heckmannch.birthdaybuddy.util.hasYear
import com.heckmannch.birthdaybuddy.util.isBirthdayToday
import com.heckmannch.birthdaybuddy.util.safeDaysUntilNext
import com.heckmannch.birthdaybuddy.util.safeNextAge
import android.text.format.DateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Hilfsklasse zur Umwandlung von Datenbank-Modellen in UI-Modelle.
 */
class ContactMapper @Inject constructor() {
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    
    // Erzeugt das beste Format NUR für Tag/Monat basierend auf der Sprache des Nutzers (z.B. "12. Mai" vs "May 12")
    private val dayMonthFormatter = DateTimeFormatter.ofPattern(
        DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMM"),
        Locale.getDefault(),
    )
    
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())

    fun toUiModel(contact: Contact, today: LocalDate): ContactUiModel {
        val birthday = contact.birthday
        val hasYear = birthday?.hasYear ?: false
        val daysLeft = birthday?.safeDaysUntilNext(today) ?: Long.MAX_VALUE
        val nextAgeValue = birthday?.safeNextAge(today)

        return ContactUiModel(
            id = contact.lookupKey, 
            contactId = contact.contactId,
            lookupKey = contact.lookupKey,
            fullName = contact.fullName,
            dateText = when {
                birthday == null -> "-"
                !hasYear -> birthday.format(dayMonthFormatter)
                else -> birthday.format(dateFormatter)
            },
            monthName = birthday?.format(monthFormatter) ?: "",
            imageUri = contact.imageUri,
            phoneNumber = contact.phoneNumber,
            initials = contact.fullName.take(1).ifBlank { "?" }.uppercase(),
            nextAge = nextAgeValue,
            daysUntilNext = daysLeft,
            isToday = birthday?.isBirthdayToday(today) ?: false,
            hasWhatsApp = contact.hasWhatsApp,
            hasSignal = contact.hasSignal,
            labels = contact.labels,
            giftIdeas = GiftIdea.fromString(contact.giftIdeas),
        )
    }
}
