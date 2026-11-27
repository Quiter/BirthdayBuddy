package com.heckmannch.birthdaybuddy.ui.components.bars

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * Shared CompositionLocal to pass the [HazeState] from background content to floating glass elements.
 */
val LocalBackdropHaze = staticCompositionLocalOf<HazeState?> { null }

/**
 * Creates a frosted glass blur style combining background with tint.
 */
fun glassBlurStyle(tint: Color, background: Color): HazeBlurStyle {
    val solid = tint.compositeOver(background)
    return HazeBlurStyle {
        backgroundColor(background)
        colorEffects(listOf(HazeColorEffect.tint(solid.copy(alpha = 0.55f))))
        fallbackColorEffect(HazeColorEffect.tint(solid))
    }
}

/**
 * Fills a floating surface with what scrolls under it blurred under its [glass] tint,
 * or with a fallback background color if no [haze] is provided.
 */
@Composable
fun Modifier.glassFill(
    glass: Color,
    shape: Shape,
    haze: HazeState? = LocalBackdropHaze.current,
): Modifier {
    val screen = MaterialTheme.colorScheme.background
    if (haze == null) return background(glass.compositeOver(screen), shape)
    val style = remember(glass, screen) { glassBlurStyle(glass, screen) }
    return clip(shape).hazeBlur(HazeInput.Sources(haze), style)
}
