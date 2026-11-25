package com.heckmannch.birthdaybuddy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.colorpicker.HsvColor
import com.heckmannch.birthdaybuddy.ui.components.colorpicker.HueSlider
import com.heckmannch.birthdaybuddy.ui.components.colorpicker.SaturationValuePanel
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

private fun Color.toHexString(): String {
    val argb = this.toArgb()
    return String.format("%06X", 0xFFFFFF and argb)
}

private fun String.toColorOrNull(): Color? {
    val clean = this.removePrefix("#").trim()
    if (clean.length != 6 || !clean.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }) return null
    return try {
        Color("#$clean".toColorInt())
    } catch (_: Exception) {
        null
    }
}

private fun Color.toHsv(): Triple<Float, Float, Float> {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    return Triple(hsv[0], hsv[1], hsv[2])
}

private val ComparisonSize = 56.dp

/**
 * Advanced Color Picker Dialog inspired by Morphe Manager's architecture.
 * Uses HSV (Hue, Saturation, Value) panels and live color comparison preview.
 */
@Composable
fun ColorPickerDialog(
    initialColor: Color,
    title: String,
    onDismissRequest: () -> Unit,
    onColorSelected: (Color) -> Unit,
    presets: List<Color> = emptyList()
) {
    val initialHsv = remember(initialColor) {
        val (h, s, v) = initialColor.toHsv()
        HsvColor(h, s, v)
    }

    var hsv by rememberSaveable(stateSaver = HsvColor.Saver) {
        mutableStateOf(initialHsv)
    }
    var hexInput by rememberSaveable(initialColor) {
        mutableStateOf(initialColor.toHexString())
    }
    var isHexError by rememberSaveable { mutableStateOf(false) }

    fun moveTo(updated: HsvColor) {
        hsv = updated
        hexInput = updated.color.toHexString()
        isHexError = false
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SpacingSmall),
                verticalArrangement = Arrangement.spacedBy(SpacingNormal)
            ) {
                // Header: Comparison preview and Hex field
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
                ) {
                    ColorComparison(
                        before = listOf(initialColor),
                        after = listOf(hsv.color)
                    )

                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() || it.uppercaseChar() in 'A'..'F' }
                            if (clean.length <= 6) {
                                hexInput = clean
                                val parsed = clean.toColorOrNull()
                                if (parsed != null) {
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
                        modifier = Modifier.weight(1f)
                    )
                }

                // Presets Grid
                if (presets.isNotEmpty()) {
                    ColorPresetGrid(
                        colors = presets,
                        selectedColor = hsv.color,
                        onSelect = { preset ->
                            val (h, s, v) = preset.toHsv()
                            moveTo(HsvColor(h, s, v))
                        }
                    )
                }

                // Saturation-Value Panel
                SaturationValuePanel(
                    hsv = hsv,
                    onChange = { s, v -> moveTo(hsv.copy(saturation = s, value = v)) },
                    height = 180.dp,
                    contentDescription = stringResource(R.string.color_picker_shade)
                )

                // Hue Slider
                HueSlider(
                    hue = hsv.hue,
                    onChange = { h -> moveTo(hsv.copy(hue = h)) },
                    contentDescription = stringResource(R.string.color_picker_hue)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalColor = hexInput.toColorOrNull() ?: hsv.color
                    onColorSelected(finalColor)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
private fun ColorComparison(before: List<Color>, after: List<Color>) {
    Row(
        modifier = Modifier
            .size(ComparisonSize)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
    ) {
        ComparisonHalf(before)
        ComparisonHalf(after)
    }
}

@Composable
private fun RowScope.ComparisonHalf(colors: List<Color>) {
    val solid by animateColorAsState(colors.first(), label = "color_comparison")
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .then(
                if (colors.size > 1) Modifier.background(Brush.verticalGradient(colors))
                else Modifier.background(solid)
            )
    )
}

@Composable
private fun ColorPresetGrid(
    colors: List<Color>,
    selectedColor: Color,
    onSelect: (Color) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SpacingSmall)
    ) {
        colors.chunked(6).forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpacingSmall, Alignment.CenterHorizontally)
            ) {
                rowColors.forEach { color ->
                    val isSelected = color.toArgb() == selectedColor.toArgb()
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                            .clickable { onSelect(color) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ColorPickerDialogPreview() {
    BirthdayBuddyTheme {
        ColorPickerDialog(
            initialColor = Color.Red,
            title = "Akzentfarbe wählen",
            onDismissRequest = {},
            onColorSelected = {},
            presets = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta)
        )
    }
}
