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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.toColorInt

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = PurpleGrey80,
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Pink80,
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = PurpleGrey40,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Pink40,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
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

private fun getDynamicCustomColorScheme(
    seedColor: Color,
    darkTheme: Boolean,
    amoled: Boolean
): ColorScheme {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seedColor.toArgb(), hsv)

    val h = hsv[0]
    val s = hsv[1]
    val v = hsv[2]

    fun fromHsv(hue: Float, sat: Float, value: Float): Color {
        val calculatedArgb = android.graphics.Color.HSVToColor(
            floatArrayOf(
                hue,
                sat.coerceIn(0f, 1f),
                value.coerceIn(0f, 1f)
            )
        )
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
        val outlineVariant = fromHsv(h, s * 0.15f, 0.30f)

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
            outline = outline,
            outlineVariant = outlineVariant,
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6)
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
        val outlineVariant = fromHsv(h, s * 0.15f, 0.82f)

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
            outline = outline,
            outlineVariant = outlineVariant,
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
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
                onPrimary = Color(0xFF00315F),
                primaryContainer = Color(0xFF004787),
                onPrimaryContainer = Color(0xFFD4E3FF),
                secondary = Color(0xFFBEC6DC),
                onSecondary = Color(0xFF253140),
                secondaryContainer = Color(0xFF3B4858),
                onSecondaryContainer = Color(0xFFD7E3F7),
                tertiary = Color(0xFFE8B9D4),
                onTertiary = Color(0xFF412740),
                tertiaryContainer = Color(0xFF593E58),
                onTertiaryContainer = Color(0xFFFCD7FB),
                background = Color(0xFF111318),
                onBackground = Color(0xFFE2E2E9),
                surface = Color(0xFF111318),
                onSurface = Color(0xFFE2E2E9),
                surfaceVariant = Color(0xFF43474E),
                onSurfaceVariant = Color(0xFFC4C6D0),
                outline = Color(0xFF8E9099),
                outlineVariant = Color(0xFF43474E),
                error = Color(0xFFFFB4AB),
                onError = Color(0xFF690005),
                errorContainer = Color(0xFF93000A),
                onErrorContainer = Color(0xFFFFDAD6)
            )

            "GREEN" -> darkColorScheme(
                primary = Color(0xFF81C784),
                onPrimary = Color(0xFF003300),
                primaryContainer = Color(0xFF1B5E20),
                onPrimaryContainer = Color(0xFFC8E6C9),
                secondary = Color(0xFFBCCBB0),
                onSecondary = Color(0xFF253423),
                secondaryContainer = Color(0xFF3B4B38),
                onSecondaryContainer = Color(0xFFD5E8CF),
                tertiary = Color(0xFFA5D6A7),
                onTertiary = Color(0xFF003738),
                tertiaryContainer = Color(0xFF1D4D4E),
                onTertiaryContainer = Color(0xFFBCEBEB),
                background = Color(0xFF101410),
                onBackground = Color(0xFFE2E3DD),
                surface = Color(0xFF101410),
                onSurface = Color(0xFFE2E3DD),
                surfaceVariant = Color(0xFF434842),
                onSurfaceVariant = Color(0xFFC3C8C0),
                outline = Color(0xFF8D938B),
                outlineVariant = Color(0xFF434842),
                error = Color(0xFFFFB4AB),
                onError = Color(0xFF690005),
                errorContainer = Color(0xFF93000A),
                onErrorContainer = Color(0xFFFFDAD6)
            )

            "RED" -> darkColorScheme(
                primary = Color(0xFFFFB4AB),
                onPrimary = Color(0xFF690005),
                primaryContainer = Color(0xFF93000A),
                onPrimaryContainer = Color(0xFFFFDAD6),
                secondary = Color(0xFFE7BDB8),
                onSecondary = Color(0xFF442926),
                secondaryContainer = Color(0xFF5D3F3C),
                onSecondaryContainer = Color(0xFFFFDAD5),
                tertiary = Color(0xFFE0C1B6),
                onTertiary = Color(0xFF462A22),
                tertiaryContainer = Color(0xFF5F4037),
                onTertiaryContainer = Color(0xFFFFDAD2),
                background = Color(0xFF201A19),
                onSurface = Color(0xFFEDE0DF),
                surface = Color(0xFF201A19),
                onBackground = Color(0xFFEDE0DF),
                surfaceVariant = Color(0xFF534341),
                onSurfaceVariant = Color(0xFFD8C2BF),
                outline = Color(0xFFA08C8A),
                outlineVariant = Color(0xFF534341),
                error = Color(0xFFFFB4AB),
                onError = Color(0xFF690005),
                errorContainer = Color(0xFF93000A),
                onErrorContainer = Color(0xFFFFDAD6)
            )

            "ORANGE" -> darkColorScheme(
                primary = Color(0xFFFFB74D),
                onPrimary = Color(0xFF4D2800),
                primaryContainer = Color(0xFF6D3B00),
                onPrimaryContainer = Color(0xFFFFDDB8),
                secondary = Color(0xFFE4C5A1),
                onSecondary = Color(0xFF3D2F20),
                secondaryContainer = Color(0xFF554535),
                onSecondaryContainer = Color(0xFFF7DFCD),
                tertiary = Color(0xFFDFCC97),
                onTertiary = Color(0xFF353117),
                tertiaryContainer = Color(0xFF4D482B),
                onTertiaryContainer = Color(0xFFECE4BF),
                background = Color(0xFF1D1B16),
                onBackground = Color(0xFFEDE1D6),
                surface = Color(0xFF1D1B16),
                onSurface = Color(0xFFEDE1D6),
                surfaceVariant = Color(0xFF4F4539),
                onSurfaceVariant = Color(0xFFD3C4B4),
                outline = Color(0xFF9C8F80),
                outlineVariant = Color(0xFF4F4539),
                error = Color(0xFFFFB4AB),
                onError = Color(0xFF690005),
                errorContainer = Color(0xFF93000A),
                onErrorContainer = Color(0xFFFFDAD6)
            )

            "PINK" -> darkColorScheme(
                primary = Color(0xFFF48FB1),
                onPrimary = Color(0xFF620031),
                primaryContainer = Color(0xFF8D004B),
                onPrimaryContainer = Color(0xFFFFD9E2),
                secondary = Color(0xFFECC1CE),
                onSecondary = Color(0xFF422931),
                secondaryContainer = Color(0xFF5B3F47),
                onSecondaryContainer = Color(0xFFFFD9E2),
                tertiary = Color(0xFFD6C2C9),
                onTertiary = Color(0xFF472A33),
                tertiaryContainer = Color(0xFF60404A),
                onTertiaryContainer = Color(0xFFFFD9E2),
                background = Color(0xFF1E1A1B),
                onBackground = Color(0xFFEDE0E2),
                surface = Color(0xFF1E1A1B),
                onSurface = Color(0xFFEDE0E2),
                surfaceVariant = Color(0xFF514346),
                onSurfaceVariant = Color(0xFFD5C2C5),
                outline = Color(0xFF9D8C90),
                outlineVariant = Color(0xFF514346),
                error = Color(0xFFFFB4AB),
                onError = Color(0xFF620031),
                errorContainer = Color(0xFF8D004B),
                onErrorContainer = Color(0xFFFFD9E2)
            )

            else -> DarkColorScheme // Purple/Default
        }
    } else {
        when (accent) {
            "BLUE" -> lightColorScheme(
                primary = Color(0xFF005FAF),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFD4E3FF),
                onPrimaryContainer = Color(0xFF001C3A),
                secondary = Color(0xFF535F70),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFD7E3F7),
                onSecondaryContainer = Color(0xFF101C2B),
                tertiary = Color(0xFF725573),
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFFCD7FB),
                onTertiaryContainer = Color(0xFF2A132D),
                background = Color(0xFFF9F9FF),
                onBackground = Color(0xFF1A1C1E),
                surface = Color(0xFFF9F9FF),
                onSurface = Color(0xFF1A1C1E),
                surfaceVariant = Color(0xFFE0E2EC),
                onSurfaceVariant = Color(0xFF43474E),
                outline = Color(0xFF74777F),
                outlineVariant = Color(0xFFE0E2EC),
                error = Color(0xFFBA1A1A),
                onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFFFDAD6),
                onErrorContainer = Color(0xFF410002)
            )

            "GREEN" -> lightColorScheme(
                primary = Color(0xFF388E3C),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFC8E6C9),
                onPrimaryContainer = Color(0xFF003300),
                secondary = Color(0xFF52634F),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFD5E8CF),
                onSecondaryContainer = Color(0xFF111F0F),
                tertiary = Color(0xFF386566),
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFBCEBEB),
                onTertiaryContainer = Color(0xFF002021),
                background = Color(0xFFF6FAF3),
                onBackground = Color(0xFF191D17),
                surface = Color(0xFFF6FAF3),
                onSurface = Color(0xFF191D17),
                surfaceVariant = Color(0xFFDFE4DC),
                onSurfaceVariant = Color(0xFF434842),
                outline = Color(0xFF737972),
                outlineVariant = Color(0xFFDFE4DC),
                error = Color(0xFFBA1A1A),
                onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFFFDAD6),
                onErrorContainer = Color(0xFF410002)
            )

            "RED" -> lightColorScheme(
                primary = Color(0xFFBA1A1A),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFDAD6),
                onPrimaryContainer = Color(0xFF410002),
                secondary = Color(0xFF775652),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFFFDAD5),
                onSecondaryContainer = Color(0xFF2C1512),
                tertiary = Color(0xFF7A574E),
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFFFDAD2),
                onTertiaryContainer = Color(0xFF2E1510),
                background = Color(0xFFFFF8F7),
                onBackground = Color(0xFF221A19),
                surface = Color(0xFFFFF8F7),
                onSurface = Color(0xFF221A19),
                surfaceVariant = Color(0xFFF5DDDA),
                onSurfaceVariant = Color(0xFF534341),
                outline = Color(0xFF857370),
                outlineVariant = Color(0xFFF5DDDA),
                error = Color(0xFFBA1A1A),
                onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFFFDAD6),
                onErrorContainer = Color(0xFF410002)
            )

            "ORANGE" -> lightColorScheme(
                primary = Color(0xFFF57C00),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFDDB8),
                onPrimaryContainer = Color(0xFF2B1700),
                secondary = Color(0xFF6E5D4B),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFF7DFCD),
                onSecondaryContainer = Color(0xFF271B0D),
                tertiary = Color(0xFF656041),
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFECE4BF),
                onTertiaryContainer = Color(0xFF201C05),
                background = Color(0xFFFFFBFF),
                onBackground = Color(0xFF1D1B16),
                surface = Color(0xFFFFFBFF),
                onSurface = Color(0xFF1D1B16),
                surfaceVariant = Color(0xFFF0E0CF),
                onSurfaceVariant = Color(0xFF4F4539),
                outline = Color(0xFF817567),
                outlineVariant = Color(0xFFF0E0CF),
                error = Color(0xFFBA1A1A),
                onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFFFDAD6),
                onErrorContainer = Color(0xFF410002)
            )

            "PINK" -> lightColorScheme(
                primary = Color(0xFFC2185B),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFD9E2),
                onPrimaryContainer = Color(0xFF3E001D),
                secondary = Color(0xFF74565F),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFFFD9E2),
                onSecondaryContainer = Color(0xFF2B151C),
                tertiary = Color(0xFF7B5762),
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFFFD9E2),
                onTertiaryContainer = Color(0xFF30151F),
                background = Color(0xFFFFF8F8),
                onBackground = Color(0xFF201A1B),
                surface = Color(0xFFFFF8F8),
                onSurface = Color(0xFF201A1B),
                surfaceVariant = Color(0xFFF2DDE1),
                onSurfaceVariant = Color(0xFF514346),
                outline = Color(0xFF837376),
                outlineVariant = Color(0xFFF2DDE1),
                error = Color(0xFFBA1A1A),
                onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFFFDAD6),
                onErrorContainer = Color(0xFF410002)
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
            val baseScheme =
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && themeAmoled) baseScheme.amoled else baseScheme
        }

        else -> {
            getCustomColorScheme(accent = themeAccent, darkTheme = darkTheme, amoled = themeAmoled)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
