package com.heckmannch.birthdaybuddy.data.local

/**
 * Datenmodell für ein potenzielles Ehepaar, das aus der Datenbank geladen wird.
 * Repräsentiert zwei Kontakte mit dem gleichen Hochzeitstag (Tag & Monat).
 */
data class PotentialCouple(
    val firstLookupKey: String,
    val firstName: String,
    val firstImageUri: String?,
    val secondLookupKey: String,
    val secondName: String,
    val secondImageUri: String?
)
