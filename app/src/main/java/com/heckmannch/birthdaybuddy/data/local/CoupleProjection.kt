package com.heckmannch.birthdaybuddy.data.local

/**
 * Data-layer projection representing a potential couple queried from Room.
 * Decouples the Room database layer from the domain model [com.heckmannch.birthdaybuddy.domain.model.CoupleSuggestion].
 */
data class CoupleProjection(
    val firstLookupKey: String,
    val firstName: String,
    val firstImageUri: String?,
    val secondLookupKey: String,
    val secondName: String,
    val secondImageUri: String?,
)
