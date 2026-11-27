package com.heckmannch.birthdaybuddy.ui.components.colorpicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.heckmannch.birthdaybuddy.ui.theme.AlphaColorPickerMarkerShadow
import com.heckmannch.birthdaybuddy.ui.theme.ColorPickerMarkerRadius
import com.heckmannch.birthdaybuddy.ui.theme.ColorPickerMarkerStroke
import com.heckmannch.birthdaybuddy.ui.theme.ColorPickerPanelDefaultHeight
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium

/**
 * HSV color representation used for the advanced color picker canvas.
 */
@Immutable
data class HsvColor(val hue: Float, val saturation: Float, val value: Float) {
    val color: Color get() = Color.hsv(hue.coerceIn(0f, 360f), saturation, value)

    companion object {
        val Saver = listSaver(
            save = { listOf(it.hue, it.saturation, it.value) },
            restore = { HsvColor(it[0], it[1], it[2]) }
        )
    }
}

private val HueStripHeight = (ColorPickerMarkerRadius + ColorPickerMarkerStroke / 2) * 2
private val HueStops = List(13) { Color.hsv(it * 30f, 1f, 1f) }

/**
 * Saturation across, value down, over a background of the pure hue.
 */
@Composable
fun SaturationValuePanel(
    hsv: HsvColor,
    onChange: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = ColorPickerPanelDefaultHeight,
    contentDescription: String? = null
) {
    var size by remember { mutableStateOf(Size.Zero) }

    fun report(position: Offset) {
        if (size == Size.Zero) return
        onChange(
            (position.x / size.width).coerceIn(0f, 1f),
            1f - (position.y / size.height).coerceIn(0f, 1f)
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(SpacingMedium))
            .semantics { contentDescription?.let { this.contentDescription = it } }
            .pointerInput(Unit) {
                detectTapGestures { report(it) }
            }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { report(it) }) { change, _ ->
                    change.consume()
                    report(change.position)
                }
            }
    ) {
        size = this.size

        drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hsv.hue, 1f, 1f))))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

        drawMarker(
            center = Offset(hsv.saturation * size.width, (1f - hsv.value) * size.height),
            fill = hsv.color,
            radius = ColorPickerMarkerRadius.toPx()
        )
    }
}

/**
 * Hue slider for selecting color spectrum.
 */
@Composable
fun HueSlider(
    hue: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = HueStripHeight,
    contentDescription: String? = null
) {
    var width by remember { mutableFloatStateOf(0f) }

    fun report(x: Float) {
        if (width <= 0f) return
        onChange((x / width).coerceIn(0f, 1f) * 360f)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription?.let { this.contentDescription = it } }
            .pointerInput(Unit) {
                detectTapGestures { report(it.x) }
            }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { report(it.x) }) { change, _ ->
                    change.consume()
                    report(change.position.x)
                }
            }
    ) {
        width = size.width

        drawRoundRect(
            brush = Brush.horizontalGradient(HueStops),
            cornerRadius = CornerRadius(size.height / 2f)
        )

        drawMarker(
            center = Offset((hue / 360f) * size.width, size.height / 2f),
            fill = Color.hsv(hue, 1f, 1f),
            radius = ColorPickerMarkerRadius.toPx()
        )
    }
}

private fun DrawScope.drawMarker(center: Offset, fill: Color, radius: Float) {
    val stroke = ColorPickerMarkerStroke.toPx()
    val outer = radius + stroke / 2f
    val clamped = Offset(
        center.x.coerceIn(outer, (size.width - outer).coerceAtLeast(outer)),
        center.y.coerceIn(outer, (size.height - outer).coerceAtLeast(outer))
    )

    drawCircle(Color.Black.copy(alpha = AlphaColorPickerMarkerShadow), outer, clamped)
    drawCircle(fill, radius, clamped)
    drawCircle(Color.White, radius, clamped, style = Stroke(stroke))
}
