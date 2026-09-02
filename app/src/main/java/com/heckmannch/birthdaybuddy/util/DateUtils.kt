package com.heckmannch.birthdaybuddy.util

import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.temporal.ChronoUnit

// --- Robuste Extensions für Datumsberechnungen ---

/**
 * Standard-Jahr für Kontakte ohne hinterlegtes Geburtsjahr in Android.
 * Muss ein valides Schaltjahr sein (z.B. Jahr 4), damit der 29. Februar als
 * LocalDate ohne Exception repräsentiert werden kann.
 */
const val NO_YEAR_MARKER = 4

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
 * Validiert und normalisiert rohe Datumsbestandteile eines Geburtstages in ein sicheres [LocalDate].
 *
 * Behandelt fehlende Jahre durch Anwenden von [NO_YEAR_MARKER] (Schaltjahr-Repräsentation für 29. Feb),
 * beschränkt den Monat auf den Bereich 1..12 und begrenzt den Tag auf die maximale Anzahl von Tagen
 * für den angegebenen Monat und das jeweilige Jahr.
 *
 * @param year Optionales Geburtsjahr oder `null` / [NO_YEAR_MARKER] falls unbekannt.
 * @param month Geburtsmonat (1..12).
 * @param day Geburtstag (1..31).
 * @return Validierte und angepasste [LocalDate]-Instanz.
 */
fun sanitizeBirthdayDate(year: Int?, month: Int, day: Int): LocalDate {
    val targetYear = if (year != null && year > 0 && year != NO_YEAR_MARKER) year else NO_YEAR_MARKER
    val safeMonth = month.coerceIn(1, 12)
    val maxDays = Month.of(safeMonth).length(Year.isLeap(targetYear.toLong()))
    val safeDay = day.coerceIn(1, maxDays)
    return LocalDate.of(targetYear, safeMonth, safeDay)
}
