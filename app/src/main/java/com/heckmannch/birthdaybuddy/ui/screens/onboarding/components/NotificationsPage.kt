package com.heckmannch.birthdaybuddy.ui.screens.onboarding.components

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.AlphaContainerMuted
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisLow
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisMedium
import com.heckmannch.birthdaybuddy.ui.theme.SearchBarHeight
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

@Composable
fun NotificationsPage(
    windowWidthSizeClass: WindowWidthSizeClass,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    persistent: Boolean,
    onPersistentChange: (Boolean) -> Unit,
    isGranted: Boolean,
    onGrant: () -> Unit,
) {
    OnboardingPageTemplate(
        windowWidthSizeClass = windowWidthSizeClass,
        illustration = { modifier ->
            NotificationsPageIllustration(
                enabled = enabled,
                persistent = persistent,
                modifier = modifier
            )
        },
        title = stringResource(R.string.onboarding_notif_page_title),
        description = stringResource(R.string.onboarding_notif_page_desc),
        settingsCard = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = AlphaEmphasisLow
                    )
                )
            ) {
                Column(modifier = Modifier.padding(SpacingSmall)) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.onboarding_notif_enable)) },
                        trailingContent = {
                            Switch(
                                checked = enabled,
                                onCheckedChange = onEnabledChange,
                                thumbContent = {
                                    Icon(
                                        imageVector = if (enabled) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    if (enabled) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.onboarding_notif_persistent)) },
                            trailingContent = {
                                Switch(
                                    checked = persistent,
                                    onCheckedChange = onPersistentChange,
                                    thumbContent = {
                                        Icon(
                                            imageVector = if (persistent) Icons.Default.Check else Icons.Default.Close,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                            contentDescription = null
                                        )
                                    }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        },
        actionButton = if (enabled && !isGranted) {
            {
                Button(
                    onClick = onGrant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SearchBarHeight)
                ) {
                    Text(stringResource(R.string.onboarding_notif_btn))
                }
            }
        } else {
            null
        }
    )
}

@Composable
private fun NotificationsPageIllustration(
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
        targetValue = if (enabled && persistent) 1.5.dp else 0.dp,
        label = "border_width"
    )
    val borderColor by animateColorAsState(
        targetValue = if (enabled && persistent) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "border_color"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.4f,
        label = "card_alpha"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Soft glowing background circle
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = if (enabled) MaterialTheme.colorScheme.secondary.copy(alpha = AlphaContainerMuted)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = CircleShape
                )
        )

        // Floating push notification Card
        Card(
            modifier = Modifier
                .width(220.dp)
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
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
                                .size(20.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cake,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
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
                            modifier = Modifier.size(14.dp),
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

