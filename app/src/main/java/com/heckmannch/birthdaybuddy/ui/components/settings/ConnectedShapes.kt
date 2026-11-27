package com.heckmannch.birthdaybuddy.ui.components.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusLargeIncreased
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusSmall
import com.heckmannch.birthdaybuddy.ui.theme.SettingsPressedCornerScale

private const val DarkSurfaceLuminanceThreshold = 0.5f

/**
 * Returns true when this [ColorScheme] represents a dark theme based on background luminance.
 */
internal fun ColorScheme.isDarkSurface(): Boolean =
    background.luminance() < DarkSurfaceLuminanceThreshold

/**
 * Explicit corner radii (in [Dp]) for a connected segment in a horizontal or vertical group.
 */
@Immutable
data class CornerRadii(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomEnd: Dp,
    val bottomStart: Dp,
) {
    /**
     * Scales all corners by [factor], typically used for press feedback morphing.
     */
    fun scaled(factor: Float): CornerRadii = CornerRadii(
        topStart = topStart * factor,
        topEnd = topEnd * factor,
        bottomEnd = bottomEnd * factor,
        bottomStart = bottomStart * factor,
    )

    companion object {
        /**
         * Uniform corner radii across all four corners.
         */
        fun uniform(radius: Dp): CornerRadii = CornerRadii(radius, radius, radius, radius)
    }
}

/**
 * Connected corner-radius system following Material 3 Expressive segmented containers.
 * Outer edges receive large rounding while inner edges between adjacent items stay tight.
 */
@Immutable
data class ConnectedShapeSpec(
    val outer: Dp,
    val inner: Dp,
) {
    /**
     * Computes corner radii for an item at [index] within a horizontal row of [count] items.
     */
    fun inRow(index: Int, count: Int): CornerRadii {
        val isFirst = index <= 0
        val isLast = index >= count - 1
        return CornerRadii(
            topStart = if (isFirst) outer else inner,
            bottomStart = if (isFirst) outer else inner,
            topEnd = if (isLast) outer else inner,
            bottomEnd = if (isLast) outer else inner,
        )
    }

    /**
     * Computes corner radii for an item at [index] within a vertical column of [count] items.
     */
    fun inColumn(index: Int, count: Int): CornerRadii {
        val isFirst = index <= 0
        val isLast = index >= count - 1
        return CornerRadii(
            topStart = if (isFirst) outer else inner,
            topEnd = if (isFirst) outer else inner,
            bottomStart = if (isLast) outer else inner,
            bottomEnd = if (isLast) outer else inner,
        )
    }
}

/**
 * Predefined connected shape specifications for settings cards and pill button groups.
 */
object ConnectedShapes {
    /** Expressive pill geometry for horizontal option button groups. */
    val Pill = ConnectedShapeSpec(
        outer = CornerRadiusExtraLarge,
        inner = CornerRadiusSmall,
    )

    /** Expressive card geometry for vertical segmented settings groups. */
    val Card = ConnectedShapeSpec(
        outer = CornerRadiusLargeIncreased,
        inner = CornerRadiusExtraSmall,
    )
}

/**
 * Smoothly animates a [CornerRadii] into a live [RoundedCornerShape].
 */
@Composable
fun animateCornersAsState(target: CornerRadii): RoundedCornerShape {
    val spec = remember { spring<Dp>(stiffness = Spring.StiffnessMediumLow) }
    val topStart by animateDpAsState(target.topStart, animationSpec = spec, label = "cornerTopStart")
    val topEnd by animateDpAsState(target.topEnd, animationSpec = spec, label = "cornerTopEnd")
    val bottomEnd by animateDpAsState(target.bottomEnd, animationSpec = spec, label = "cornerBottomEnd")
    val bottomStart by animateDpAsState(target.bottomStart, animationSpec = spec, label = "cornerBottomStart")
    return RoundedCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart,
    )
}

/**
 * Tracks a spring-animated 0f..1f press progress for [interactionSource].
 */
@Composable
fun rememberPressProgress(interactionSource: InteractionSource): State<Float> {
    val isPressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pressProgress",
    )
}

/**
 * Computes a press-responsive [RoundedCornerShape] that tightens corners while pressed.
 */
@Composable
fun rememberPressShape(
    base: CornerRadii,
    interactionSource: InteractionSource,
    pressedScale: Float = SettingsPressedCornerScale,
): RoundedCornerShape {
    val isPressed by interactionSource.collectIsPressedAsState()
    val target = if (isPressed) base.scaled(pressedScale) else base
    return animateCornersAsState(target)
}

/**
 * Clips the modifier to [shape] and applies a Material 3 ripple clickable handler.
 */
fun Modifier.shapedClickable(
    shape: Shape,
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    role: Role? = null,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier = composed {
    this
        .clip(shape)
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )
}

/**
 * Clips the modifier to [shape] and applies a Material 3 ripple toggleable handler.
 */
fun Modifier.shapedToggleable(
    value: Boolean,
    shape: Shape,
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    role: Role? = Role.Switch,
    onValueChange: (Boolean) -> Unit,
): Modifier = composed {
    this
        .clip(shape)
        .toggleable(
            value = value,
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            role = role,
            onValueChange = onValueChange,
        )
}
