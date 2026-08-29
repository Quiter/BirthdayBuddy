package com.heckmannch.birthdaybuddy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Birthday Colors
val BirthdayGold = Color(0xFFFFD700)
val BirthdayGoldLight = Color(0xFFC67C00)
val BirthdaySilver = Color(0xFFC0C0C0)
val BirthdayKidBlue = Color(0xFF4285F4)
val BirthdayKidPink = Color(0xFFF06292)
val BirthdayKidAmber = Color(0xFFFFB300)
val BirthdayKidAmberLight = Color(0xFFD97706)
val BirthdayKidGreen = Color(0xFF4CAF50)

val KidColors = listOf(
    BirthdayKidBlue,
    BirthdayKidPink,
    BirthdayKidAmber,
    BirthdayKidGreen
)

val birthdayGoldColor: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) BirthdayGold else BirthdayGoldLight

val birthdayKidAmberColor: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) BirthdayKidAmber else BirthdayKidAmberLight


