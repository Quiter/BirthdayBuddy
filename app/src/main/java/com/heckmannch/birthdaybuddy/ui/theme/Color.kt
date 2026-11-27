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

// Action / Brand Colors
val PixelBlue = Color(0xFF1A73E8)

// Birthday Colors
val BirthdayGold = Color(0xFFFFD700)
val BirthdayGoldEnd = Color(0xFFFFB700)
val BirthdayGoldLight = Color(0xFFC67C00)
val BirthdaySilver = Color(0xFFC0C0C0)
val BirthdaySilverEnd = Color(0xFFB0BEC5)
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

// Glass Card Base Colors
val GlassLightContentColor = Color.White
val GlassDarkContentColor = Color.Black
val GlassHighlightColor = Color.White
val GlassShadowColor = Color.Black

// Birthday Card Gradients
val BirthdayGoldGradient = listOf(BirthdayGold, BirthdayGoldEnd)
val BirthdaySilverGradient = listOf(BirthdaySilver, BirthdaySilverEnd)
val BirthdayKidGradient = KidColors

// Label Card Status Colors (Light & Dark tuned for WCAG contrast with GlassCardStyle)
val LabelIgnoredLightStart = Color(0xFFFFB4AB)
val LabelIgnoredLightEnd = Color(0xFFFFDAD6)
val LabelIgnoredDarkStart = Color(0xFFB71C1C)
val LabelIgnoredDarkEnd = Color(0xFF7F1D1D)

val LabelHiddenLightStart = Color(0xFFBDBDBD)
val LabelHiddenLightEnd = Color(0xFFD6D6D6)
val LabelHiddenDarkStart = Color(0xFF424242)
val LabelHiddenDarkEnd = Color(0xFF2E2E2E)

val birthdayGoldColor: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) BirthdayGold else BirthdayGoldLight

val birthdayKidAmberColor: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) BirthdayKidAmber else BirthdayKidAmberLight

/**
 * Default neutral frosted glass card gradient used across Home, Labels, and Settings.
 */
@Composable
fun defaultCardGradient(): List<Color> = listOf(
    MaterialTheme.colorScheme.surfaceContainerHigh,
    MaterialTheme.colorScheme.surfaceContainerLow
)

/**
 * Gradient used for regular birthdays occurring today.
 */
@Composable
fun birthdayRegularTodayCardGradient(): List<Color> = listOf(
    MaterialTheme.colorScheme.primaryContainer,
    MaterialTheme.colorScheme.surfaceContainerHigh
)

/**
 * Gradient used for ignored label cards (red).
 */
@Composable
fun labelIgnoredCardGradient(): List<Color> =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        listOf(LabelIgnoredDarkStart, LabelIgnoredDarkEnd)
    } else {
        listOf(LabelIgnoredLightStart, LabelIgnoredLightEnd)
    }

/**
 * Gradient used for hidden label cards (gray).
 */
@Composable
fun labelHiddenCardGradient(): List<Color> =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        listOf(LabelHiddenDarkStart, LabelHiddenDarkEnd)
    } else {
        listOf(LabelHiddenLightStart, LabelHiddenLightEnd)
    }

/**
 * Gradient used for active/selected label cards when neither ignored nor hidden.
 */
@Composable
fun labelActiveCardGradient(): List<Color> = listOf(
    MaterialTheme.colorScheme.primaryContainer,
    MaterialTheme.colorScheme.secondaryContainer
)


