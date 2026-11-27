package com.heckmannch.birthdaybuddy.util

@Suppress("RegExpRedundantEscape")
private val PARENTHESES_REGEX = "\\(.*?\\)|\\[.*?\\]|\\{.*?\\}".toRegex()
private val WHITESPACE_REGEX = "\\s+".toRegex()

/**
 * Kombiniert zwei Namen zu einem Paar-Namen (z.B. "Max & Erika Mustermann").
 */
fun mergeNames(name1: String, name2: String): String {
    val parts1 = name1.trim().split(WHITESPACE_REGEX)
    val parts2 = name2.trim().split(WHITESPACE_REGEX)
    if ((parts1.size > 1) && (parts2.size > 1) && parts1.last()
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
 * Ignoriert Namenszusätze in Klammern (z.B. "(Muster)" oder "(Dr.)"), entfernt Sonderzeichen
 * und gibt ausschließlich gültige Buchstaben ohne Sonderzeichen zurück.
 */
fun String.getInitials(): String {
    val textWithoutParentheses = this.replace(PARENTHESES_REGEX, " ")
    val words = textWithoutParentheses
        .trim()
        .split(WHITESPACE_REGEX)
        .asSequence()
        .map { word -> word.filter { it.isLetter() } }
        .filter { it.isNotBlank() }
        .toList()

    return when {
        words.isEmpty() -> ""
        words.size == 1 -> words.first().take(1).uppercase()
        else -> "${words.first().take(1)}${words.last().take(1)}".uppercase()
    }
}
