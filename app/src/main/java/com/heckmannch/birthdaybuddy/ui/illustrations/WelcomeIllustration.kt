package com.heckmannch.birthdaybuddy.ui.illustrations

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.ui.theme.AlphaContainerMuted
import com.heckmannch.birthdaybuddy.ui.theme.AlphaOnboardingCard
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.ElevationOnboardingCard
import com.heckmannch.birthdaybuddy.ui.theme.IllustrationPreviewSize
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCardWelcomeHeight
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCardWelcomeWidth
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingIllustrationCircleSize
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

@Composable
fun WelcomeIllustration(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "welcome_float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Background soft glowing circle
        Box(
            modifier = Modifier
                .size(OnboardingIllustrationCircleSize)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = AlphaContainerMuted),
                    shape = CircleShape
                )
        )

        // Floating Welcome Card
        Card(
            modifier = Modifier
                .size(width = OnboardingCardWelcomeWidth, height = OnboardingCardWelcomeHeight)
                .graphicsLayer {
                    translationY = offsetY * density
                },
            shape = RoundedCornerShape(SpacingNormal),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AlphaOnboardingCard)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = ElevationOnboardingCard
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(SpacingNormal),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    Text(
                        text = "Erika Mustermann",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(SpacingExtraSmall))
                    Text(
                        text = "30. Geb. • in 2 Tagen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeIllustrationPreview() {
    BirthdayBuddyTheme {
        Box(
            modifier = Modifier
                .size(IllustrationPreviewSize)
                .padding(SpacingNormal),
            contentAlignment = Alignment.Center
        ) {
            WelcomeIllustration()
        }
    }
}
