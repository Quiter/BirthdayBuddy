package com.heckmannch.birthdaybuddy.utils

import java.time.LocalDate
import java.time.MonthDay
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val GERMAN_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val GERMAN_NO_YEAR_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.")

/**
 * Erweitert den String (ISO-Datum) um eine Formatierung ins deutsche Format.
 * Nutzt 'this', um direkt auf dem String aufgerufen zu werden: birthday.toGermanDate()
 */
fun String.toGermanDate(): String {
    if (this.isBlank()) return ""
    return try {
        if (this.startsWith("--")) {
            MonthDay.parse(this).format(GERMAN_NO_YEAR_FORMATTER)
        } else {
            LocalDate.parse(this, ISO_FORMATTER).format(GERMAN_FORMATTER)
        }
    } catch (_: Exception) {
        this
    }
}

/**
 * Berechnet Alter und verbleibende Tage direkt aus dem ISO-String.
 * Rückgabe: Pair(Alter am nächsten Geburtstag, Tage bis dahin)
 */
fun String.calculateAgeAndDays(): Pair<Int, Int> {
    if (this.isBlank()) return Pair(-1, 0)
    
    return try {
        val today = LocalDate.now()
        
        if (this.startsWith("--")) {
            val monthDay = MonthDay.parse(this)
            var nextBirthday = monthDay.atYear(today.year)
            
            if (nextBirthday.isBefore(today)) {
                nextBirthday = nextBirthday.plusYears(1)
            }
            
            val days = ChronoUnit.DAYS.between(today, nextBirthday).toInt()
            Pair(-1, days) 
        } else {
            val birthDate = LocalDate.parse(this, ISO_FORMATTER)
            var nextBirthday = birthDate.withYear(today.year)
            var age = today.year - birthDate.year
            
            if (nextBirthday.isBefore(today)) {
                nextBirthday = nextBirthday.plusYears(1)
                age++
            }
            
            val days = ChronoUnit.DAYS.between(today, nextBirthday).toInt()
            Pair(age, days)
        }
    } catch (_: Exception) {
        Pair(-1, 0)
    }
}
