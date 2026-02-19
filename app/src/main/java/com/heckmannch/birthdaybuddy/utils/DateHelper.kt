package com.heckmannch.birthdaybuddy.utils

import java.time.LocalDate
import java.time.MonthDay
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Definition der Zeit-Formatierer für ISO und die deutsche Darstellung
private val ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val GERMAN_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val GERMAN_NO_YEAR_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.")

/**
 * Wandelt ein Datum aus dem internen Format (ISO) in ein lesbares deutsches Format um.
 * Berücksichtigt dabei auch Datumsangaben ohne Jahr (Format "--MM-DD").
 *
 * @param dateString Das Datum als String (z.B. "1990-05-15" oder "--05-15").
 * @return Das formatierte Datum (z.B. "15.05.1990" oder "15.05.").
 */
fun formatGermanDate(dateString: String): String {
    if (dateString.isBlank()) return ""
    return try {
        if (dateString.startsWith("--")) {
            // Fall: Kein Jahr angegeben (häufig bei Kontakten ohne Geburtsjahr)
            val monthDay = MonthDay.parse(dateString)
            monthDay.format(GERMAN_NO_YEAR_FORMATTER)
        } else {
            // Fall: Vollständiges Datum mit Jahr
            val date = LocalDate.parse(dateString, ISO_FORMATTER)
            date.format(GERMAN_FORMATTER)
        }
    } catch (e: Exception) {
        // Fallback: Wenn das Parsen fehlschlägt, geben wir den Original-String zurück
        dateString
    }
}

/**
 * Berechnet zwei wichtige Werte für die App:
 * 1. Das Alter, das die Person an ihrem nächsten Geburtstag erreicht.
 * 2. Die Anzahl der Tage, bis dieser Geburtstag stattfindet.
 *
 * @param birthDateString Das Geburtsdatum im ISO-Format.
 * @return Ein Pair bestehend aus (Alter, verbleibende Tage). 
 *         Wenn das Alter unbekannt ist (kein Jahr im Kontakt), wird -1 zurückgegeben.
 */
fun calculateAgeAndDays(birthDateString: String): Pair<Int, Int> {
    if (birthDateString.isBlank()) return Pair(-1, 0)
    
    return try {
        val today = LocalDate.now()
        
        if (birthDateString.startsWith("--")) {
            // Logik für Kontakte OHNE Geburtsjahr
            val monthDay = MonthDay.parse(birthDateString)
            var nextBirthday = monthDay.atYear(today.year)
            
            // Wenn der Geburtstag dieses Jahr schon war, planen wir für das nächste Jahr
            if (nextBirthday.isBefore(today)) {
                nextBirthday = nextBirthday.plusYears(1)
            }
            
            val days = ChronoUnit.DAYS.between(today, nextBirthday).toInt()
            Pair(-1, days) // Alter ist unbekannt, daher -1
        } else {
            // Logik für Kontakte MIT Geburtsjahr
            val birthDate = LocalDate.parse(birthDateString, ISO_FORMATTER)
            var nextBirthday = birthDate.withYear(today.year)
            
            // Initiales Alter (Differenz der Jahre)
            var age = today.year - birthDate.year
            
            // Wenn der Geburtstag dieses Jahr noch kommt oder heute ist, passt 'age'.
            // Wenn er bereits war, erhöhen wir das Alter für den NÄCHSTEN Geburtstag.
            if (nextBirthday.isBefore(today)) {
                nextBirthday = nextBirthday.plusYears(1)
                age++
            }
            
            val days = ChronoUnit.DAYS.between(today, nextBirthday).toInt()
            Pair(age, days)
        }
    } catch (e: Exception) {
        Pair(-1, 0)
    }
}
