package com.heckmannch.birthdaybuddy.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.AlphaContainerLow
import com.heckmannch.birthdaybuddy.ui.theme.AlphaContainerSubtle
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisDisabled
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSettingsSelectionBorderDark
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSettingsSelectionBorderLight
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSettingsSelectionUnselectedDark
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSettingsSelectionUnselectedLight
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThin
import com.heckmannch.birthdaybuddy.ui.theme.SettingsAnimDurationMs
import com.heckmannch.birthdaybuddy.ui.theme.SettingsOptionCardHeightCompact
import com.heckmannch.birthdaybuddy.ui.theme.SettingsOptionIconSizeCompact
import com.heckmannch.birthdaybuddy.ui.theme.SettingsPressedWidthRatio
import com.heckmannch.birthdaybuddy.ui.theme.SettingsSegmentGap
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import kotlin.math.roundToInt

/**
 * Parent-data payload carrying an option tile's current press progress for [OptionGroup].
 */
@Immutable
internal data class ButtonGroupItemData(val pressProgress: Float = 0f)

private data class ButtonGroupItemModifier(val pressProgress: Float) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any =
        ButtonGroupItemData(pressProgress = pressProgress)
}

/**
 * Attaches live press-expansion progress to a child of [OptionGroup].
 */
fun Modifier.buttonGroupItem(pressProgress: Float): Modifier =
    this.then(ButtonGroupItemModifier(pressProgress))

/**
 * Distributes [availableWidth] across [pressProgresses] equal-base items, widening any pressed
 * item by up to [ratio] of a base slot while borrowing equally from its immediate neighbors.
 */
internal fun widenPressed(
    availableWidth: Int,
    pressProgresses: FloatArray,
    ratio: Float = SettingsPressedWidthRatio,
): IntArray {
    val count = pressProgresses.size
    if (count == 0) return IntArray(0)
    if (count == 1) return intArrayOf(availableWidth.coerceAtLeast(0))

    val base = availableWidth.toFloat() / count
    val widths = FloatArray(count) { base }

    for (i in 0 until count) {
        val progress = pressProgresses[i].coerceIn(0f, 1f)
        if (progress <= 0f) continue
        val extra = base * ratio * progress
        val hasLeft = i > 0
        val hasRight = i < count - 1
        val neighborCount = (if (hasLeft) 1 else 0) + (if (hasRight) 1 else 0)
        widths[i] += extra
        val borrow = extra / neighborCount
        if (hasLeft) widths[i - 1] -= borrow
        if (hasRight) widths[i + 1] -= borrow
    }

    val result = IntArray(count) { widths[it].roundToInt().coerceAtLeast(0) }
    val diff = availableWidth - result.sum()
    if (diff != 0) {
        result[count - 1] = (result[count - 1] + diff).coerceAtLeast(0)
    }
    return result
}

/**
 * Expressive horizontal connected button group for mutually exclusive option tiles,
 * where pressing a tile subtly expands its width and compresses its immediate neighbors.
 */
@Composable
fun OptionGroup(
    modifier: Modifier = Modifier,
    spacing: Dp = SettingsSegmentGap,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val count = measurables.size
        if (count == 0) return@Layout layout(constraints.minWidth, constraints.minHeight) {}

        val gapPx = spacing.roundToPx()
        val totalGaps = gapPx * (count - 1)
        val availableWidth = (constraints.maxWidth - totalGaps).coerceAtLeast(0)
        val presses = FloatArray(count) { idx ->
            (measurables[idx].parentData as? ButtonGroupItemData)?.pressProgress ?: 0f
        }
        val widths = widenPressed(availableWidth, presses)

        val placeables = measurables.mapIndexed { idx, measurable ->
            val w = widths[idx]
            measurable.measure(
                Constraints(
                    minWidth = w,
                    maxWidth = w,
                    minHeight = constraints.minHeight,
                    maxHeight = constraints.maxHeight,
                )
            )
        }
        val height = placeables.maxOfOrNull { it.height } ?: constraints.minHeight

        layout(constraints.maxWidth, height) {
            var x = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(x, 0)
                x += placeable.width + gapPx
            }
        }
    }
}

/**
 * Selectable expressive tile that participates in [ConnectedShapes.Pill] geometry,
 * press corner morphing, width expansion, and accessible selection semantics.
 */
@Composable
fun SelectionTile(
    selected: Boolean,
    onClick: () -> Unit,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minHeight: Dp = SettingsOptionCardHeightCompact,
    content: @Composable ColumnScope.(contentColor: Color) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.isDarkSurface()
    val interactionSource = remember { MutableInteractionSource() }
    val pressProgress by rememberPressProgress(interactionSource)
    val baseCorners = ConnectedShapes.Pill.inRow(index, count)
    val shape = rememberPressShape(base = baseCorners, interactionSource = interactionSource)

    val containerTarget = when {
        selected -> colorScheme.primary.copy(
            alpha = if (isDark) AlphaContainerSubtle + AlphaContainerLow else AlphaContainerSubtle
        )
        else -> colorScheme.surfaceContainerHighest.copy(
            alpha = if (isDark) AlphaSettingsSelectionUnselectedDark else AlphaSettingsSelectionUnselectedLight
        )
    }
    val contentTarget = when {
        !enabled -> colorScheme.onSurface.copy(alpha = AlphaEmphasisDisabled)
        selected -> colorScheme.onSurface
        else -> colorScheme.onSurfaceVariant
    }
    val borderTarget = if (selected) {
        colorScheme.primary.copy(
            alpha = if (isDark) AlphaSettingsSelectionBorderDark else AlphaSettingsSelectionBorderLight
        )
    } else {
        Color.Transparent
    }

    val colorSpec = remember { tween<Color>(durationMillis = SettingsAnimDurationMs) }
    val containerColor by animateColorAsState(containerTarget, animationSpec = colorSpec, label = "tileContainer")
    val contentColor by animateColorAsState(contentTarget, animationSpec = colorSpec, label = "tileContent")
    val borderColor by animateColorAsState(borderTarget, animationSpec = colorSpec, label = "tileBorder")

    val stateDesc = stringResource(
        if (selected) R.string.settings_state_selected else R.string.settings_state_not_selected
    )

    Box(
        modifier = modifier
            .buttonGroupItem(pressProgress)
            .defaultMinSize(minHeight = minHeight)
            .background(containerColor, shape)
            .border(BorderStroke(BorderWidthThin, borderColor), shape)
            .shapedClickable(
                shape = shape,
                interactionSource = interactionSource,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics {
                this.selected = selected
                this.stateDescription = stateDesc
            }
            .padding(horizontal = SpacingSmall, vertical = SpacingSmall),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SpacingExtraSmall, Alignment.CenterVertically),
        ) {
            content(contentColor)
        }
    }
}

/**
 * Expressive icon-and-label option card used inside an [OptionGroup] (e.g. Theme Mode selector).
 */
@Composable
fun ModernIconOptionCard(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SelectionTile(
        selected = selected,
        onClick = onClick,
        index = index,
        count = count,
        modifier = modifier,
        enabled = enabled,
    ) { contentColor ->
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected && enabled) MaterialTheme.colorScheme.primary else contentColor,
            modifier = Modifier.size(SettingsOptionIconSizeCompact),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
