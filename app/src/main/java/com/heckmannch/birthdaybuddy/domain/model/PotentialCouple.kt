package com.heckmannch.birthdaybuddy.domain.model

/**
 * Pure domain model representing a potential couple suggestions based on wedding anniversaries.
 */
data class PotentialCouple(
    val firstLookupKey: String,
    val firstName: String,
    val firstImageUri: String?,
    val secondLookupKey: String,
    val secondName: String,
    val secondImageUri: String?
)
