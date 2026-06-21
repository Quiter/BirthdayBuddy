package com.heckmannch.birthdaybuddy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.graphics.toColorInt
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeContent
import com.materialkolor.dynamiccolor.MaterialDynamicColors

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

private fun createHctColorScheme(seedColor: Color, isDark: Boolean, contrast: Double): ColorScheme {
    val hct = Hct.fromInt(seedColor.toArgb())
    val scheme = SchemeContent(hct, isDark, contrast)
    val colors = MaterialDynamicColors()

    return ColorScheme(
        primary = Color(colors.primary().getArgb(scheme)),
        onPrimary = Color(colors.onPrimary().getArgb(scheme)),
        primaryContainer = Color(colors.primaryContainer().getArgb(scheme)),
        onPrimaryContainer = Color(colors.onPrimaryContainer().getArgb(scheme)),
        inversePrimary = Color(colors.inversePrimary().getArgb(scheme)),
        secondary = Color(colors.secondary().getArgb(scheme)),
        onSecondary = Color(colors.onSecondary().getArgb(scheme)),
        secondaryContainer = Color(colors.secondaryContainer().getArgb(scheme)),
        onSecondaryContainer = Color(colors.onSecondaryContainer().getArgb(scheme)),
        tertiary = Color(colors.tertiary().getArgb(scheme)),
        onTertiary = Color(colors.onTertiary().getArgb(scheme)),
        tertiaryContainer = Color(colors.tertiaryContainer().getArgb(scheme)),
        onTertiaryContainer = Color(colors.onTertiaryContainer().getArgb(scheme)),
        background = Color(colors.background().getArgb(scheme)),
        onBackground = Color(colors.onBackground().getArgb(scheme)),
        surface = Color(colors.surface().getArgb(scheme)),
        onSurface = Color(colors.onSurface().getArgb(scheme)),
        surfaceVariant = Color(colors.surfaceVariant().getArgb(scheme)),
        onSurfaceVariant = Color(colors.onSurfaceVariant().getArgb(scheme)),
        surfaceTint = Color(colors.primary().getArgb(scheme)),
        outline = Color(colors.outline().getArgb(scheme)),
        outlineVariant = Color(colors.outlineVariant().getArgb(scheme)),
        scrim = Color(colors.scrim().getArgb(scheme)),
        error = Color(colors.error().getArgb(scheme)),
        onError = Color(colors.onError().getArgb(scheme)),
        errorContainer = Color(colors.errorContainer().getArgb(scheme)),
        onErrorContainer = Color(colors.onErrorContainer().getArgb(scheme)),
        inverseSurface = Color(colors.inverseSurface().getArgb(scheme)),
        inverseOnSurface = Color(colors.inverseOnSurface().getArgb(scheme)),
        surfaceContainerLowest = Color(colors.surfaceContainerLowest().getArgb(scheme)),
        surfaceContainerLow = Color(colors.surfaceContainerLow().getArgb(scheme)),
        surfaceContainer = Color(colors.surfaceContainer().getArgb(scheme)),
        surfaceContainerHigh = Color(colors.surfaceContainerHigh().getArgb(scheme)),
        surfaceContainerHighest = Color(colors.surfaceContainerHighest().getArgb(scheme)),
        surfaceDim = Color(colors.surfaceDim().getArgb(scheme)),
        surfaceBright = Color(colors.surfaceBright().getArgb(scheme)),
        primaryFixed = Color(colors.primaryFixed().getArgb(scheme)),
        primaryFixedDim = Color(colors.primaryFixedDim().getArgb(scheme)),
        onPrimaryFixed = Color(colors.onPrimaryFixed().getArgb(scheme)),
        onPrimaryFixedVariant = Color(colors.onPrimaryFixedVariant().getArgb(scheme)),
        secondaryFixed = Color(colors.secondaryFixed().getArgb(scheme)),
        secondaryFixedDim = Color(colors.secondaryFixedDim().getArgb(scheme)),
        onSecondaryFixed = Color(colors.onSecondaryFixed().getArgb(scheme)),
        onSecondaryFixedVariant = Color(colors.onSecondaryFixedVariant().getArgb(scheme)),
        tertiaryFixed = Color(colors.tertiaryFixed().getArgb(scheme)),
        tertiaryFixedDim = Color(colors.tertiaryFixedDim().getArgb(scheme)),
        onTertiaryFixed = Color(colors.onTertiaryFixed().getArgb(scheme)),
        onTertiaryFixedVariant = Color(colors.onTertiaryFixedVariant().getArgb(scheme))
    )
}

private fun getCustomColorScheme(accent: String, darkTheme: Boolean, amoled: Boolean, contrast: Double): ColorScheme {
    val seedColor = if (accent.startsWith("#")) {
        try {
            Color(accent.toColorInt())
        } catch (_: Exception) {
            Color(0xFF6750A4) // Fallback
        }
    } else {
        when (accent) {
            "BLUE" -> Color(0xFF005FAF)
            "GREEN" -> Color(0xFF388E3C)
            "RED" -> Color(0xFFBA1A1A)
            "ORANGE" -> Color(0xFFF57C00)
            "PINK" -> Color(0xFFC2185B)
            else -> Color(0xFF6750A4) // Purple/Default
        }
    }
    val baseScheme = createHctColorScheme(seedColor, darkTheme, contrast)
    return if (darkTheme && amoled) baseScheme.amoled else baseScheme
}

@Composable
fun BirthdayBuddyTheme(
    themeMode: String = "SYSTEM",
    themeAmoled: Boolean = false,
    themeAccent: String = "SYSTEM",
    themeContrast: Double = 0.0,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val isPreview = LocalInspectionMode.current
    val dynamicColor =
        !isPreview && themeAccent == "SYSTEM" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            val baseScheme =
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && themeAmoled) baseScheme.amoled else baseScheme
        }

        else -> {
            getCustomColorScheme(accent = themeAccent, darkTheme = darkTheme, amoled = themeAmoled, contrast = themeContrast)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
