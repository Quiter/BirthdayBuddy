package com.heckmannch.birthdaybuddy.ui.screens.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun ConfettiEffect(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    // Konvertiere DP-Größen in Pixel für den Canvas
    val minSizePx = with(density) { 6.dp.toPx() }
    val maxSizePx = with(density) { 14.dp.toPx() }
    
    val animatable = remember { Animatable(0f) }
    
    val particles = remember(colors) {
        List(60) { // Etwas mehr Partikel für besseren Effekt
            ConfettiParticle(
                color = colors[Random.nextInt(colors.size)],
                initialX = Random.nextFloat(),
                initialY = -0.2f, // Startet etwas weiter oben
                speed = (Random.nextFloat() * 1.5f) + 1.0f, // Schnellerer Fall
                drift = (Random.nextFloat() * 300f) - 150f,
                rotationSpeed = (Random.nextFloat() * 1080f) - 540f,
                size = (Random.nextFloat() * (maxSizePx - minSizePx)) + minSizePx
            )
        }
    }

    LaunchedEffect(Unit) {
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = LinearOutSlowInEasing)
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val progress = animatable.value

        if (progress < 1f) {
            particles.forEach { particle ->
                // Berechne Position: y fällt von oben nach unten
                val y = (particle.initialY + progress * particle.speed) * height
                val x = (particle.initialX * width) + (progress * particle.drift)
                
                // Zeichne nur, wenn im sichtbaren Bereich (mit Puffer)
                if (y in -100f..height + 100f) {
                    rotate(
                        degrees = progress * particle.rotationSpeed,
                        pivot = Offset(x + particle.size / 2, y + particle.size / 4)
                    ) {
                        drawRect(
                            color = particle.color.copy(alpha = 1f - (progress * 0.8f)), // Sanfteres Fade-out
                            topLeft = Offset(x, y),
                            size = Size(particle.size, particle.size / 2)
                        )
                    }
                }
            }
        }
    }
}

private data class ConfettiParticle(
    val color: Color,
    val initialX: Float,
    val initialY: Float,
    val speed: Float,
    val drift: Float,
    val rotationSpeed: Float,
    val size: Float
)
