package com.heckmannch.birthdaybuddy.ui.components.cards

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.heckmannch.birthdaybuddy.ui.theme.GlassDarkContentColor
import com.heckmannch.birthdaybuddy.ui.theme.GlassLightContentColor
import kotlin.math.abs

val LocalCardBackground = compositionLocalOf<Color?> { null }
val LocalAccent = compositionLocalOf<Color?> { null }

private const val CardSeparation = 0.15f

@Composable
fun Color.distinctFromCard(): Color {
    val card = LocalCardBackground.current ?: return this
    return ensureContrast(card, CardSeparation)
}

fun List<Color>.blend(): Color = when {
    isEmpty() -> Color.Transparent
    size == 1 -> first()
    else -> Color(
        red = sumOf { it.red.toDouble() }.toFloat() / size,
        green = sumOf { it.green.toDouble() }.toFloat() / size,
        blue = sumOf { it.blue.toDouble() }.toFloat() / size,
        alpha = sumOf { it.alpha.toDouble() }.toFloat() / size
    )
}

fun Color.requiresLightContent(): Boolean = luminance() < 0.5f

fun Color.contrastingContent(): Color =
    if (requiresLightContent()) GlassLightContentColor else GlassDarkContentColor

fun Color.ensureContrast(
    background: Color,
    minLuminanceDiff: Float = 0.05f
): Color {
    val bgLum = background.luminance()
    val fgLum = this.luminance()
    val diff = abs(bgLum - fgLum)
    if (diff >= minLuminanceDiff) return this
    return if (bgLum > 0.5f) darken((minLuminanceDiff - diff + 0.05f).coerceIn(0f, 0.8f))
    else lighten((minLuminanceDiff - diff + 0.05f).coerceIn(0f, 0.8f))
}

fun Color.lighten(factor: Float): Color = Color(
    red = red + (1f - red) * factor,
    green = green + (1f - green) * factor,
    blue = blue + (1f - blue) * factor,
    alpha = alpha
)

fun Color.darken(factor: Float): Color = Color(
    red = red * (1f - factor),
    green = green * (1f - factor),
    blue = blue * (1f - factor),
    alpha = alpha
)

val LayoutDirection.isRtl: Boolean get() = this == LayoutDirection.Rtl

@Composable
fun isRtl(): Boolean = LocalLayoutDirection.current.isRtl

fun startEdgeX(width: Float, rtl: Boolean, inset: Float = 0f): Float = if (rtl) width - inset else inset
fun endEdgeX(width: Float, rtl: Boolean, inset: Float = 0f): Float = if (rtl) inset else width - inset

fun startToEndGradient(colors: List<Color>, rtl: Boolean): Brush =
    Brush.horizontalGradient(if (rtl) colors.reversed() else colors)

fun startToEndGradient(startColor: Color, endColor: Color, rtl: Boolean): Brush =
    startToEndGradient(listOf(startColor, endColor), rtl)

const val PressedScale = 0.97f

@Composable
fun rememberPressScale(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = PressedScale,
    label: String = "press_scale"
): State<Float> {
    val isPressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = label
    )
}

@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = PressedScale,
    label: String = "press_scale"
): Modifier {
    val scale = rememberPressScale(interactionSource, enabled, pressedScale, label)
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
