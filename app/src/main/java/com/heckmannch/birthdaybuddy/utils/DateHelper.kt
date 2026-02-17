package com.heckmannch.birthdaybuddy.utils

// Übersetzt das amerikanische ISO-Datum (YYYY-MM-DD) in DD.MM.YYYY
fun formatGermanDate(dateString: String): String {
    return try {
        if (dateString.startsWith("--")) {
            val month = dateString.substring(2, 4)
            val day = dateString.substring(5, 7)
            "$day.$month."
        } else {
            val parts = dateString.split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1]
                val day = parts[2]
                "$day.$month.$year"
            } else {
                dateString
            }
        }
    } catch (e: Exception) {
        dateString
    }
}

// Berechnet das Alter und die verbleibenden Tage bis zum nächsten Geburtstag
fun calculateAgeAndDays(birthDateString: String): Pair<Int, Int> {
    if (birthDateString.isEmpty()) return Pair(0, 0)

    return try {
        val today = java.time.LocalDate.now()

        if (birthDateString.startsWith("--")) {
            val month = birthDateString.substring(2, 4).toInt()
            val day = birthDateString.substring(5, 7).toInt()

            var nextBday = java.time.LocalDate.of(today.year, month, day)
            if (nextBday.isBefore(today)) {
                nextBday = nextBday.plusYears(1)
            }

            val days = java.time.temporal.ChronoUnit.DAYS.between(today, nextBday).toInt()
            return Pair(0, days)
        } else {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val birthDate = java.time.LocalDate.parse(birthDateString, formatter)

            var turnsAge = today.year - birthDate.year
            var nextBday = birthDate.withYear(today.year)

            if (nextBday.isBefore(today)) {
                nextBday = nextBday.plusYears(1)
                turnsAge += 1
            }

            val days = java.time.temporal.ChronoUnit.DAYS.between(today, nextBday).toInt()
            return Pair(turnsAge, days)
        }
    } catch (e: Exception) {
        Pair(0, 0)
    }
}