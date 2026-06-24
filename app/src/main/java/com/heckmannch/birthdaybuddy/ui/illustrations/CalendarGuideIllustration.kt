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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.theme.AlphaContainerMuted
import com.heckmannch.birthdaybuddy.ui.theme.AlphaContainerSubtle
import com.heckmannch.birthdaybuddy.ui.theme.AlphaOnboardingCard
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.ElevationOnboardingCard
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCalendarEventAlpha
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCardGuideHeight
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCardGuideWidth
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingGuideCheckIconSize
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingGuideSmallCornerRadius
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingGuideTextSpacer
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingIllustrationCircleSize
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

@Composable
fun CalendarGuideIllustration(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "guide_float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    Box(
        modifier = modifier.padding(top = SpacingNormal),
        contentAlignment = Alignment.Center
    ) {
        // Glowing background circle
        Box(
            modifier = Modifier
                .size(OnboardingIllustrationCircleSize)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = AlphaContainerMuted),
                    shape = CircleShape
                )
        )

        // Floating Drawer Card
        Card(
            modifier = Modifier
                .width(OnboardingCardGuideWidth)
                .height(OnboardingCardGuideHeight)
                .graphicsLayer {
                    translationY = offsetY * density
                },
            shape = RoundedCornerShape(SpacingNormal),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AlphaOnboardingCard)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = ElevationOnboardingCard)
        ) {
            Column(modifier = Modifier.padding(SpacingMedium)) {
                Text(
                    text = "Meine Kalender",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(OnboardingGuideTextSpacer))

                // Calendar item 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(IconSizeExtraSmall)
                            .background(
                                Color(0xFF4285F4),
                                shape = RoundedCornerShape(OnboardingGuideSmallCornerRadius)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(OnboardingGuideCheckIconSize)
                        )
                    }
                    Spacer(modifier = Modifier.width(SpacingSmall))
                    Text(
                        text = "Termine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = OnboardingCalendarEventAlpha)
                    )
                }

                Spacer(modifier = Modifier.height(SpacingSmall))

                // Calendar item 2 (BirthdayBuddy)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = AlphaContainerSubtle),
                            shape = RoundedCornerShape(SpacingExtraSmall)
                        )
                        .padding(SpacingExtraSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(IconSizeExtraSmall)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(OnboardingGuideSmallCornerRadius)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(OnboardingGuideCheckIconSize)
                        )
                    }
                    Spacer(modifier = Modifier.width(SpacingSmall))
                    Text(
                        text = "BirthdayBuddy",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(SpacingSmall))

                // Calendar item 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(IconSizeExtraSmall)
                            .background(
                                Color(0xFF0F9D58),
                                shape = RoundedCornerShape(OnboardingGuideSmallCornerRadius)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(OnboardingGuideCheckIconSize)
                        )
                    }
                    Spacer(modifier = Modifier.width(SpacingSmall))
                    Text(
                        text = "Feiertage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = OnboardingCalendarEventAlpha)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarGuideIllustrationPreview() {
    BirthdayBuddyTheme {
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CalendarGuideIllustration()
        }
    }
}
