package com.heckmannch.birthdaybuddy.model

data class BirthdayContact(
    val name: String,
    val birthday: String, // Später wandeln wir das in ein echtes Datum um
    val labels: List<String>, // Geändert von String zu List<String>
    val remainingDays: Int,
    val age: Int
)