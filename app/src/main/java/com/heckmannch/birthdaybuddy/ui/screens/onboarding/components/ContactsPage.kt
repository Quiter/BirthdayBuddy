package com.heckmannch.birthdaybuddy.ui.screens.onboarding.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.ContactsIllustration
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisLow
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingIllustrationHeight
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingIllustrationHeightSmall
import com.heckmannch.birthdaybuddy.ui.theme.SearchBarHeight
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import com.heckmannch.birthdaybuddy.ui.theme.OnboardingIllustrationCircleSize

@Composable
fun ContactsPage(
    windowWidthSizeClass: WindowWidthSizeClass,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    isGranted: Boolean,
    onGrant: () -> Unit,
) {
    OnboardingPageTemplate(
        windowWidthSizeClass = windowWidthSizeClass,
        illustration = { modifier ->
            AnimatedContent(
                targetState = isGranted,
                modifier = modifier,
                transitionSpec = {
                    (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                },
                label = "contacts_illustration"
            ) { granted ->
                if (granted) {
                    Box(
                        modifier = Modifier.size(OnboardingIllustrationHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(OnboardingIllustrationCircleSize),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    ContactsIllustration(
                        modifier = Modifier.size(OnboardingIllustrationHeightSmall)
                    )
                }
            }
        },
        title = stringResource(R.string.onboarding_contacts_title),
        description = stringResource(R.string.onboarding_contacts_desc),
        settingsCard = {
            if (isGranted) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_contacts_granted),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
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
                            headlineContent = { Text(stringResource(R.string.settings_sync_title)) },
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
                    Text(stringResource(R.string.onboarding_contacts_btn))
                }
            }
        } else {
            null
        }
    )
}

