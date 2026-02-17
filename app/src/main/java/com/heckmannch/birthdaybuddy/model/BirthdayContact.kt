package com.heckmannch.birthdaybuddy.model

data class BirthdayContact(
    val name: String,
    val birthday: String, // Später wandeln wir das in ein echtes Datum um
    val label: String,    // Für deine Kategorien (Privat, Arbeit, etc.)
    val remainingDays: Int,
    val age: Int
)