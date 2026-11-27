package com.heckmannch.birthdaybuddy.ui.components.cards

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.heckmannch.birthdaybuddy.ui.theme.AlphaGlassCardChipContainer
import com.heckmannch.birthdaybuddy.ui.theme.AlphaGlassCardFillEnd
import com.heckmannch.birthdaybuddy.ui.theme.AlphaGlassCardFlatEdge
import com.heckmannch.birthdaybuddy.ui.theme.AlphaGlassCardMarkContainer
import com.heckmannch.birthdaybuddy.ui.theme.AlphaGlassCardShadow
import com.heckmannch.birthdaybuddy.ui.theme.AlphaGlassCardSubtitle
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusLarge
import com.heckmannch.birthdaybuddy.ui.theme.GlassCardHeightDefault
import com.heckmannch.birthdaybuddy.ui.theme.GlassCardIconSize
import com.heckmannch.birthdaybuddy.ui.theme.GlassCardMarkSize
import com.heckmannch.birthdaybuddy.ui.theme.GlassDarkContentColor
import com.heckmannch.birthdaybuddy.ui.theme.GlassLightContentColor
import com.heckmannch.birthdaybuddy.ui.theme.GlassShadowColor
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

/**
 * Visual style configuration for frosted glass cards across BirthdayBuddy screens
 * (Home, Labels, and Settings).
 */
@Immutable
data class GlassCardStyle(
    val monochrome: Boolean = false,
    val outlines: Boolean = true,
    val iconSize: Dp = GlassCardIconSize,
    val titleColor: Color,
    val subtitleColor: Color,
    val titleStyle: TextStyle,
    val subtitleStyle: TextStyle,
    val chipContainerColor: Color,
    val chipContentColor: Color,
    val actionContainerColor: Color,
    val actionContentColor: Color,
    val markContainerColor: Color,
    val markSize: Dp = GlassCardMarkSize,
    val cardColor: Color,
    val cardRadius: Dp = CornerRadiusLarge,
    val cardHeight: Dp = GlassCardHeightDefault,
    val contentPadding: Dp = SpacingNormal,
    val contentSpacing: Dp = SpacingNormal,
    val textSpacing: Dp = SpacingExtraSmall
) {
    /** Even edge of a card in one color for flat contrast. */
    val flatEdgeColor: Color get() = titleColor.copy(alpha = AlphaGlassCardFlatEdge)

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
            titleColor = GlassDarkContentColor,
            subtitleColor = GlassDarkContentColor.copy(alpha = subtitleColor.alpha),
            titleStyle = titleStyle.copy(shadow = null),
            subtitleStyle = subtitleStyle.copy(shadow = null),
            chipContainerColor = GlassDarkContentColor.copy(alpha = chipContainerColor.alpha),
            chipContentColor = GlassDarkContentColor,
            actionContainerColor = GlassDarkContentColor,
            actionContentColor = GlassLightContentColor,
            markContainerColor = GlassDarkContentColor.copy(alpha = markContainerColor.alpha)
        )
    }

    companion object {
        const val FILL_ALPHA = AlphaGlassCardFillEnd
    }
}

@Composable
fun glassCardStyle(subtitleAlpha: Float = AlphaGlassCardSubtitle): GlassCardStyle {
    val titleShadow = Shadow(
        color = GlassShadowColor.copy(alpha = AlphaGlassCardShadow),
        offset = Offset(0f, 2f),
        blurRadius = 4f
    )
    val subtitleShadow = Shadow(
        color = GlassShadowColor.copy(alpha = AlphaGlassCardShadow),
        offset = Offset(0f, 1f),
        blurRadius = 2f
    )

    return GlassCardStyle(
        monochrome = false,
        outlines = true,
        iconSize = GlassCardIconSize,
        titleColor = GlassLightContentColor,
        subtitleColor = GlassLightContentColor.copy(alpha = subtitleAlpha),
        titleStyle = MaterialTheme.typography.titleMedium.copy(shadow = titleShadow),
        subtitleStyle = MaterialTheme.typography.bodyMedium.copy(shadow = subtitleShadow),
        chipContainerColor = GlassLightContentColor.copy(alpha = AlphaGlassCardChipContainer),
        chipContentColor = GlassLightContentColor,
        actionContainerColor = MaterialTheme.colorScheme.primaryContainer,
        actionContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        markContainerColor = GlassLightContentColor.copy(alpha = AlphaGlassCardMarkContainer),
        markSize = GlassCardMarkSize,
        cardColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}
