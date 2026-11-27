package com.heckmannch.birthdaybuddy.ui.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.heckmannch.birthdaybuddy.ui.theme.AlphaGlassCardBorderNeutral
import com.heckmannch.birthdaybuddy.ui.theme.AlphaGlassCardBorderTinted
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThin

/**
 * Hairlines and border definitions for glassmorphic cards in BirthdayBuddy.
 * Models Morphe Manager's CardBorder for subtle decorative card outlines.
 */
object CardBorder {
    val Width: Dp = BorderWidthThin

    @Composable
    fun of(color: Color, width: Dp = Width): BorderStroke = BorderStroke(width, color)

    val neutral: BorderStroke
        @Composable get() = of(MaterialTheme.colorScheme.outlineVariant.copy(alpha = AlphaGlassCardBorderNeutral))

    @Composable
    fun tinted(accent: Color): BorderStroke = of(accent.copy(alpha = AlphaGlassCardBorderTinted))
}

fun Modifier.cardBorder(border: BorderStroke?, shape: Shape): Modifier =
    if (border != null) border(border, shape) else this
