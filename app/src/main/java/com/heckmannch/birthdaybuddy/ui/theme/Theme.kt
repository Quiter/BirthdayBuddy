package com.heckmannch.birthdaybuddy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

// Eine CompositionLocal, die den tatsächlichen Theme-Status (Hell/Dunkel) speichert.
// Das verhindert, dass Komponenten isSystemInDarkTheme() nutzen müssen.
val LocalThemeIsDark = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = AppBackgroundDark,
    surface = AppBackgroundDark
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = AppBackgroundLight,
    surface = AppBackgroundLight
)

@Composable
fun BirthdayBuddyTheme(
    theme: Int = 0, // 0: System, 1: Light, 2: Dark
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Hier wird entschieden, ob die App dunkel sein soll
    val darkTheme = when(theme) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val baseScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme) {
                baseScheme.copy(background = AppBackgroundDark, surface = AppBackgroundDark)
            } else {
                baseScheme.copy(background = AppBackgroundLight, surface = AppBackgroundLight)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Wir geben den Status 'darkTheme' über den Provider an alle Kinder weiter
    CompositionLocalProvider(LocalThemeIsDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
