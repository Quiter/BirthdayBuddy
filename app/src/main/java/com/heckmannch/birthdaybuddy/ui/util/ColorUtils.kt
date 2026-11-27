package com.heckmannch.birthdaybuddy.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt
import java.util.Locale

private const val LightContentLuminanceThreshold = 0.5f

/**
 * Converts this [Color] to HSV components: hue in `0f..360f` degrees, saturation and value in
 * `0f..1f`.
 *
 * Achromatic colors (grays, black, white) have no inherent hue and report `0f`, so callers
 * editing an existing HSV state should preserve their current hue when saturation is `0f`.
 */
fun Color.toHsv(): Triple<Float, Float, Float> {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    return Triple(hsv[0], hsv[1], hsv[2])
}

/**
 * Formats this [Color] as an uppercase hexadecimal string using [Locale.ROOT].
 *
 * @param includeHash Prepends `#` when `true` (default). Pass `false` for input fields that draw
 *   their own `#` prefix.
 * @param includeAlpha Formats as 8-digit `AARRGGBB` instead of 6-digit `RRGGBB` when `true`.
 */
fun Color.toHexString(
    includeHash: Boolean = true,
    includeAlpha: Boolean = false,
): String {
    val argb = toArgb()
    val prefix = if (includeHash) "#" else ""
    return if (includeAlpha) {
        String.format(Locale.ROOT, "%s%08X", prefix, argb)
    } else {
        String.format(Locale.ROOT, "%s%06X", prefix, argb and 0xFFFFFF)
    }
}

/**
 * Parses a 6-digit (`RRGGBB`) or 8-digit (`AARRGGBB`) hexadecimal string with optional leading `#`
 * into a Compose [Color], returning `null` for blank or malformed values.
 */
fun String?.toColorOrNull(): Color? {
    val clean = this?.trim()?.removePrefix("#").orEmpty()
    if ((clean.length != 6 && clean.length != 8) || !clean.all { it.isHexDigit() }) {
        return null
    }
    return runCatching { Color("#$clean".toColorInt()) }.getOrNull()
}

/**
 * Darkens this [Color] by mixing it toward black by [factor] (`0f..1f`), preserving alpha.
 */
fun Color.darken(factor: Float): Color {
    val clamped = factor.coerceIn(0f, 1f)
    return Color(
        red = red * (1f - clamped),
        green = green * (1f - clamped),
        blue = blue * (1f - clamped),
        alpha = alpha,
    )
}

/**
 * Returns `true` when this color is dark enough to require light (white) foreground content for
 * legibility based on relative luminance.
 */
fun Color.requiresLightContent(): Boolean = luminance() < LightContentLuminanceThreshold

/**
 * Returns [Color.White] or [Color.Black], whichever provides stronger contrast on this background.
 */
fun Color.contrastingContent(): Color = if (requiresLightContent()) Color.White else Color.Black

private fun Char.isHexDigit(): Boolean = isDigit() || uppercaseChar() in 'A'..'F'
