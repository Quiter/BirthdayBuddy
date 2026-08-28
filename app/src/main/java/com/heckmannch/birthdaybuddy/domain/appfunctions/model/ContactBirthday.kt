package com.heckmannch.birthdaybuddy.domain.appfunctions.model

import androidx.appfunctions.AppFunctionSerializable

/**
 * The birthday details for a single contact, returned by the `getContactBirthday` function.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class ContactBirthday(
    /** Unique contact lookup key used to identify this contact in subsequent calls. */
    val contactId: String,
    /** Full display name of the contact (e.g. "Thomas Huber"). */
    val fullName: String,
    /**
     * Month of the birthday (1 = January, 12 = December), or null if no birthday is stored
     * for this contact.
     */
    val birthdayMonth: Int?,
    /**
     * Day of the birthday (1–31), or null if no birthday is stored for this contact.
     */
    val birthdayDay: Int?,
    /**
     * Four-digit birth year, or null if the birth year is not stored or is unknown.
     */
    val birthdayYear: Int?,
    /**
     * Number of days until the next occurrence of this birthday from today, or null if no
     * birthday date is stored for this contact.
     */
    val daysUntil: Int?,
    /**
     * Age the contact will reach on their next birthday, or null if the birth year is unknown
     * or no birthday is stored.
     */
    val age: Int?,
)
