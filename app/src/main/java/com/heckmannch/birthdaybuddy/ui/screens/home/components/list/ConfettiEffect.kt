package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    particleCount: Int = 30
) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val particles = remember(colors) {
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

    Canvas(modifier = modifier) {
        particles.forEach { particle ->
            val currentY = lerp(particle.y * size.height, size.height * 1.5f, progress)
            val currentX = (particle.x * size.width) + (particle.velocityX * size.width * progress)
            val currentRotation = particle.rotation + (particle.rotationSpeed * progress * 360f)

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
