package com.heckmannch.birthdaybuddy.ui.illustrations

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.ConfettiEffect
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.IllustrationPreviewSize
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingIllustrationCircleSize
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

@Composable
fun ReadyIllustration(
    modifier: Modifier = Modifier
) {
    var animateCheck by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateCheck = true
    }

    val checkScale by animateFloatAsState(
        targetValue = if (animateCheck) 1.2f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkScale"
    )

    val confettiColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Confetti Celebration Effect
        ConfettiEffect(
            colors = confettiColors,
            modifier = Modifier.fillMaxSize(),
            particleCount = 40
        )

        // Growing Checkmark icon
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier
                .size(OnboardingIllustrationCircleSize)
                .graphicsLayer {
                    scaleX = checkScale
                    scaleY = checkScale
                },
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReadyIllustrationPreview() {
    BirthdayBuddyTheme {
        Box(
            modifier = Modifier
                .size(IllustrationPreviewSize)
                .padding(SpacingNormal),
            contentAlignment = Alignment.Center
        ) {
            ReadyIllustration()
        }
    }
}
