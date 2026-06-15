package com.heckmannch.birthdaybuddy.util

import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.temporal.ChronoUnit

// --- Robuste Extensions für Datumsberechnungen ---

private val WHITESPACE_REGEX = "\\s+".toRegex()

/**
 * Standard-Jahr für Kontakte ohne hinterlegtes Geburtsjahr in Android.
 */
const val NO_YEAR_MARKER = 1900

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

/**
 * Projiziert ein Datum auf ein Zieljahr und korrigiert den 29. Februar auf den 28., 
 * falls das Zieljahr kein Schaltjahr ist.
 */
fun LocalDate.toYear(targetYear: Int): LocalDate {
    val isLeapDay = (this.month == Month.FEBRUARY) && (this.dayOfMonth == 29)
    return if (isLeapDay && !Year.isLeap(targetYear.toLong())) {
        LocalDate.of(targetYear, Month.FEBRUARY, 28)
    } else {
        this.withYear(targetYear)
    }
}

/**
 * Kombiniert zwei Namen zu einem Paar-Namen (z.B. "Max & Erika Mustermann").
 */
fun mergeNames(name1: String, name2: String): String {
    val parts1 = name1.trim().split(WHITESPACE_REGEX)
    val parts2 = name2.trim().split(WHITESPACE_REGEX)
    if (parts1.size > 1 && parts2.size > 1 && parts1.last()
            .equals(parts2.last(), ignoreCase = true)
    ) {
        val firstName1 = parts1.dropLast(1).joinToString(" ")
        val firstName2 = parts2.dropLast(1).joinToString(" ")
        return "$firstName1 & $firstName2 ${parts1.last()}"
    }
    return "$name1 & $name2"
}
