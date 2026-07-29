package com.heckmannch.birthdaybuddy.ui.illustrations

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
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.ui.theme.AlphaBorderSubtle
import com.heckmannch.birthdaybuddy.ui.theme.AlphaContainerLow
import com.heckmannch.birthdaybuddy.ui.theme.AlphaOnboardingCalendarDisabled
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSurfaceContainerHigh
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayQuoteIconContainerSize
import com.heckmannch.birthdaybuddy.ui.theme.ContactAvatarHeaderSize
import com.heckmannch.birthdaybuddy.ui.theme.ElevationDefault
import com.heckmannch.birthdaybuddy.ui.theme.ElevationHigh
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.IllustrationCardContainerSize
import com.heckmannch.birthdaybuddy.ui.theme.IllustrationPreviewSize
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCalendarEventIconSize
import com.heckmannch.birthdaybuddy.ui.theme.SidebarHeaderSpacerHeight
import com.heckmannch.birthdaybuddy.ui.theme.SpacingLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

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
                .size(BirthdayQuoteIconContainerSize)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = AlphaContainerLow),
                    shape = CircleShape
                )
        )

        // Small floating accent circles/bubbles
        Box(
            modifier = Modifier
                .size(IconSizeExtraSmall)
                .align(Alignment.TopStart)
                .padding(start = SpacingNormal, top = SpacingNormal)
                .graphicsLayer {
                    translationY = -offsetY * 1.5f * density
                    alpha = AlphaSurfaceContainerHigh
                }
                .background(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = AlphaBorderSubtle),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(OnboardingCalendarEventIconSize)
                .align(Alignment.BottomEnd)
                .padding(end = SpacingLarge, bottom = SpacingNormal)
                .graphicsLayer {
                    translationY = offsetY * 1.2f * density
                    alpha = AlphaOnboardingCalendarDisabled
                }
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = AlphaBorderSubtle),
                    shape = CircleShape
                )
        )

        // Main Contacts Container (overlapping avatars)
        Box(
            modifier = Modifier
                .size(IllustrationCardContainerSize)
                .graphicsLayer {
                    translationY = offsetY * density
                },
            contentAlignment = Alignment.Center
        ) {
            // Left contact circle (secondary contact)
            Surface(
                modifier = Modifier
                    .size(IconSizeExtraLarge)
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        translationX = SpacingMedium.toPx()
                        translationY = -SpacingSmall.toPx()
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = ElevationDefault
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
                    .size(IconSizeExtraLarge)
                    .align(Alignment.CenterEnd)
                    .graphicsLayer {
                        translationX = -SpacingMedium.toPx()
                        translationY = SpacingSmall.toPx()
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                tonalElevation = ElevationDefault
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
                    .size(ContactAvatarHeaderSize)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = ElevationHigh
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(SidebarHeaderSpacerHeight),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactsIllustrationPreview() {
    BirthdayBuddyTheme {
        Box(
            modifier = Modifier
                .size(IllustrationPreviewSize)
                .padding(SpacingNormal),
            contentAlignment = Alignment.Center
        ) {
            ContactsIllustration()
        }
    }
}
