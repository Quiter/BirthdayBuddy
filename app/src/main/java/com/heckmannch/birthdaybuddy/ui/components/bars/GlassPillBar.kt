package com.heckmannch.birthdaybuddy.ui.components.bars

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Data holder for an action/tab in [FloatingPillBar].
 */
@Immutable
data class FloatingPillItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val selected: Boolean? = null,
    val contentDescription: String? = null,
    val modifier: Modifier = Modifier,
)

/**
 * Color defaults for the glassmorphic pill bar and items.
 */
object GlassPillDefaults {
    val Height: Dp = 64.dp
    val ActionHeight: Dp = 52.dp
    val Inset: Dp = 6.dp
    val SlotMaxWidth: Dp = 96.dp
    val LabelPadding: Dp = 4.dp
    val SelectedEdgeWidth: Dp = 1.dp

    @Composable
    fun containerColor(selected: Boolean = false): Color {
        val isDark = isSystemInDarkTheme()
        val base = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        val alpha = if (selected) {
            if (isDark) 0.55f else 0.72f
        } else {
            if (isDark) 0.35f else 0.6f
        }
        return base.copy(alpha = alpha)
    }

    @Composable
    fun contentColor(selected: Boolean = false): Color =
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun borderColor(selected: Boolean = false): Color {
        val isDark = isSystemInDarkTheme()
        val base = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        val alpha = if (selected) {
            if (isDark) 0.55f else 0.45f
        } else {
            if (isDark) 0.4f else 0.6f
        }
        return base.copy(alpha = alpha)
    }
}

/**
 * Floating glassmorphism pill navigation bar inspired by Morphe Manager.
 * Features background blur via Haze, animated selection indicator, and spring click feedback.
 */
@Composable
fun FloatingPillBar(
    items: List<FloatingPillItem>,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(max = GlassPillDefaults.Inset * 2 + GlassPillDefaults.SlotMaxWidth * items.size)
                    .fillMaxWidth()
                    .height(GlassPillDefaults.Height)
                    .glassFill(GlassPillDefaults.containerColor(), CircleShape),
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(1.dp, GlassPillDefaults.borderColor()),
            ) {
                BoxWithConstraints(modifier = Modifier.padding(horizontal = GlassPillDefaults.Inset)) {
                    val weights = pillLabelWeights(items.map { it.label }, maxWidth)
                    val slots = remember { mutableStateMapOf<Int, IntRange>() }

                    SelectionIndicator(
                        span = slots[items.indexOfFirst { it.selected == true }],
                        modifier = Modifier.align(AbsoluteAlignment.CenterLeft),
                    )
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items.forEachIndexed { index, item ->
                            PillAction(
                                item = item,
                                weight = weights?.get(index) ?: 1f,
                                showLabel = weights != null,
                                modifier = Modifier.onPlaced { coordinates ->
                                    val left = coordinates.positionInParent().x.roundToInt()
                                    slots[index] = left until (left + coordinates.size.width)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionIndicator(span: IntRange?, modifier: Modifier = Modifier) {
    span ?: return
    val left by animateIntAsState(
        targetValue = span.first,
        animationSpec = spring(stiffness = 400f, dampingRatio = 0.8f),
        label = "pill_indicator_left",
    )
    val width by animateIntAsState(
        targetValue = span.last + 1 - span.first,
        animationSpec = spring(stiffness = 400f, dampingRatio = 0.8f),
        label = "pill_indicator_width",
    )
    val density = LocalDensity.current
    val containerColor = GlassPillDefaults.containerColor(selected = true)
    val borderColor = GlassPillDefaults.borderColor(selected = true)

    Box(
        modifier = modifier
            .absoluteOffset { IntOffset(left, 0) }
            .width(with(density) { width.toDp() })
            .height(GlassPillDefaults.ActionHeight)
            .background(containerColor, CircleShape)
            .border(GlassPillDefaults.SelectedEdgeWidth, borderColor, CircleShape),
    )
}

@Composable
private fun RowScope.PillAction(
    item: FloatingPillItem,
    weight: Float,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
) {
    val isSelected = item.selected == true
    val contentColor by animateColorAsState(
        targetValue = GlassPillDefaults.contentColor(selected = isSelected),
        label = "pill_action_content",
    )
    val description = item.contentDescription ?: item.label
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = item.modifier
            .then(modifier)
            .weight(weight)
            .fillMaxWidth()
            .height(GlassPillDefaults.ActionHeight)
            .graphicsLayer {
                val scale = if (isPressed) 0.95f else 1f
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                role = if (item.selected != null) Role.Tab else Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    item.onClick()
                },
            )
            .semantics {
                this.contentDescription = description
                item.selected?.let { this.selected = it }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        if (showLabel) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                modifier = Modifier
                    .padding(horizontal = GlassPillDefaults.LabelPadding)
                    .clearAndSetSemantics {},
            )
        }
    }
}

@Composable
private fun pillLabelWeights(labels: List<String>, width: Dp): List<Float>? {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style: TextStyle = MaterialTheme.typography.labelSmall

    val labelWidths = remember(labels, style, density, measurer) {
        labels.map { label ->
            with(density) { measurer.measure(label, style, maxLines = 1).size.width.toDp() }
        }
    }

    return remember(labelWidths, width) {
        if (labels.isEmpty()) return@remember null
        val needed = labelWidths.map { it + GlassPillDefaults.LabelPadding * 2 }
        val total = needed.fold(0.dp) { sum, labelWidth -> sum + labelWidth }
        when {
            needed.all { it <= width / labels.size } -> labels.map { 1f }
            total <= width -> needed.map { (it + (width - total) / labels.size).value }
            else -> null
        }
    }
}
