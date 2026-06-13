package com.heckmannch.birthdaybuddy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.toColorInt

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

val ColorScheme.amoled: ColorScheme
    get() = this.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceContainer = Color(0xFF0F0F0F),
        surfaceContainerLow = Color(0xFF080808),
        surfaceContainerLowest = Color.Black,
        surfaceContainerHigh = Color(0xFF161616),
        surfaceContainerHighest = Color(0xFF222222),
    )

private fun getDynamicCustomColorScheme(seedColor: Color, darkTheme: Boolean, amoled: Boolean): ColorScheme {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seedColor.toArgb(), hsv)
    
    val h = hsv[0]
    val s = hsv[1]
    val v = hsv[2]
    
    fun fromHsv(hue: Float, sat: Float, value: Float): Color {
        val calculatedArgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f)))
        return Color(calculatedArgb)
    }
    
    val baseScheme = if (darkTheme) {
        val primary = fromHsv(h, s * 0.45f, 0.90f)
        val onPrimary = if (primary.luminance() > 0.5f) Color.Black else Color.White
        val primaryContainer = fromHsv(h, s.coerceAtLeast(0.7f), 0.35f)
        val onPrimaryContainer = fromHsv(h, s * 0.3f, 0.95f)
        
        val secondary = fromHsv(h, s * 0.25f, 0.75f)
        val onSecondary = if (secondary.luminance() > 0.5f) Color.Black else Color.White
        val secondaryContainer = fromHsv(h, s * 0.35f, 0.25f)
        val onSecondaryContainer = fromHsv(h, s * 0.25f, 0.90f)
        
        val tertiary = fromHsv((h + 60f) % 360f, s * 0.35f, 0.75f)
        val onTertiary = if (tertiary.luminance() > 0.5f) Color.Black else Color.White
        val tertiaryContainer = fromHsv((h + 60f) % 360f, s * 0.4f, 0.25f)
        val onTertiaryContainer = fromHsv((h + 60f) % 360f, s * 0.25f, 0.90f)
        
        val background = fromHsv(h, s * 0.12f, 0.08f)
        val onBackground = Color(0xFFE6E1E5)
        
        val surface = background
        val onSurface = onBackground
        val surfaceVariant = fromHsv(h, s * 0.18f, 0.16f)
        val onSurfaceVariant = fromHsv(h, s * 0.25f, 0.85f)
        
        val outline = fromHsv(h, s * 0.2f, 0.55f)
        
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline
        )
    } else {
        val primary = fromHsv(h, s.coerceAtLeast(0.65f).coerceAtMost(0.85f), v.coerceAtMost(0.75f))
        val onPrimary = if (primary.luminance() > 0.5f) Color.Black else Color.White
        val primaryContainer = fromHsv(h, s * 0.25f, 0.94f)
        val onPrimaryContainer = fromHsv(h, s.coerceAtLeast(0.75f), 0.25f)
        
        val secondary = fromHsv(h, s * 0.35f, v * 0.75f)
        val onSecondary = if (secondary.luminance() > 0.5f) Color.Black else Color.White
        val secondaryContainer = fromHsv(h, s * 0.12f, 0.95f)
        val onSecondaryContainer = fromHsv(h, s * 0.7f, 0.25f)
        
        val tertiary = fromHsv((h + 60f) % 360f, s * 0.4f, v * 0.75f)
        val onTertiary = if (tertiary.luminance() > 0.5f) Color.Black else Color.White
        val tertiaryContainer = fromHsv((h + 60f) % 360f, s * 0.15f, 0.95f)
        val onTertiaryContainer = fromHsv((h + 60f) % 360f, s * 0.7f, 0.25f)
        
        val background = fromHsv(h, s * 0.04f, 0.99f)
        val onBackground = Color(0xFF1C1B1F)
        
        val surface = background
        val onSurface = onBackground
        val surfaceVariant = fromHsv(h, s * 0.08f, 0.90f)
        val onSurfaceVariant = fromHsv(h, s * 0.35f, 0.25f)
        
        val outline = fromHsv(h, s * 0.25f, 0.45f)
        
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline
        )
    }
    
    return if (darkTheme && amoled) baseScheme.amoled else baseScheme
}

