package com.heckmannch.birthdaybuddy.util

private val WHITESPACE_REGEX = "\\s+".toRegex()

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

/**
 * Berechnet Initialen aus einem Namen (z.B. "Max Mustermann" → "MM").
 */
fun String.getInitials(): String {
    val parts = this.trim().split(WHITESPACE_REGEX).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts.first().take(1).uppercase()
        else -> "${parts.first().take(1)}${parts.last().take(1)}".uppercase()
    }
}
