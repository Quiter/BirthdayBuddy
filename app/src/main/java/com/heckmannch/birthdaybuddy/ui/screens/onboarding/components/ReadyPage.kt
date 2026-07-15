package com.heckmannch.birthdaybuddy.ui.screens.onboarding.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppWidthSizeClass
import com.heckmannch.birthdaybuddy.ui.components.StepItem
import com.heckmannch.birthdaybuddy.ui.illustrations.ReadyIllustration
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisSubtle
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SearchBarHeight
import com.heckmannch.birthdaybuddy.ui.theme.SpacingLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

@Composable
fun ReadyPage(
    windowWidthSizeClass: AppWidthSizeClass,
    hasContactPermission: Boolean,
    notificationsEnabled: Boolean,
    calendarSyncEnabled: Boolean,
    onStart: () -> Unit,
) {
    OnboardingPageTemplate(
        windowWidthSizeClass = windowWidthSizeClass,
        illustration = { modifier ->
            ReadyIllustration(modifier)
        },
        title = stringResource(R.string.onboarding_ready_title),
        description = stringResource(R.string.onboarding_ready_desc),
        settingsCard = {
            // Zusammenfassung
            Card(
                shape = MaterialTheme.shapes.extraLarge,
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
                    Spacer(modifier = Modifier.height(SpacingNormal))

                    // Contacts Access
                    StepItem(
                        title = stringResource(R.string.onboarding_summary_contacts_title),
                        description = stringResource(
                            if (hasContactPermission) R.string.onboarding_summary_contacts_desc_enabled
                            else R.string.onboarding_summary_contacts_desc_disabled
                        ),
                        isCompleted = hasContactPermission,
                        isLocked = !hasContactPermission,
                        icon = if (!hasContactPermission) Icons.Default.Close else null
                    )

                    Spacer(modifier = Modifier.height(SpacingNormal))

                    // Notifications
                    StepItem(
                        title = stringResource(R.string.onboarding_summary_notif_title),
                        description = stringResource(
                            if (notificationsEnabled) R.string.onboarding_summary_notif_desc_enabled
                            else R.string.onboarding_summary_notif_desc_disabled
                        ),
                        isCompleted = notificationsEnabled,
                        isLocked = !notificationsEnabled,
                        icon = if (!notificationsEnabled) Icons.Default.Close else null
                    )

                    Spacer(modifier = Modifier.height(SpacingNormal))

                    // Calendar Sync
                    StepItem(
                        title = stringResource(R.string.onboarding_summary_calendar_title),
                        description = stringResource(
                            if (calendarSyncEnabled) R.string.onboarding_summary_calendar_desc_enabled
                            else R.string.onboarding_summary_calendar_desc_disabled
                        ),
                        isCompleted = calendarSyncEnabled,
                        isLocked = !calendarSyncEnabled,
                        icon = if (!calendarSyncEnabled) Icons.Default.Close else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingLarge))

            Text(
                text = if (hasContactPermission) stringResource(R.string.onboarding_ready_sync_info)
                else stringResource(R.string.onboarding_ready_no_sync_info),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = if (windowWidthSizeClass == AppWidthSizeClass.COMPACT) TextAlign.Center else TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        actionButton = {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SearchBarHeight)
                    .testTag("onboarding_start_button")
            ) {
                Text(stringResource(R.string.onboarding_ready_btn))
            }
        }
    )
}

@Preview(showSystemUi = true)
@Composable
private fun ReadyPagePreview() {
    BirthdayBuddyTheme {
        ReadyPage(
            windowWidthSizeClass = AppWidthSizeClass.COMPACT,
            hasContactPermission = true,
            notificationsEnabled = true,
            calendarSyncEnabled = true,
            onStart = {}
        )
    }
}
