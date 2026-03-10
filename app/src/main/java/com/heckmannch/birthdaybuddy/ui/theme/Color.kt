package com.heckmannch.birthdaybuddy.ui.theme

import androidx.compose.ui.graphics.Color

// Standard Theme Farben
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// App Background Farben
val AppBackgroundLight = Color(0xFFFEFBFF)
val AppBackgroundDark = Color(0xFF1B1B1F)

// Einstellungen (Settings) Farben
val SettingsPillBackgroundLight = Color(0xFFF3F4F9)
val SettingsPillBackgroundDark = Color(0xFF1E1F22)

val SettingsColorOrganisation = Color(0xFF4285F4) // Google Blue
val SettingsColorNotifications = Color(0xFFFBBC04) // Google Yellow
val SettingsColorDisplay = Color(0xFF4CAF50) // Ein schönes Grün für "Anzeige & Filter"

/**
 * Zentrale Verwaltung der Farben für Geburtstags-Events.
 * Wird sowohl in der App-Liste als auch im Widget verwendet.
 */
object BirthdayColors {
    // Basis Farben (Highlights)
    val Gold = Color(0xFFFFD700)
    val GoldSecondary = Color(0xFFFBC02D)
    val Silver = Color(0xFFC0C0C0)
    val SilverSecondary = Color(0xFFE0E0E0)
    
    // Kinder-Farben (Regenbogen)
    val KidColors = listOf(
        Color(0xFF4285F4), // Blue
        Color(0xFFF06292), // Pink
        Color(0xFFFFB300), // Amber
        Color(0xFF4CAF50)  // Green
    )
    val KidPrimary = Color(0xFF4285F4)
    
    // Messenger Farben
    val WhatsApp = Color(0xFF25D366)
    val Signal = Color(0xFF3A76F0)
    val Telegram = Color(0xFF0088CC)

    // Container Farben (Heute) - Light Theme
    val GoldContainerLight = Color(0xFFFFFDE7)
    val SilverContainerLight = Color(0xFFF5F5F5)
    val KidContainerLight = Color(0xFFE3F2FD)

    // Container Farben (Heute) - Dark Theme
    val GoldContainerDark = Color(0xFF332D00)
    val SilverContainerDark = Color(0xFF2C2C2C)
    val KidContainerDark = Color(0xFF0D1B2A)

    // Text-Farben für Status-Labels (Heute)
    val GoldTextDark = Color(0xFF827717)
    val SilverTextDark = Color(0xFF616161)
    val KidTextDark = Color(0xFF1976D2)

    // Verlauf-Farben für Alter
    fun ageNear(isDark: Boolean) = if (isDark) Color(0xFFFFA4A4) else Color(0xFFFF5252)
    fun ageFar(isDark: Boolean) = if (isDark) Color(0xFFD32F2F) else Color(0xFF8B0000)

    // Verlauf-Farben für Tage
    fun daysNear(isDark: Boolean) = if (isDark) Color(0xFF80D8FF) else Color(0xFF00BFFF)
    fun daysFar(isDark: Boolean) = if (isDark) Color(0xFF5C6BC0) else Color(0xFF00008B)
}
