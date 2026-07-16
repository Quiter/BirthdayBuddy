package com.heckmannch.birthdaybuddy.ui.screens.settings.otherevents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.components.InfoCard
import com.heckmannch.birthdaybuddy.ui.components.SettingsCard
import com.heckmannch.birthdaybuddy.ui.components.SettingsSwitchRow
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.NotificationIntent
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.NotificationViewModel
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OtherEventsSettingsContent(
    otherEventsEnabled: Boolean,
    onToggleChange: (Boolean) -> Unit,
    showBackButton: Boolean,
    onNavigateBack: () -> Unit,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    AppResponsiveScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.other_events_title)) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.notifications_back),
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = SpacingNormal,
                top = paddingValues.calculateTopPadding() + SpacingSmall,
                end = SpacingNormal,
                bottom = paddingValues.calculateBottomPadding() + SpacingSmall
            ),
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


