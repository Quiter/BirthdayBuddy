package com.heckmannch.birthdaybuddy.ui.components.colorpicker

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.settings.CornerRadii
import com.heckmannch.birthdaybuddy.ui.components.settings.rememberPressShape
import com.heckmannch.birthdaybuddy.ui.components.settings.shapedClickable
import com.heckmannch.birthdaybuddy.ui.theme.AlphaColorPickerSwatchDisabled
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisLow
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThin
import com.heckmannch.birthdaybuddy.ui.theme.CalendarPresetColors
import com.heckmannch.birthdaybuddy.ui.theme.ColorPickerCheckerCellSize
import com.heckmannch.birthdaybuddy.ui.theme.ColorPickerSelectedBorderDarkenFactor
import com.heckmannch.birthdaybuddy.ui.theme.ColorPickerSwatchMaxSize
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusMedium
import com.heckmannch.birthdaybuddy.ui.theme.SelectedBorderWidthThick
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import com.heckmannch.birthdaybuddy.ui.util.darken
import com.heckmannch.birthdaybuddy.ui.util.toHexString

/** Default number of swatches per row in full-width preset grids. */
const val PRESET_GRID_COLUMNS = 6

/** Compact number of swatches per row inside dialogs so presets occupy fewer vertical rows. */
const val PICKER_PRESET_COLUMNS = 8

private const val SolidAlphaThreshold = 0.5f
private val CheckerDark = Color(0xFFCCCCCC)

/**
 * Single preset entry in a [ColorPresetGrid].
 *
 * @param key Unique identifier used to match the currently selected swatch.
 * @param color Rendered swatch color.
 * @param label Optional accessibility label announced instead of the raw hex code.
 */
@Immutable
data class ColorPresetSwatch(
    val key: Any,
    val color: Color,
    val label: String? = null,
)

/**
 * Responsive preset color grid that lays out plain [colors] with up to [columns] swatches per row.
 * Swatches automatically shrink on narrow containers so rows stay balanced without overflow.
 */
@Composable
fun ColorPresetGrid(
    colors: List<Color>,
    selected: Color?,
    onSelect: (Color) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = PRESET_GRID_COLUMNS,
    enabled: Boolean = true,
) {
    val selectedArgb = selected?.toArgb()
    val presets = remember(colors) {
        colors.map { ColorPresetSwatch(key = it.toArgb(), color = it) }
    }

    ColorPresetGrid(
        presets = presets,
        selectedKey = selectedArgb,
        onSelect = { preset -> onSelect(preset.color) },
        modifier = modifier,
        columns = columns,
        enabled = enabled,
    )
}

/**
 * Responsive grid of [ColorPresetSwatch] items with expressive Material 3 shape morphing
 * (rounded square morphing into a pill when selected, tightening on press) and harmonized
 * selection borders.
 */
@Composable
fun ColorPresetGrid(
    presets: List<ColorPresetSwatch>,
    selectedKey: Any?,
    onSelect: (ColorPresetSwatch) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = PRESET_GRID_COLUMNS,
    enabled: Boolean = true,
) {
    val safeColumns = columns.coerceAtLeast(1)
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Compute swatch size in whole pixels so rounding never pushes the last item onto a new row.
        val swatchSize = with(density) {
            val spacingPx = SpacingSmall.roundToPx() * (safeColumns - 1)
            ((constraints.maxWidth - spacingPx).coerceAtLeast(0) / safeColumns).toDp()
        }.coerceAtMost(ColorPickerSwatchMaxSize)

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingSmall, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(SpacingSmall),
            maxItemsInEachRow = safeColumns,
        ) {
            presets.forEach { preset ->
                ColorSwatch(
                    color = preset.color,
                    selected = preset.key == selectedKey,
                    onClick = { onSelect(preset) },
                    label = preset.label,
                    enabled = enabled,
                    size = swatchSize,
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    size: Dp,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val baseCorners = CornerRadii.uniform(
        if (selected) CornerRadiusExtraLarge else CornerRadiusMedium
    )
    val shape = rememberPressShape(
        base = baseCorners,
        interactionSource = interactionSource,
    )

    val scheme = MaterialTheme.colorScheme
    val selectedLabel = stringResource(R.string.settings_state_selected)
    val notSelectedLabel = stringResource(R.string.settings_state_not_selected)
    val accessibilityLabel = label ?: remember(color) { color.toHexString() }

    val selectedBorder = if (color.alpha >= SolidAlphaThreshold) {
        color.darken(ColorPickerSelectedBorderDarkenFactor)
    } else {
        scheme.primary
    }

    val borderWidth by animateDpAsState(
        targetValue = if (selected) SelectedBorderWidthThick else BorderWidthThin,
        label = "swatch_border_width",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            selectedBorder
        } else {
            scheme.outline.copy(alpha = AlphaEmphasisLow)
        },
        label = "swatch_border_color",
    )
    val alpha = if (enabled) 1f else AlphaColorPickerSwatchDisabled

    Box(
        modifier = modifier
            .size(size)
            .shapedClickable(
                shape = shape,
                interactionSource = interactionSource,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .then(
                if (color.alpha < 1f) {
                    Modifier
                        .transparencyChecker()
                        .background(color.copy(alpha = color.alpha * alpha))
                } else {
                    Modifier.background(color.copy(alpha = alpha))
                }
            )
            .border(borderWidth, borderColor, shape)
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                contentDescription = accessibilityLabel
                stateDescription = if (selected) selectedLabel else notSelectedLabel
            },
    )
}

/**
 * Draws a checkerboard pattern behind semi-transparent swatches so alpha remains visually clear.
 */
private fun Modifier.transparencyChecker(): Modifier = drawBehind {
    val cell = ColorPickerCheckerCellSize.toPx()
    if (cell <= 0f) return@drawBehind
    drawRect(Color.White)
    var y = 0f
    var row = 0
    while (y < size.height) {
        var x = if (row % 2 == 0) 0f else cell
        while (x < size.width) {
            drawRect(CheckerDark, topLeft = Offset(x, y), size = Size(cell, cell))
            x += cell * 2
        }
        y += cell
        row++
    }
}

@Preview(showBackground = true)
@Composable
private fun ColorPresetGridPreview() {
    BirthdayBuddyTheme {
        ColorPresetGrid(
            colors = CalendarPresetColors,
            selected = CalendarPresetColors[2],
            onSelect = {},
            columns = PICKER_PRESET_COLUMNS,
        )
    }
}
