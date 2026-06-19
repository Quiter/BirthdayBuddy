package com.heckmannch.birthdaybuddy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.ContactImageSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    title: String,
    onDismissRequest: () -> Unit,
    onColorSelected: (Color) -> Unit,
    presets: List<Color> = emptyList()
) {
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(0f) }
    var value by remember { mutableStateOf(0f) }
    var hexInput by remember { mutableStateOf("") }

    // Helper function to format HSV to HEX
    fun hsvToHex(h: Float, s: Float, v: Float): String {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
        return String.format("%06X", 0xFFFFFF and argb)
    }

    // Initialize states from initialColor
    LaunchedEffect(initialColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
        hexInput = String.format("%06X", 0xFFFFFF and initialColor.toArgb())
    }

    val hexRegex = Regex("^[0-9a-fA-F]{6}$")
    val isValid = hexInput.length == 6 && hexRegex.matches(hexInput)
    val selectedColor = if (isValid) {
        try {
            Color("#$hexInput".toColorInt())
        } catch (_: Exception) {
            Color.Transparent
        }
    } else {
        Color.Transparent
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpacingSmall)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(SpacingNormal)
            ) {
                // Presets Section
                if (presets.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.theme_accent_custom_dialog_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(SpacingSmall)
                    ) {
                        presets.chunked(4).forEach { rowPresets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
                            ) {
                                rowPresets.forEach { color ->
                                    val hexString =
                                        String.format("%06X", 0xFFFFFF and color.toArgb())
                                    val isPresetSelected =
                                        hexInput.uppercase() == hexString.uppercase()

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(ContactImageSizeSmall)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isPresetSelected) 3.dp else 1.dp,
                                                color = if (isPresetSelected) MaterialTheme.colorScheme.outline else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                val hsv = FloatArray(3)
                                                android.graphics.Color.colorToHSV(
                                                    color.toArgb(),
                                                    hsv
                                                )
                                                hue = hsv[0]
                                                saturation = hsv[1]
                                                value = hsv[2]
                                                hexInput = hexString
                                            }
                                    ) {
                                        if (isPresetSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(IconSizeSmall)
                                            )
                                        }
                                    }
                                }
                                // Filler for trailing elements if not full row
                                repeat(4 - rowPresets.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(SpacingExtraSmall))
                }

                // Saturation-Value Canvas
                SaturationValueBox(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onSaturationValueChange = { sat, valVal ->
                        saturation = sat
                        value = valVal
                        hexInput = hsvToHex(hue, saturation, value)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Hue Slider
                HueSlider(
                    hue = hue,
                    onHueChange = { newHue ->
                        hue = newHue
                        hexInput = hsvToHex(hue, saturation, value)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // HEX text field input
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        val cleanInput =
                            input.filter { it.isDigit() || it.uppercaseChar() in 'A'..'F' }
                        if (cleanInput.length <= 6) {
                            hexInput = cleanInput
                            if (cleanInput.length == 6) {
                                try {
                                    val parsedColor = Color("#$cleanInput".toColorInt())
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(parsedColor.toArgb(), hsv)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    value = hsv[2]
                                } catch (_: Exception) {
                                }
                            }
                        }
                    },
                    label = { Text("HEX Code") },
                    prefix = { Text("#") },
                    placeholder = { Text("e.g. FF5722") },
                    isError = hexInput.isNotEmpty() && !isValid,
                    supportingText = {
                        if (hexInput.isNotEmpty() && !isValid) {
                            Text(
                                text = stringResource(R.string.theme_accent_custom_invalid_hex),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Preview Row
                if (isValid) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = SpacingSmall),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(ContactImageSizeSmall)
                                .clip(CircleShape)
                                .background(selectedColor)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                        Text(
                            text = stringResource(R.string.color_picker_preview),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onColorSelected(selectedColor) },
                enabled = isValid
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

private fun fromHsv(hue: Float): Color {
    val calculatedArgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
    return Color(calculatedArgb)
}

@Composable
private fun SaturationValueBox(
    hue: Float,
    saturation: Float,
    value: Float,
    onSaturationValueChange: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f) // Maintain a perfect square
            .clip(MaterialTheme.shapes.small)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, fromHsv(hue))
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black)
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            val x = change.position.x.coerceIn(0f, size.width.toFloat())
                            val y = change.position.y.coerceIn(0f, size.height.toFloat())
                            val sat = x / size.width
                            val valVal = 1f - (y / size.height)
                            onSaturationValueChange(sat, valVal)
                        },
                        onDragStart = { offset ->
                            val x = offset.x.coerceIn(0f, size.width.toFloat())
                            val y = offset.y.coerceIn(0f, size.height.toFloat())
                            val sat = x / size.width
                            val valVal = 1f - (y / size.height)
                            onSaturationValueChange(sat, valVal)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val x = offset.x.coerceIn(0f, size.width.toFloat())
                        val y = offset.y.coerceIn(0f, size.height.toFloat())
                        val sat = x / size.width
                        val valVal = 1f - (y / size.height)
                        onSaturationValueChange(sat, valVal)
                    }
                }
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = saturation * size.width
            val cy = (1f - value) * size.height
            drawCircle(
                color = Color.Black,
                radius = SpacingSmall.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(), // Maintain precise drawing size
                center = Offset(cx, cy)
            )
        }
    }
}

@Composable
private fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val hueColors = remember {
        listOf(
            Color.Red,
            Color.Yellow,
            Color.Green,
            Color.Cyan,
            Color.Blue,
            Color.Magenta,
            Color.Red
        )
    }

    Box(
        modifier = modifier
            .height(SpacingNormal)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(brush = Brush.horizontalGradient(hueColors))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        change.consume()
                        val x = change.position.x.coerceIn(0f, size.width.toFloat())
                        onHueChange((x / size.width) * 360f)
                    },
                    onDragStart = { offset ->
                        val x = offset.x.coerceIn(0f, size.width.toFloat())
                        onHueChange((x / size.width) * 360f)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val x = offset.x.coerceIn(0f, size.width.toFloat())
                    onHueChange((x / size.width) * 360f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = (hue / 360f) * size.width
            val cy = size.height / 2f
            drawCircle(
                color = Color.Black,
                radius = SpacingSmall.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(), // Maintain precise drawing size
                center = Offset(cx, cy)
            )
        }
    }
}
