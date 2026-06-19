package com.heckmannch.birthdaybuddy.ui.screens.onboarding.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.ConfettiEffect
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisSubtle
import com.heckmannch.birthdaybuddy.ui.theme.SearchBarHeight
import com.heckmannch.birthdaybuddy.ui.theme.SpacingLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

@Composable
fun ReadyPage(
    windowWidthSizeClass: WindowWidthSizeClass,
    hasContactPermission: Boolean,
    notificationsEnabled: Boolean,
    calendarSyncEnabled: Boolean,
    onStart: () -> Unit,
) {
    OnboardingPageTemplate(
        windowWidthSizeClass = windowWidthSizeClass,
        illustration = { modifier ->
            ReadyPageIllustration(modifier)
        },
        title = stringResource(R.string.onboarding_ready_title),
        description = stringResource(R.string.onboarding_ready_desc),
        settingsCard = {
            // Zusammenfassung
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = AlphaEmphasisSubtle
                    )
                )
            ) {
                Column(modifier = Modifier.padding(SpacingNormal)) {
                    Text(
                        text = stringResource(R.string.onboarding_summary_header),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(SpacingSmall))
                    Text(
                        text = if (hasContactPermission) stringResource(R.string.onboarding_summary_contacts_enabled)
                        else stringResource(R.string.onboarding_summary_contacts_disabled),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (notificationsEnabled) stringResource(R.string.onboarding_summary_notif_enabled)
                        else stringResource(R.string.onboarding_summary_notif_disabled),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (calendarSyncEnabled) stringResource(R.string.onboarding_summary_calendar_enabled)
                        else stringResource(R.string.onboarding_summary_calendar_disabled),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingLarge))

            Text(
                text = if (hasContactPermission) stringResource(R.string.onboarding_ready_sync_info)
                else stringResource(R.string.onboarding_ready_no_sync_info),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = if (windowWidthSizeClass == WindowWidthSizeClass.Compact) TextAlign.Center else TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        actionButton = {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SearchBarHeight)
            ) {
                Text(stringResource(R.string.onboarding_ready_btn))
            }
        }
    )
}

@Composable
private fun ReadyPageIllustration(modifier: Modifier = Modifier) {
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
                .size(100.dp)
                .graphicsLayer {
                    scaleX = checkScale
                    scaleY = checkScale
                },
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

