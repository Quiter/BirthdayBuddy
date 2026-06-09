package com.heckmannch.birthdaybuddy.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.screens.settings.about.AboutScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.about.PrivacyPolicyScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.backup.BackupScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.calendar.CalendarSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.labels.LabelSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.NotificationSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.otherevents.OtherEventsSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.sync.SyncSettingsScreen
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.viewmodel.BackupViewModel
import com.heckmannch.birthdaybuddy.viewmodel.CalendarViewModel
import com.heckmannch.birthdaybuddy.viewmodel.HomeViewModel
import com.heckmannch.birthdaybuddy.viewmodel.LabelViewModel
import com.heckmannch.birthdaybuddy.viewmodel.NotificationViewModel

enum class SettingsTab {
    NOTIFICATIONS,
    CALENDAR,
    LABELS,
    BACKUP,
    SYNC,
    OTHER_EVENTS,
    ABOUT,
    PRIVACY_POLICY
}

@Composable
fun SettingsScreen(
    windowWidthSizeClass: WindowWidthSizeClass,
    homeViewModel: HomeViewModel?,
    onNavigateToLabels: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToOtherEvents: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    SettingsContent(
        windowWidthSizeClass = windowWidthSizeClass,
        homeViewModel = homeViewModel,
        onNavigateToSync = onNavigateToSync,
        onNavigateToLabels = onNavigateToLabels,
        onNavigateToNotifications = onNavigateToNotifications,
        onNavigateToCalendar = onNavigateToCalendar,
        onNavigateToBackup = onNavigateToBackup,
        onNavigateToAbout = onNavigateToAbout,
        onNavigateToOtherEvents = onNavigateToOtherEvents,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    windowWidthSizeClass: WindowWidthSizeClass,
    homeViewModel: HomeViewModel?,
    onNavigateToSync: () -> Unit,
    onNavigateToLabels: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToOtherEvents: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
        val scrollBehavior =
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
        AppResponsiveScaffold(
            windowWidthSizeClass = windowWidthSizeClass,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.notifications_back),
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_notifications_title)) },
                            supportingContent = { Text(stringResource(R.string.settings_notifications_desc)) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable { onNavigateToNotifications() }
                        )
                    }

                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_calendar_title)) },
                            supportingContent = { Text(stringResource(R.string.settings_calendar_desc)) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable { onNavigateToCalendar() }
                        )
                    }

                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_labels_title)) },
                            supportingContent = { Text(stringResource(R.string.settings_labels_desc)) },
                            leadingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.Label,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable { onNavigateToLabels() }
                        )
                    }

                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_backup_title)) },
                            supportingContent = { Text(stringResource(R.string.settings_backup_desc)) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable { onNavigateToBackup() }
                        )
                    }

                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_sync_title)) },
                            supportingContent = { Text(stringResource(R.string.settings_sync_desc)) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable { onNavigateToSync() }
                        )
                    }

                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_other_events_title)) },
                            supportingContent = { Text(stringResource(R.string.settings_other_events_desc)) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable { onNavigateToOtherEvents() }
                        )
                    }

                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_about_title)) },
                            supportingContent = { Text(stringResource(R.string.settings_about_desc)) },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable { onNavigateToAbout() }
                        )
                    }
                }

                SettingsFooter()
            }
        }
    } else {
        var activeTab by rememberSaveable { mutableStateOf(SettingsTab.NOTIFICATIONS) }

        val notificationViewModel: NotificationViewModel = hiltViewModel()
        val calendarViewModel: CalendarViewModel = hiltViewModel()
        val labelViewModel: LabelViewModel = hiltViewModel()
        val backupViewModel: BackupViewModel = hiltViewModel()

        AppResponsiveScaffold(
            windowWidthSizeClass = windowWidthSizeClass,
            useAdaptiveWidth = false,
            topBar = {}
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Linke Spalte: Menü (340.dp breit)
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    TopAppBar(
                        title = { Text(stringResource(R.string.settings_title)) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.notifications_back),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        item {
                            val isSelected = activeTab == SettingsTab.NOTIFICATIONS
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.settings_notifications_title)) },
                                supportingContent = { Text(stringResource(R.string.settings_notifications_desc)) },
                                leadingContent = {
                                    Icon(Icons.Default.Notifications, contentDescription = null)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified)
                                    .clickable { activeTab = SettingsTab.NOTIFICATIONS }
                            )
                        }

                        item {
                            val isSelected = activeTab == SettingsTab.CALENDAR
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.settings_calendar_title)) },
                                supportingContent = { Text(stringResource(R.string.settings_calendar_desc)) },
                                leadingContent = {
                                    Icon(Icons.Default.DateRange, contentDescription = null)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified)
                                    .clickable { activeTab = SettingsTab.CALENDAR }
                            )
                        }

                        item {
                            val isSelected = activeTab == SettingsTab.LABELS
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.settings_labels_title)) },
                                supportingContent = { Text(stringResource(R.string.settings_labels_desc)) },
                                leadingContent = {
                                    Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified)
                                    .clickable { activeTab = SettingsTab.LABELS }
                            )
                        }

                        item {
                            val isSelected = activeTab == SettingsTab.BACKUP
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.settings_backup_title)) },
                                supportingContent = { Text(stringResource(R.string.settings_backup_desc)) },
                                leadingContent = {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified)
                                    .clickable { activeTab = SettingsTab.BACKUP }
                            )
                        }

                        item {
                            val isSelected = activeTab == SettingsTab.SYNC
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.settings_sync_title)) },
                                supportingContent = { Text(stringResource(R.string.settings_sync_desc)) },
                                leadingContent = {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified)
                                    .clickable { activeTab = SettingsTab.SYNC }
                            )
                        }

                        item {
                            val isSelected = activeTab == SettingsTab.OTHER_EVENTS
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.settings_other_events_title)) },
                                supportingContent = { Text(stringResource(R.string.settings_other_events_desc)) },
                                leadingContent = {
                                    Icon(Icons.Default.Star, contentDescription = null)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified)
                                    .clickable { activeTab = SettingsTab.OTHER_EVENTS }
                            )
                        }

                        item {
                            val isSelected =
                                activeTab == SettingsTab.ABOUT || activeTab == SettingsTab.PRIVACY_POLICY
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.settings_about_title)) },
                                supportingContent = { Text(stringResource(R.string.settings_about_desc)) },
                                leadingContent = {
                                    Icon(Icons.Default.Info, contentDescription = null)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified)
                                    .clickable { activeTab = SettingsTab.ABOUT }
                            )
                        }
                    }

                    SettingsFooter()
                }

                // Trennlinie
                Spacer(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Rechte Spalte: Detail-Ansicht
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (activeTab) {
                        SettingsTab.NOTIFICATIONS -> {
                            NotificationSettingsScreen(
                                windowWidthSizeClass = windowWidthSizeClass,
                                viewModel = notificationViewModel,
                                showBackButton = false,
                                onNavigateBack = {}
                            )
                        }

                        SettingsTab.CALENDAR -> {
                            CalendarSettingsScreen(
                                windowWidthSizeClass = windowWidthSizeClass,
                                viewModel = calendarViewModel,
                                showBackButton = false,
                                onNavigateBack = {}
                            )
                        }

                        SettingsTab.LABELS -> {
                            LabelSettingsScreen(
                                windowWidthSizeClass = windowWidthSizeClass,
                                viewModel = labelViewModel,
                                showBackButton = false,
                                onNavigateBack = {}
                            )
                        }

                        SettingsTab.BACKUP -> {
                            BackupScreen(
                                windowWidthSizeClass = windowWidthSizeClass,
                                viewModel = backupViewModel,
                                showBackButton = false,
                                onNavigateBack = {}
                            )
                        }

                        SettingsTab.SYNC -> {
                            if (homeViewModel != null) {
                                SyncSettingsScreen(
                                    windowWidthSizeClass = windowWidthSizeClass,
                                    viewModel = homeViewModel,
                                    showBackButton = false,
                                    onNavigateBack = {}
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Sync is not available in preview")
                                }
                            }
                        }

                        SettingsTab.OTHER_EVENTS -> {
                            OtherEventsSettingsScreen(
                                windowWidthSizeClass = windowWidthSizeClass,
                                viewModel = notificationViewModel,
                                showBackButton = false,
                                onNavigateBack = {}
                            )
                        }

                        SettingsTab.ABOUT -> {
                            AboutScreen(
                                windowWidthSizeClass = windowWidthSizeClass,
                                showBackButton = false,
                                onNavigateBack = {},
                                onNavigateToPrivacyPolicy = {
                                    activeTab = SettingsTab.PRIVACY_POLICY
                                }
                            )
                        }

                        SettingsTab.PRIVACY_POLICY -> {
                            PrivacyPolicyScreen(
                                windowWidthSizeClass = windowWidthSizeClass,
                                showBackButton = false,
                                onNavigateBack = {
                                    activeTab = SettingsTab.ABOUT
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsFooter() {
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    BirthdayBuddyTheme {
        SettingsContent(
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            homeViewModel = null,
            onNavigateToSync = {},
            onNavigateToLabels = {},
            onNavigateToNotifications = {},
            onNavigateToCalendar = {},
            onNavigateToBackup = {},
            onNavigateToAbout = {},
            onNavigateToOtherEvents = {},
            onNavigateBack = {}
        )
    }
}
