package com.heckmannch.birthdaybuddy.ui.screens.home.components.cards

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

/**
 * Visual style configuration for frosted glass cards across BirthdayBuddy screens.
 * Directly modeled after Morphe Manager's GlassCardStyle architecture for a frosted glass look.
 */
@Immutable
data class GlassCardStyle(
    val monochrome: Boolean = false,
    val outlines: Boolean = true,
    val iconSize: Dp = 56.dp,
    val titleColor: Color,
    val subtitleColor: Color,
    val titleStyle: TextStyle,
    val subtitleStyle: TextStyle,
    val chipContainerColor: Color,
    val chipContentColor: Color,
    val actionContainerColor: Color,
    val actionContentColor: Color,
    val markContainerColor: Color,
    val markSize: Dp = 24.dp,
    val cardColor: Color,
    val cardRadius: Dp = CornerRadiusLarge,
    val cardHeight: Dp = 80.dp,
    val contentPadding: Dp = SpacingNormal,
    val contentSpacing: Dp = SpacingNormal,
    val textSpacing: Dp = SpacingExtraSmall
) {
    /** Even edge of a card in one color for flat contrast. */
    val flatEdgeColor: Color get() = titleColor.copy(alpha = 0.30f)

    fun cardColors(bundleColors: List<Color>): List<Color> =
        if (bundleColors.isEmpty()) listOf(cardColor) else bundleColors

    fun flatColor(color: Color): Color = color

    fun flatFill(color: Color?): Color =
        if (monochrome || color == null) cardColor else color.copy(alpha = FILL_ALPHA)

    fun onCard(gradientColors: List<Color>): GlassCardStyle = onFill(cardColors(gradientColors).blend())

    fun onFill(color: Color?): GlassCardStyle {
        if (monochrome) return this

        val fill = color ?: cardColor
        if (fill.requiresLightContent()) return this

        return copy(
            titleColor = Color.Black,
            subtitleColor = Color.Black.copy(alpha = subtitleColor.alpha),
            titleStyle = titleStyle.copy(shadow = null),
            subtitleStyle = subtitleStyle.copy(shadow = null),
            chipContainerColor = Color.Black.copy(alpha = chipContainerColor.alpha),
            chipContentColor = Color.Black,
            actionContainerColor = Color.Black,
            actionContentColor = Color.White,
            markContainerColor = Color.Black.copy(alpha = markContainerColor.alpha)
        )
    }

    companion object {
        const val FILL_ALPHA = 0.64f
    }
}

@Composable
fun glassCardStyle(subtitleAlpha: Float = 0.85f): GlassCardStyle {
    val titleShadow = Shadow(
        color = Color.Black.copy(alpha = 0.4f),
        offset = Offset(0f, 2f),
        blurRadius = 4f
    )
    val subtitleShadow = Shadow(
        color = Color.Black.copy(alpha = 0.4f),
        offset = Offset(0f, 1f),
        blurRadius = 2f
    )
    val iconSize = 56.dp

    return GlassCardStyle(
        monochrome = false,
        outlines = true,
        iconSize = iconSize,
        titleColor = Color.White,
        subtitleColor = Color.White.copy(alpha = subtitleAlpha),
        titleStyle = MaterialTheme.typography.titleMedium.copy(shadow = titleShadow),
        subtitleStyle = MaterialTheme.typography.bodyMedium.copy(shadow = subtitleShadow),
        chipContainerColor = Color.White.copy(alpha = 0.20f),
        chipContentColor = Color.White,
        actionContainerColor = MaterialTheme.colorScheme.primaryContainer,
        actionContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        markContainerColor = Color.White.copy(alpha = 0.35f),
        markSize = 24.dp,
        cardColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}
