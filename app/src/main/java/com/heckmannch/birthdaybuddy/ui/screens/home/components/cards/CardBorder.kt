package com.heckmannch.birthdaybuddy.ui.screens.home.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Hairlines and border definitions for glassmorphic cards in BirthdayBuddy.
 * Models Morphe Manager's CardBorder for subtle decorative card outlines.
 */
object CardBorder {
    val Width: Dp = 1.dp
    private const val TINTED_ALPHA = 0.25f

    @Composable
    fun of(color: Color, width: Dp = Width): BorderStroke = BorderStroke(width, color)

    val neutral: BorderStroke
        @Composable get() = of(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

    @Composable
    fun tinted(accent: Color): BorderStroke = of(accent.copy(alpha = TINTED_ALPHA))
}

fun Modifier.cardBorder(border: BorderStroke?, shape: Shape): Modifier =
    if (border != null) border(border, shape) else this
