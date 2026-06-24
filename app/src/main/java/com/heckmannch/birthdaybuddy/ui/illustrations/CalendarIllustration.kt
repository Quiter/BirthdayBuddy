package com.heckmannch.birthdaybuddy.ui.illustrations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisSubtle
import com.heckmannch.birthdaybuddy.ui.theme.AlphaOnboardingCalendarDisabled
import com.heckmannch.birthdaybuddy.ui.theme.AlphaOnboardingCalendarInactive
import com.heckmannch.birthdaybuddy.ui.theme.AlphaOnboardingCard
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.ElevationOnboardingCard
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCalendarCellSize
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCalendarEventAlpha
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCalendarEventCornerRadius
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCalendarEventHeight
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCalendarEventIconSize
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCalendarEventPadding
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCalendarHeaderHeight
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCalendarIndicatorSize
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCardCalendarHeight
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCardCalendarWidth
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingIllustrationCircleSize
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

@Composable
fun CalendarIllustration(
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cal_float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Soft glowing background circle
        Box(
            modifier = Modifier
                .size(OnboardingIllustrationCircleSize)
                .background(
                    color = if (enabled) MaterialTheme.colorScheme.tertiary.copy(alpha = AlphaContainerMuted)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = AlphaOnboardingCalendarInactive),
                    shape = CircleShape
                )
        )

        // Floating Calendar Sheet
        Card(
            modifier = Modifier
                .width(OnboardingCardCalendarWidth)
                .height(OnboardingCardCalendarHeight)
                .graphicsLayer {
                    translationY = offsetY * density
                },
            shape = RoundedCornerShape(SpacingNormal),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AlphaOnboardingCard)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = ElevationOnboardingCard)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(OnboardingCalendarHeaderHeight)
                        .background(
                            if (enabled) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaOnboardingCalendarDisabled)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "JUNI",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // Grid representation
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(SpacingSmall),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(3) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            repeat(4) { col ->
                                val isEventDay = row == 1 && col == 2
                                Box(
                                    modifier = Modifier
                                        .size(OnboardingCalendarCellSize)
                                        .background(
                                            color = if (isEventDay && enabled) {
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            } else {
                                                Color.Transparent
                                            },
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isEventDay && enabled) {
                                        Text(
                                            text = "🎂",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(OnboardingCalendarIndicatorSize)
                                                .background(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                        alpha = AlphaEmphasisSubtle
                                                    ),
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Connected Event Card
                    AnimatedVisibility(
                        visible = enabled,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(OnboardingCalendarEventHeight)
                                .padding(horizontal = SpacingExtraSmall),
                            shape = RoundedCornerShape(OnboardingCalendarEventCornerRadius),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(
                                    alpha = OnboardingCalendarEventAlpha
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = OnboardingCalendarEventPadding),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(OnboardingCalendarEventIconSize),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(SpacingExtraSmall))
                                Text(
                                    text = "Erika's 30. Geb.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarIllustrationEnabledPreview() {
    BirthdayBuddyTheme {
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CalendarIllustration(enabled = true)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarIllustrationDisabledPreview() {
    BirthdayBuddyTheme {
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CalendarIllustration(enabled = false)
        }
    }
}
