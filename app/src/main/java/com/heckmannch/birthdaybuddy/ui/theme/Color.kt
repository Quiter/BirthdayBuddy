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

// WIDGET FARBEN (Zentral für einfaches Testing)
// Hier kannst du die Werte anpassen, um das Design zu verändern.
object WidgetColors {
    val Gold = Color(0xFFFFD700)
    val Silver = Color(0xFFE0E0E0) // Etwas heller als vorher für mehr Kontrast
    val KidBirthday = Color(0xFF0CD0BD) // Ein schönes Blau
    val TodayDefault = Color(0xFF4CAF50) // Ein frisches Grün für "normale" Geburtstage heute
    
    // Farbe für den Kreis, wenn der Geburtstag NICHT heute ist
    val UpcomingCircle = Color(0xFF473670)
}
