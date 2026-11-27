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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.heckmannch.birthdaybuddy.ui.theme.AlphaColorPickerMarkerShadow
import com.heckmannch.birthdaybuddy.ui.theme.ColorPickerMarkerRadius
import com.heckmannch.birthdaybuddy.ui.theme.ColorPickerMarkerStroke
import com.heckmannch.birthdaybuddy.ui.theme.ColorPickerPanelDefaultHeight
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusMedium

/**
 * HSV color representation used by the color picker canvas controls.
 *
 * Keeps [hue] independently from [Color] so achromatic shades (saturation `0f`) do not lose the
 * selected hue axis when saved or restored.
 */
@Immutable
data class HsvColor(val hue: Float, val saturation: Float, val value: Float) {
    /** The Compose [Color] corresponding to this HSV coordinate. */
    val color: Color get() = Color.hsv(hue.coerceIn(0f, 360f), saturation, value)

    companion object {
        /** State saver preserving hue, saturation, and value across configuration changes. */
        val Saver = listSaver(
            save = { listOf(it.hue, it.saturation, it.value) },
            restore = { HsvColor(it[0], it[1], it[2]) },
        )
    }
}

/**
 * Matches the marker's full outer diameter so the handle fills the hue strip cleanly from edge to
 * edge. A stroked ring extends half its stroke width outside its radius.
 */
private val HueStripHeight = (ColorPickerMarkerRadius + ColorPickerMarkerStroke / 2) * 2

/** Evenly spaced 30-degree steps around the 360-degree hue wheel. */
private val HueStops = List(13) { Color.hsv(it * 30f, 1f, 1f) }

/**
 * Two-dimensional saturation (horizontal) and value/brightness (vertical) panel rendered over the
 * pure [hsv] hue. Both tap and drag gestures continuously report updated coordinates.
 */
@Composable
fun SaturationValuePanel(
    hsv: HsvColor,
    onChange: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = ColorPickerPanelDefaultHeight,
    contentDescription: String? = null,
) {
    val currentOnChange by rememberUpdatedState(onChange)

    fun PointerInputScope.reportPosition(position: Offset) {
        if (size.width <= 0 || size.height <= 0) return
        currentOnChange(
            (position.x / size.width).coerceIn(0f, 1f),
            1f - (position.y / size.height).coerceIn(0f, 1f),
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(CornerRadiusMedium))
            .semantics { contentDescription?.let { this.contentDescription = it } }
            .pointerInput(Unit) {
                detectTapGestures { reportPosition(it) }
            }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { reportPosition(it) }) { change, _ ->
                    change.consume()
                    reportPosition(change.position)
                }
            },
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hsv.hue, 1f, 1f))))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

        drawMarker(
            center = Offset(hsv.saturation * size.width, (1f - hsv.value) * size.height),
            fill = hsv.color,
            radius = ColorPickerMarkerRadius.toPx(),
        )
    }
}

/**
 * Horizontal full-spectrum hue slider (`0f..360f`). Always displays fully saturated hues so the
 * control reads as a color wheel spectrum independent of the current saturation or value.
 */
@Composable
fun HueSlider(
    hue: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = HueStripHeight,
    contentDescription: String? = null,
) {
    val currentOnChange by rememberUpdatedState(onChange)

    fun PointerInputScope.reportHue(x: Float) {
        if (size.width <= 0) return
        currentOnChange((x / size.width).coerceIn(0f, 1f) * 360f)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription?.let { this.contentDescription = it } }
            .pointerInput(Unit) {
                detectTapGestures { reportHue(it.x) }
            }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { reportHue(it.x) }) { change, _ ->
                    change.consume()
                    reportHue(change.position.x)
                }
            },
    ) {
        // Rounded in draw rather than clipped so the marker ring is never clipped at the ends.
        drawRoundRect(
            brush = Brush.horizontalGradient(HueStops),
            cornerRadius = CornerRadius(size.height / 2f),
        )

        drawMarker(
            center = Offset((hue / 360f) * size.width, size.height / 2f),
            fill = Color.hsv(hue, 1f, 1f),
            radius = ColorPickerMarkerRadius.toPx(),
        )
    }
}

/**
 * Draws the shared circular selection handle: picked color filled inside a white ring over a
 * subtle dark shadow so it stays legible against both bright and dark gradient regions.
 */
private fun DrawScope.drawMarker(center: Offset, fill: Color, radius: Float) {
    val stroke = ColorPickerMarkerStroke.toPx()
    val outer = radius + stroke / 2f
    val clamped = Offset(
        center.x.coerceIn(outer, (size.width - outer).coerceAtLeast(outer)),
        center.y.coerceIn(outer, (size.height - outer).coerceAtLeast(outer)),
    )

    drawCircle(Color.Black.copy(alpha = AlphaColorPickerMarkerShadow), outer, clamped)
    drawCircle(fill, radius, clamped)
    drawCircle(Color.White, radius, clamped, style = Stroke(stroke))
}
