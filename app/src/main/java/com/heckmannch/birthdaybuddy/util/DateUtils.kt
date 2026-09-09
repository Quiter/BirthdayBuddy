package com.heckmannch.birthdaybuddy.util

import com.heckmannch.birthdaybuddy.domain.util.NO_YEAR_MARKER
import com.heckmannch.birthdaybuddy.domain.util.toYear
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// --- Robuste Extensions für Datumsberechnungen ---

/**
 * Prüft, ob ein Datum ein gültiges Geburtsjahr enthält.
 */
val LocalDate.hasYear: Boolean get() = this.year != NO_YEAR_MARKER

/**
 * Prüft, ob die Person heute Geburtstag hat.
 */
fun LocalDate.isBirthdayToday(today: LocalDate = LocalDate.now()): Boolean {
    return safeDaysUntilNext(today) == 0L
}

/**
 * Berechnet die Tage bis zum nächsten Vorkommen dieses Datums.
 */
fun LocalDate.safeDaysUntilNext(today: LocalDate = LocalDate.now()): Long {
    return ChronoUnit.DAYS.between(today, toNextOccurrence(today))
}

/**
 * Berechnet das Alter, das die Person am nächsten Geburtstag erreicht.
 * Gibt null zurück, wenn kein Geburtsjahr bekannt ist.
 */
fun LocalDate.safeNextAge(today: LocalDate = LocalDate.now()): Int? {
    if (!hasYear) return null
    return toNextOccurrence(today).year - this.year
}

/**
 * Hilfsfunktion um das nächste Vorkommen eines Datums zu finden (Handling für 29. Feb).
 */
fun LocalDate.toNextOccurrence(today: LocalDate): LocalDate {
    val next = this.toYear(today.year)
    return if (next.isBefore(today)) {
        this.toYear(today.year + 1)
    } else {
        next
    }
}

