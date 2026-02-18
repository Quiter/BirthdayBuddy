package com.heckmannch.birthdaybuddy.utils

import java.time.LocalDate
import java.time.MonthDay
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val GERMAN_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val GERMAN_NO_YEAR_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.")

/**
 * Formatiert ein Datum von ISO (yyyy-MM-dd oder --MM-DD) in das deutsche Format.
 */
fun formatGermanDate(dateString: String): String {
    if (dateString.isBlank()) return ""
    return try {
        if (dateString.startsWith("--")) {
            val monthDay = MonthDay.parse(dateString)
            monthDay.format(GERMAN_NO_YEAR_FORMATTER)
        } else {
            val date = LocalDate.parse(dateString, ISO_FORMATTER)
            date.format(GERMAN_FORMATTER)
        }
    } catch (e: Exception) {
        dateString
    }
}

/**
 * Berechnet das Alter (beim nächsten Geburtstag) und die verbleibenden Tage.
 * Pair(Alter, TageBisGeburtstag)
 */
fun calculateAgeAndDays(birthDateString: String): Pair<Int, Int> {
    if (birthDateString.isBlank()) return Pair(0, 0)
    
    return try {
        val today = LocalDate.now()
        
        if (birthDateString.startsWith("--")) {
            val monthDay = MonthDay.parse(birthDateString)
            var nextBirthday = monthDay.atYear(today.year)
            
            if (nextBirthday.isBefore(today)) {
                nextBirthday = nextBirthday.plusYears(1)
            }
            
            val days = ChronoUnit.DAYS.between(today, nextBirthday).toInt()
            Pair(0, days) // Jahr unbekannt -> Alter 0
        } else {
            val birthDate = LocalDate.parse(birthDateString, ISO_FORMATTER)
            var nextBirthday = birthDate.withYear(today.year)
            var age = today.year - birthDate.year
            
            if (nextBirthday.isBefore(today)) {
                nextBirthday = nextBirthday.plusYears(1)
                age++
            }
            
            val days = ChronoUnit.DAYS.between(today, nextBirthday).toInt()
            Pair(age, days)
        }
    } catch (e: Exception) {
        Pair(0, 0)
    }
}
