package com.heckmannch.birthdaybuddy.util

import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.temporal.ChronoUnit

// --- Robuste Extensions für Datumsberechnungen ---

/**
 * Berechnet die Tage bis zum nächsten Vorkommen dieses Datums.
 */
fun LocalDate.safeDaysUntilNext(today: LocalDate = LocalDate.now()): Long {
    return ChronoUnit.DAYS.between(today, toNextOccurrence(today))
}

/**
 * Berechnet das Alter, das die Person am nächsten Geburtstag erreicht.
 */
fun LocalDate.safeNextAge(today: LocalDate = LocalDate.now()): Int {
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

/**
 * Projiziert ein Datum auf ein Zieljahr und korrigiert den 29. Februar auf den 28., 
 * falls das Zieljahr kein Schaltjahr ist.
 */
fun LocalDate.toYear(targetYear: Int): LocalDate {
    return if (this.month == Month.FEBRUARY && this.dayOfMonth == 29 && !Year.isLeap(targetYear.toLong())) {
        LocalDate.of(targetYear, Month.FEBRUARY, 28)
    } else {
        this.withYear(targetYear)
    }
}
