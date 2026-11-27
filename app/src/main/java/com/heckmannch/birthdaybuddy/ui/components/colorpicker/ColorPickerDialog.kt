package com.heckmannch.birthdaybuddy.ui.components.colorpicker

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisLow
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThin
import com.heckmannch.birthdaybuddy.ui.theme.CalendarPresetColors
import com.heckmannch.birthdaybuddy.ui.theme.ColorPickerComparisonSize
import com.heckmannch.birthdaybuddy.ui.theme.ColorPickerDialogLandscapeMaxWidth
import com.heckmannch.birthdaybuddy.ui.theme.ColorPickerPanelHeight
import com.heckmannch.birthdaybuddy.ui.theme.ColorPickerPanelLandscapeHeight
import com.heckmannch.birthdaybuddy.ui.theme.SettingsContentMaxWidth
import com.heckmannch.birthdaybuddy.ui.theme.SpacingLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import com.heckmannch.birthdaybuddy.ui.util.toColorOrNull
import com.heckmannch.birthdaybuddy.ui.util.toHexString
import com.heckmannch.birthdaybuddy.ui.util.toHsv

private const val HexColorLength = 6

/**
 * Material 3 HSV color picker dialog featuring a saturation/value canvas, full-spectrum hue
 * slider, live before/after color comparison, hex input field, optional preset swatch grid, and
 * adaptive side-by-side layout in landscape orientation.
 *
 * @param initialColor Starting color shown as the "before" half and initial HSV selection.
 * @param title Dialog title displayed at the top.
 * @param onDismissRequest Invoked when the dialog is dismissed or cancelled.
 * @param onColorSelected Invoked with the confirmed [Color] when the user taps Save.
 * @param presets Optional preset swatches displayed for one-tap color selection.
 */
@Composable
fun ColorPickerDialog(
    initialColor: Color,
    title: String,
    onDismissRequest: () -> Unit,
    onColorSelected: (Color) -> Unit,
    presets: List<Color> = emptyList(),
) {
    val initialColorKey = initialColor.toArgb()
    val initialHsv = remember(initialColorKey) {
        val (h, s, v) = initialColor.toHsv()
        HsvColor(h, s, v)
    }

    var hsv by rememberSaveable(initialColorKey, stateSaver = HsvColor.Saver) {
        mutableStateOf(initialHsv)
    }
    var hexInput by rememberSaveable(initialColorKey) {
        mutableStateOf(initialColor.toHexString(includeHash = false))
    }
    var isHexError by rememberSaveable(initialColorKey) {
        mutableStateOf(false)
    }

    fun moveTo(updated: HsvColor) {
        hsv = updated
        hexInput = updated.color.toHexString(includeHash = false)
        isHexError = false
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val header: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium),
        ) {
            ColorComparison(
                before = initialColor,
                after = hsv.color,
            )

            OutlinedTextField(
                value = hexInput,
                onValueChange = { input ->
                    val clean = input
                        .filter { it.isDigit() || it.uppercaseChar() in 'A'..'F' }
                        .uppercase()
                    if (clean.length <= HexColorLength) {
                        hexInput = clean
                        val parsed = if (clean.length == HexColorLength) clean.toColorOrNull() else null
                        if (parsed != null) {
                            // Keep the active hue when an achromatic color (saturation == 0) is entered
                            // so the panel does not jump back to red.
                            val (h, s, v) = parsed.toHsv()
                            hsv = HsvColor(if (s == 0f) hsv.hue else h, s, v)
                            isHexError = false
                        } else {
                            isHexError = clean.isNotEmpty()
                        }
                    }
                },
                label = { Text(stringResource(R.string.color_picker_hex_label)) },
                prefix = { Text("#") },
                placeholder = { Text("RRGGBB") },
                isError = isHexError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }

    val swatches: @Composable () -> Unit = {
        if (presets.isNotEmpty()) {
            ColorPresetGrid(
                colors = presets,
                selected = hsv.color,
                onSelect = { preset ->
                    val (h, s, v) = preset.toHsv()
                    moveTo(HsvColor(if (s == 0f) hsv.hue else h, s, v))
                },
                columns = PICKER_PRESET_COLUMNS,
            )
        }
    }

    val panel: @Composable () -> Unit = {
        SaturationValuePanel(
            hsv = hsv,
            onChange = { s, v -> moveTo(hsv.copy(saturation = s, value = v)) },
            height = if (isLandscape) ColorPickerPanelLandscapeHeight else ColorPickerPanelHeight,
            contentDescription = stringResource(R.string.color_picker_shade),
        )
    }

    val strip: @Composable () -> Unit = {
        HueSlider(
            hue = hsv.hue,
            onChange = { h -> moveTo(hsv.copy(hue = h)) },
            contentDescription = stringResource(R.string.color_picker_hue),
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(horizontal = if (isLandscape) SpacingNormal else SpacingLarge)
            .widthIn(max = if (isLandscape) ColorPickerDialogLandscapeMaxWidth else SettingsContentMaxWidth),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            if (isLandscape) {
                // In landscape vertical space is tight and the canvas needs drag gestures rather
                // than competing with a vertical scroll container, so lay out controls side by side.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpacingNormal),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        panel()
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(SpacingMedium),
                    ) {
                        header()
                        if (presets.isNotEmpty()) {
                            swatches()
                        }
                        strip()
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SpacingSmall),
                    verticalArrangement = Arrangement.spacedBy(SpacingNormal),
                ) {
                    header()
                    if (presets.isNotEmpty()) {
                        swatches()
                    }
                    panel()
                    strip()
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalColor = if (hexInput.length == HexColorLength) {
                        hexInput.toColorOrNull() ?: hsv.color
                    } else {
                        hsv.color
                    }
                    onColorSelected(finalColor)
                    onDismissRequest()
                },
            ) {
                Text(stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

/**
 * Split-circle preview comparing the [before] color on the start half with the live [after]
 * selection on the end half.
 */
@Composable
private fun ColorComparison(before: Color, after: Color) {
    val animatedAfter by animateColorAsState(after, label = "color_comparison_after")

    Row(
        modifier = Modifier
            .size(ColorPickerComparisonSize)
            .clip(CircleShape)
            .border(
                BorderWidthThin,
                MaterialTheme.colorScheme.outline.copy(alpha = AlphaEmphasisLow),
                CircleShape,
            ),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(before),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(animatedAfter),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ColorPickerDialogPortraitPreview() {
    BirthdayBuddyTheme {
        ColorPickerDialog(
            initialColor = Color(0xFFE91E63),
            title = "Akzentfarbe wählen",
            onDismissRequest = {},
            onColorSelected = {},
            presets = CalendarPresetColors,
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 720,
    heightDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun ColorPickerDialogLandscapePreview() {
    BirthdayBuddyTheme {
        ColorPickerDialog(
            initialColor = Color(0xFF2196F3),
            title = "Akzentfarbe wählen",
            onDismissRequest = {},
            onColorSelected = {},
            presets = CalendarPresetColors,
        )
    }
}
