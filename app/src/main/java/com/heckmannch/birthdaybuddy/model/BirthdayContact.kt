package com.heckmannch.birthdaybuddy.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Repräsentiert einen Geburtstagskontakt in der lokalen Datenbank.
 */
@Entity(tableName = "birthdays")
@Serializable
data class BirthdayContact(
    @PrimaryKey val id: String,
    val name: String,
    val birthday: String,
    val labels: List<String>,
    val remainingDays: Int,
    val age: Int,
    val actions: ContactActions = ContactActions(),
    val photoUri: String? = null
)

@Serializable
data class ContactActions(
    val phoneNumber: String? = null,
    val email: String? = null,
    val hasWhatsApp: Boolean = false,
    val hasSignal: Boolean = false,
    val hasTelegram: Boolean = false
)
