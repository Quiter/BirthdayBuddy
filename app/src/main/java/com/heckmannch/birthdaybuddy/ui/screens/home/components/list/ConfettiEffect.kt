package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.util.lerp
import kotlin.random.Random

private data class Particle(
    val x: Float,
    val y: Float,
    val size: Size,
    val color: Color,
    val velocityX: Float,
    val velocityY: Float,
    val rotation: Float,
    val rotationSpeed: Float
)

@Composable
fun ConfettiEffect(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    particleCount: Int = 30,
    onAnimationEnd: () -> Unit = {}
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(3000, easing = LinearEasing)
        )
        onAnimationEnd()
    }

    val particles = remember(colors, particleCount) {
        if (colors.isEmpty()) {
            emptyList()
        } else {
            List(particleCount) {
                val particleSize = Random.nextFloat() * 10f + 5f
                Particle(
                    x = Random.nextFloat(),
                    y = Random.nextFloat() * -1f,
                    size = Size(particleSize, particleSize),
                    color = colors.random(),
                    velocityX = (Random.nextFloat() - 0.5f) * 0.2f,
                    velocityY = Random.nextFloat() * 0.5f + 0.5f,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = (Random.nextFloat() - 0.5f) * 10f
                )
            }
        }
    }

    if (progress.value < 1f) {
        Canvas(modifier = modifier) {
            val progressValue = progress.value
            particles.forEach { particle ->
                val currentY = lerp(particle.y * size.height, size.height * 1.5f, progressValue)
                val currentX = (particle.x * size.width) + (particle.velocityX * size.width * progressValue)
                val currentRotation = particle.rotation + (particle.rotationSpeed * progressValue * 360f)

                rotate(currentRotation, pivot = Offset(currentX, currentY)) {
                    drawRect(
                        color = particle.color,
                        topLeft = Offset(currentX, currentY),
                        size = particle.size
                    )
                }
            }
        }
    }
}
