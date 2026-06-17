package com.heckmannch.birthdaybuddy.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun ContactsIllustration(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "contacts_float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Soft glowing background circle
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape
                )
        )

        // Small floating accent circles/bubbles
        Box(
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 16.dp)
                .graphicsLayer {
                    translationY = -offsetY * 1.5f * density
                    alpha = 0.6f
                }
                .background(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(12.dp)
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 16.dp)
                .graphicsLayer {
                    translationY = offsetY * 1.2f * density
                    alpha = 0.4f
                }
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = CircleShape
                )
        )

        // Main Contacts Container (overlapping avatars)
        Box(
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer {
                    translationY = offsetY * density
                },
            contentAlignment = Alignment.Center
        ) {
            // Left contact circle (secondary contact)
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        translationX = 12.dp.toPx()
                        translationY = -8.dp.toPx()
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "👩",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            // Right contact circle (tertiary contact)
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterEnd)
                    .graphicsLayer {
                        translationX = -12.dp.toPx()
                        translationY = 8.dp.toPx()
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                tonalElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "👨",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            // Center main contact circle (larger, on top)
            Surface(
                modifier = Modifier
                    .size(68.dp)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