private fun getCustomColorScheme(accent: String, darkTheme: Boolean, amoled: Boolean): ColorScheme {
    if (accent.startsWith("#")) {
        try {
            val parsedColor = Color(accent.toColorInt())
            return getDynamicCustomColorScheme(parsedColor, darkTheme, amoled)
        } catch (_: Exception) {
            // Fallback to default
        }
    }
    val baseScheme = if (darkTheme) {
        when (accent) {
            "BLUE" -> darkColorScheme(
                primary = Color(0xFFA5C8FF),
                secondary = Color(0xFFBEC6DC),
                tertiary = Color(0xFFE8B9D4),
                background = Color(0xFF111318),
                surface = Color(0xFF111318)
            )
            "GREEN" -> darkColorScheme(
                primary = Color(0xFF81C784),
                secondary = Color(0xFFBCCBB0),
                tertiary = Color(0xFFA5D6A7),
                background = Color(0xFF101410),
                surface = Color(0xFF101410)
            )
            "RED" -> darkColorScheme(
                primary = Color(0xFFFFB4AB),
                secondary = Color(0xFFE7BDB8),
                tertiary = Color(0xFFE0C1B6),
                background = Color(0xFF201A19),
                surface = Color(0xFF201A19)
            )
            "ORANGE" -> darkColorScheme(
                primary = Color(0xFFFFB74D),
                secondary = Color(0xFFE4C5A1),
                tertiary = Color(0xFFDFCC97),
                background = Color(0xFF1D1B16),
                surface = Color(0xFF1D1B16)
            )
            "PINK" -> darkColorScheme(
                primary = Color(0xFFF48FB1),
                secondary = Color(0xFFECC1CE),
                tertiary = Color(0xFFD6C2C9),
                background = Color(0xFF1E1A1B),
                surface = Color(0xFF1E1A1B)
            )
            else -> DarkColorScheme // Purple/Default
        }
    } else {
        when (accent) {
            "BLUE" -> lightColorScheme(
                primary = Color(0xFF005FAF),
                secondary = Color(0xFF535F70),
                tertiary = Color(0xFF725573),
                background = Color(0xFFF9F9FF),
                surface = Color(0xFFF9F9FF)
            )
            "GREEN" -> lightColorScheme(
                primary = Color(0xFF388E3C),
                secondary = Color(0xFF52634F),
                tertiary = Color(0xFF386566),
                background = Color(0xFFF6FAF3),
                surface = Color(0xFFF6FAF3)
            )
            "RED" -> lightColorScheme(
                primary = Color(0xFFBA1A1A),
                secondary = Color(0xFF775652),
                tertiary = Color(0xFF7A574E),
                background = Color(0xFFFFF8F7),
                surface = Color(0xFFFFF8F7)
            )
            "ORANGE" -> lightColorScheme(
                primary = Color(0xFFF57C00),
                secondary = Color(0xFF6E5D4B),
                tertiary = Color(0xFF656041),
                background = Color(0xFFFFFBFF),
                surface = Color(0xFFFFFBFF)
            )
            "PINK" -> lightColorScheme(
                primary = Color(0xFFC2185B),
                secondary = Color(0xFF74565F),
                tertiary = Color(0xFF7B5762),
                background = Color(0xFFFFF8F8),
                surface = Color(0xFFFFF8F8)
            )
            else -> LightColorScheme // Purple/Default
        }
    }
    
    return if (darkTheme && amoled) baseScheme.amoled else baseScheme
}

@Composable
fun BirthdayBuddyTheme(
    themeMode: String = "SYSTEM",
    themeAmoled: Boolean = false,
    themeAccent: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val dynamicColor = themeAccent == "SYSTEM" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            val baseScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && themeAmoled) baseScheme.amoled else baseScheme
        }
        else -> {
            getCustomColorScheme(accent = themeAccent, darkTheme = darkTheme, amoled = themeAmoled)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
