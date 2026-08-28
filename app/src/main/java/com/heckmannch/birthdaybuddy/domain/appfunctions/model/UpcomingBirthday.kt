package com.heckmannch.birthdaybuddy.domain.appfunctions.model

import androidx.appfunctions.AppFunctionSerializable

/**
 * A single upcoming birthday entry returned by the `getUpcomingBirthdays` function.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class UpcomingBirthday(
    /** Unique contact lookup key used to identify this contact in subsequent calls. */
    val contactId: String,
    /** Full display name of the contact (e.g. "Maria Müller"). */
    val fullName: String,
    /** Month of the birthday (1 = January, 12 = December). */
    val birthdayMonth: Int,
    /** Day of the birthday (1–31). */
    val birthdayDay: Int,
    /**
     * Four-digit birth year, or null if the contact has no year stored.
     * Contacts without a year have the sentinel value 1900 in the Android Contacts
     * provider, which is normalised to null here.
     */
    val birthdayYear: Int?,
    /**
     * Number of days until the next occurrence of this birthday from today.
     * A value of 0 means the birthday is today.
     * Maximum value equals the `withinDays` parameter passed to `getUpcomingBirthdays`.
     */
    val daysUntil: Int,
    /**
     * Age the contact will reach on their next birthday, or null if the birth year is unknown.
     * Example: a contact born in 1990 whose birthday is in 5 days would have age = current year − 1990.
     */
    val age: Int?,
)
