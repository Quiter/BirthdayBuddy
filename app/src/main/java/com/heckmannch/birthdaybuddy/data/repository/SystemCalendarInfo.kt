package com.heckmannch.birthdaybuddy.data.repository

/**
 * Modell zur Repräsentation von Kalender-Metadaten aus dem Android-Systemkalender,
 * entkoppelt von cursorbasierten Implementierungen für bessere Testbarkeit.
 */
data class SystemCalendarInfo(
    val id: Long,
    val name: String,
    val accountName: String,
    val accountType: String,
    val displayName: String?,
    val visible: Int
)
