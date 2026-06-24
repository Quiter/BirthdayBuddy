package com.heckmannch.birthdaybuddy.ui.screens.onboarding.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppSwitch
import com.heckmannch.birthdaybuddy.ui.illustrations.CalendarIllustration
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisLow
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SearchBarHeight
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

@Composable
fun CalendarPage(
    windowWidthSizeClass: WindowWidthSizeClass,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    isGranted: Boolean,
    onGrant: () -> Unit,
) {
    OnboardingPageTemplate(
        windowWidthSizeClass = windowWidthSizeClass,
        illustration = { modifier ->
            CalendarIllustration(
                enabled = enabled,
                modifier = modifier
            )
        },
        title = stringResource(R.string.onboarding_calendar_title),
        description = stringResource(R.string.onboarding_calendar_desc),
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
                        headlineContent = { Text(stringResource(R.string.onboarding_calendar_enable)) },
                        trailingContent = {
                            AppSwitch(
                                checked = enabled,
                                onCheckedChange = onEnabledChange
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
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
                    Text(stringResource(R.string.onboarding_calendar_btn))
                }
            }
        } else {
            null
        }
    )
}

@Preview(showBackground = true)
@Composable
fun CalendarPagePreview_Enabled() {
    BirthdayBuddyTheme {
        CalendarPage(
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            enabled = true,
            onEnabledChange = {},
            isGranted = false,
            onGrant = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarPagePreview_Disabled() {
    BirthdayBuddyTheme {
        CalendarPage(
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            enabled = false,
            onEnabledChange = {},
            isGranted = false,
            onGrant = {}
        )
    }
}
