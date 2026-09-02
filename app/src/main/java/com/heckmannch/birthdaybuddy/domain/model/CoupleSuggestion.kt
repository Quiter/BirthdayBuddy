package com.heckmannch.birthdaybuddy.domain.model

/**
 * Domain model representing a couple suggestion.
 * This class is free of any Android or UI dependencies, satisfying Clean Architecture
 * guidelines by keeping the domain layer decoupled from presentation concerns.
 *
 * @property firstLookupKey Unique identifier for the first contact.
 * @property firstName The full name of the first contact.
 * @property firstImageUri The URI string pointing to the first contact's picture, if any.
 * @property secondLookupKey Unique identifier for the second contact.
 * @property secondName The full name of the second contact.
 * @property secondImageUri The URI string pointing to the second contact's picture, if any.
 */
data class CoupleSuggestion(
    val firstLookupKey: String,
    val firstName: String,
    val firstImageUri: String?,
    val secondLookupKey: String,
    val secondName: String,
    val secondImageUri: String?,
)
