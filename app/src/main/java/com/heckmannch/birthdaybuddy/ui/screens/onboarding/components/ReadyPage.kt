package com.heckmannch.birthdaybuddy.ui.screens.onboarding.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.illustrations.ReadyIllustration
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisSubtle
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
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

@Preview(showSystemUi = true)
@Composable
private fun ReadyPagePreview() {
    BirthdayBuddyTheme {
        ReadyPage(
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            hasContactPermission = true,
            notificationsEnabled = true,
            calendarSyncEnabled = true,
            onStart = {}
        )
    }
}
