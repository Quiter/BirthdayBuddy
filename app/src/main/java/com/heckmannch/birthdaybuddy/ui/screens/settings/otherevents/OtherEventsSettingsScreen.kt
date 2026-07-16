package com.heckmannch.birthdaybuddy.ui.screens.settings.otherevents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.InfoCard
import com.heckmannch.birthdaybuddy.ui.components.SettingsCard
import com.heckmannch.birthdaybuddy.ui.components.SettingsDetailScaffold
import com.heckmannch.birthdaybuddy.ui.components.SettingsSwitchRow
import com.heckmannch.birthdaybuddy.ui.components.withSettingsInsets
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.NotificationIntent
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.NotificationViewModel
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

@Composable
fun OtherEventsSettingsScreen(
    viewModel: NotificationViewModel,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val otherEventsEnabled = uiState.otherEventsEnabled

    OtherEventsSettingsContent(
        otherEventsEnabled = otherEventsEnabled,
        onToggleChange = { viewModel.onIntent(NotificationIntent.SetOtherEventsEnabled(it)) },
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun OtherEventsSettingsContent(
    otherEventsEnabled: Boolean,
    onToggleChange: (Boolean) -> Unit,
    showBackButton: Boolean,
    onNavigateBack: () -> Unit,
) {
    SettingsDetailScaffold(
        title = stringResource(R.string.settings_other_events_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues.withSettingsInsets(),
            verticalArrangement = Arrangement.spacedBy(SpacingNormal)
        ) {
            item {
                SettingsCard {
                    SettingsSwitchRow(
                        title = stringResource(R.string.other_events_enable),
                        description = stringResource(R.string.other_events_desc),
                        checked = otherEventsEnabled,
                        onCheckedChange = onToggleChange,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null
                            )
                        }
                    )
                }
            }

            item {
                InfoCard(
                    title = stringResource(R.string.other_events_info_title),
                    description = stringResource(R.string.other_events_info_desc)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OtherEventsSettingsPreview() {
    MaterialTheme {
        OtherEventsSettingsContent(
            otherEventsEnabled = true,
            onToggleChange = {},
            showBackButton = true,
            onNavigateBack = {}
        )
    }
}


