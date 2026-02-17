package com.heckmannch.birthdaybuddy.utils

// Hilfsfunktion: Macht aus "1990-05-25" ein schönes "25.05.1990"
fun formatBirthdayGerman(dateString: String): String {
    if (dateString.isEmpty()) return "Unbekannt"

    return try {
        if (dateString.startsWith("--")) {
            // Fall 1: Kein Jahr bekannt (z.B. "--05-25")
            val month = dateString.substring(2, 4)
            val day = dateString.substring(5, 7)
            "$day.$month."
        } else {
            // Fall 2: Normales Datum (z.B. "1990-05-25")
            val parts = dateString.split("-")
            if (parts.size == 3) {
                "${parts[2]}.${parts[1]}.${parts[0]}" // Tag.Monat.Jahr
            } else {
                dateString
            }
        }
    } catch (e: Exception) {
        dateString // Falls das Format ganz komisch ist, geben wir es einfach unformatiert zurück
    }
}