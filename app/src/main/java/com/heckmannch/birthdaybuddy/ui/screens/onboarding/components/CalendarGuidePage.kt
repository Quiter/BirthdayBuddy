package com.heckmannch.birthdaybuddy.ui.screens.onboarding.components

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.util.Log
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.AlphaContainerMuted
import com.heckmannch.birthdaybuddy.ui.theme.AlphaContainerSubtle
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisLow
import com.heckmannch.birthdaybuddy.ui.theme.AlphaOnboardingCard
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.ElevationOnboardingCard
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCalendarCellSize
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCalendarEventAlpha
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCardGuideHeight
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingCardGuideWidth
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingGuideCheckIconSize
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingGuideCircleSize
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingGuideIconSize
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingGuideSmallCornerRadius
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingGuideTextSpacer
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingGuideTinySpacer
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingIllustrationCircleSize
import com.heckmannch.birthdaybuddy.ui.theme.SearchBarHeight
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

@Composable
fun CalendarGuidePage(windowWidthSizeClass: WindowWidthSizeClass) {
    val context = LocalContext.current

    OnboardingPageTemplate(
        windowWidthSizeClass = windowWidthSizeClass,
        illustration = { modifier ->
            CalendarGuideIllustration(modifier)
        },
        title = stringResource(R.string.onboarding_calendar_guide_title),
        description = stringResource(R.string.onboarding_calendar_guide_desc),
        settingsCard = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AlphaEmphasisLow)
                )
            ) {
                Column(modifier = Modifier.padding(SpacingNormal)) {
                    // Step 1
                    GuideStepItem(
                        title = stringResource(R.string.onboarding_calendar_guide_step1_title),
                        description = stringResource(R.string.onboarding_calendar_guide_step1_desc),
                        icon = Icons.Default.PlayArrow
                    )

                    Spacer(modifier = Modifier.height(SpacingNormal))

                    // Step 2
                    GuideStepItem(
                        title = stringResource(R.string.onboarding_calendar_guide_step2_title),
                        description = stringResource(R.string.onboarding_calendar_guide_step2_desc),
                        icon = Icons.Default.Menu
                    )

                    Spacer(modifier = Modifier.height(SpacingNormal))

                    // Step 3
                    GuideStepItem(
                        title = stringResource(R.string.onboarding_calendar_guide_step3_title),
                        description = stringResource(R.string.onboarding_calendar_guide_step3_desc),
                        icon = Icons.Default.Check
                    )
                }
            }
        },
        actionButton = {
            Button(
                onClick = { openDefaultCalendarApp(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SearchBarHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(OnboardingCalendarCellSize)
                    )
                    Spacer(modifier = Modifier.width(SpacingSmall))
                    Text(stringResource(R.string.onboarding_calendar_guide_btn))
                }
            }
        }
    )
}

@Composable
private fun CalendarGuideIllustration(modifier: Modifier = Modifier) {
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

@Composable
private fun GuideStepItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(OnboardingGuideCircleSize)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = AlphaContainerSubtle),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(OnboardingGuideIconSize)
            )
        }

        Spacer(modifier = Modifier.width(SpacingMedium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(OnboardingGuideTinySpacer))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun openDefaultCalendarApp(context: Context) {
    try {
        val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
        android.content.ContentUris.appendId(builder, System.currentTimeMillis())
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = builder.build()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_CALENDAR)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e2: Exception) {
            Log.e("CalendarSyncRepo", "Could not open calendar app", e2)
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun CalendarGuidePagePreview() {
    BirthdayBuddyTheme {
        CalendarGuidePage(windowWidthSizeClass = WindowWidthSizeClass.Compact)
    }
}


