package com.heckmannch.birthdaybuddy.ui.illustrations

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Lock
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
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisMedium
import com.heckmannch.birthdaybuddy.ui.theme.AlphaOnboardingCalendarDisabled
import com.heckmannch.birthdaybuddy.ui.theme.AlphaOnboardingCalendarInactive
import com.heckmannch.birthdaybuddy.ui.theme.AlphaOnboardingCard
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.ElevationOnboardingCard
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.IllustrationPreviewSize
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCalendarCellSize
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCalendarEventIconSize
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCalendarEventPadding
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCardBorderWidth
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCardNotifWidth
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingIllustrationCircleSize
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

@Composable
fun NotificationsIllustration(
    enabled: Boolean,
    persistent: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "notif_float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (enabled && persistent) OnboardingCardBorderWidth else 0.dp,
        label = "border_width"
    )
    val borderColor by animateColorAsState(
        targetValue = if (enabled && persistent) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "border_color"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else AlphaOnboardingCalendarDisabled,
        label = "card_alpha"
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
                    color = if (enabled) MaterialTheme.colorScheme.secondary.copy(alpha = AlphaContainerMuted)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = AlphaOnboardingCalendarInactive),
                    shape = CircleShape
                )
        )

        // Floating push notification Card
        Card(
            modifier = Modifier
                .width(OnboardingCardNotifWidth)
                .graphicsLayer {
                    translationY = offsetY * density
                    alpha = cardAlpha
                }
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(SpacingNormal)
                ),
            shape = RoundedCornerShape(SpacingNormal),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AlphaOnboardingCard)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = ElevationOnboardingCard)
        ) {
            Column(modifier = Modifier.padding(SpacingMedium)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(OnboardingCalendarCellSize)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cake,
                                contentDescription = null,
                                modifier = Modifier.size(OnboardingCalendarEventIconSize),
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(OnboardingCalendarEventPadding))
                        Text(
                            text = "BirthdayBuddy",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(SpacingExtraSmall))
                        Text(
                            text = "• jetzt",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaEmphasisMedium)
                        )
                    }

                    if (enabled && persistent) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(IconSizeExtraSmall),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(SpacingSmall))

                // Content
                Text(
                    text = "🎉 Erika hat heute Geb.!",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(SpacingExtraSmall))
                Text(
                    text = "Sie wird heute 30 Jahre alt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationsIllustrationEnabledPreview() {
    BirthdayBuddyTheme {
        Box(
            modifier = Modifier
                .size(IllustrationPreviewSize)
                .padding(SpacingNormal),
            contentAlignment = Alignment.Center
        ) {
            NotificationsIllustration(enabled = true, persistent = true)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationsIllustrationDisabledPreview() {
    BirthdayBuddyTheme {
        Box(
            modifier = Modifier
                .size(IllustrationPreviewSize)
                .padding(SpacingNormal),
            contentAlignment = Alignment.Center
        ) {
            NotificationsIllustration(enabled = false, persistent = false)
        }
    }
}
