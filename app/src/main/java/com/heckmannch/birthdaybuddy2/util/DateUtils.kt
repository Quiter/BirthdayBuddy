package com.heckmannch.birthdaybuddy2.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

// --- Robuste Extensions für Datumsberechnungen ---

fun LocalDate.safeDaysUntilNext(): Long {
    val today = LocalDate.now()
    return ChronoUnit.DAYS.between(today, toNextOccurrence(today))
}

fun LocalDate.safeNextAge(): Int {
    val today = LocalDate.now()
    return toNextOccurrence(today).year - this.year
}

/**
 * Hilfsfunktion um das nächste Vorkommen eines Datums zu finden (Handling für 29. Feb).
 */
fun LocalDate.toNextOccurrence(today: LocalDate): LocalDate {
    var next = this.toYear(today.year)
    if (next.isBefore(today)) {
        next = this.toYear(today.year + 1)
    }
    return next
}

fun LocalDate.toYear(targetYear: Int): LocalDate {
    return if ((this.monthValue == 2) && (this.dayOfMonth == 29) && !java.time.Year.isLeap(targetYear.toLong())) {
        LocalDate.of(targetYear, 2, 28)
    } else {
        this.withYear(targetYear)
    }
}
