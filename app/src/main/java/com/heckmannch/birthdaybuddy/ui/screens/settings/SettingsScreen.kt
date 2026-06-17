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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.runtime.remember
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
import com.heckmannch.birthdaybuddy.ui.screens.settings.theme.ThemeSettingsScreen
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.viewmodel.BackupViewModel
import com.heckmannch.birthdaybuddy.viewmodel.CalendarViewModel
import com.heckmannch.birthdaybuddy.viewmodel.HomeViewModel
import com.heckmannch.birthdaybuddy.viewmodel.LabelViewModel
import com.heckmannch.birthdaybuddy.viewmodel.NotificationViewModel
import com.heckmannch.birthdaybuddy.viewmodel.ThemeViewModel

enum class SettingsTab {
    NOTIFICATIONS,
    CALENDAR,
    LABELS,
    BACKUP,
    THEME,
    SYNC,
    OTHER_EVENTS,
    ABOUT,
    PRIVACY_POLICY
}

private data class SettingsMenuItemData(
    val titleRes: Int,
    val descRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tab: SettingsTab,
    val onClick: () -> Unit
)

@Composable
fun SettingsScreen(
    windowWidthSizeClass: WindowWidthSizeClass,
    homeViewModel: HomeViewModel?,
    onNavigateToLabels: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToTheme: () -> Unit,
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
        onNavigateToTheme = onNavigateToTheme,
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
    onNavigateToTheme: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToOtherEvents: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val menuItems = remember(
        onNavigateToNotifications,
        onNavigateToCalendar,
        onNavigateToLabels,
        onNavigateToBackup,
        onNavigateToTheme,
        onNavigateToSync,
        onNavigateToOtherEvents,
        onNavigateToAbout
    ) {
        listOf(
            SettingsMenuItemData(
                titleRes = R.string.settings_notifications_title,
                descRes = R.string.settings_notifications_desc,
                icon = Icons.Default.Notifications,
                tab = SettingsTab.NOTIFICATIONS,
                onClick = onNavigateToNotifications
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_calendar_title,
                descRes = R.string.settings_calendar_desc,
                icon = Icons.Default.DateRange,
                tab = SettingsTab.CALENDAR,
                onClick = onNavigateToCalendar
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_labels_title,
                descRes = R.string.settings_labels_desc,
                icon = Icons.AutoMirrored.Filled.Label,
                tab = SettingsTab.LABELS,
                onClick = onNavigateToLabels
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_backup_title,
                descRes = R.string.settings_backup_desc,
                icon = Icons.Default.Share,
                tab = SettingsTab.BACKUP,
                onClick = onNavigateToBackup
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_theme_title,
                descRes = R.string.settings_theme_desc,
                icon = Icons.Default.Palette,
                tab = SettingsTab.THEME,
                onClick = onNavigateToTheme
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_sync_title,
                descRes = R.string.settings_sync_desc,
                icon = Icons.Default.Refresh,
                tab = SettingsTab.SYNC,
                onClick = onNavigateToSync
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_other_events_title,
                descRes = R.string.settings_other_events_desc,
                icon = Icons.Default.Star,
                tab = SettingsTab.OTHER_EVENTS,
                onClick = onNavigateToOtherEvents
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_about_title,
                descRes = R.string.settings_about_desc,
                icon = Icons.Default.Info,
                tab = SettingsTab.ABOUT,
                onClick = onNavigateToAbout
            )
        )
    }

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
                    items(menuItems) { item ->
                        SettingsMenuItem(
                            titleRes = item.titleRes,
                            descRes = item.descRes,
                            icon = item.icon,
                            onClick = item.onClick
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
        val themeViewModel: ThemeViewModel = hiltViewModel()

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
                        items(menuItems) { item ->
                            val isSelected = when (item.tab) {
                                SettingsTab.ABOUT -> activeTab == SettingsTab.ABOUT || activeTab == SettingsTab.PRIVACY_POLICY
                                else -> activeTab == item.tab
                            }
                            SettingsMenuItem(
                                titleRes = item.titleRes,
                                descRes = item.descRes,
                                icon = item.icon,
                                isSelected = isSelected,
                                useTabletStyle = true,
                                onClick = { activeTab = item.tab }
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

                        SettingsTab.THEME -> {
                            ThemeSettingsScreen(
                                windowWidthSizeClass = windowWidthSizeClass,
                                viewModel = themeViewModel,
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

@Composable
private fun SettingsMenuItem(
    titleRes: Int,
    descRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean = false,
    useTabletStyle: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected && useTabletStyle) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected && useTabletStyle) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val supportingColor = if (isSelected && useTabletStyle) {
        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val iconColor = if (isSelected && useTabletStyle) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val itemModifier = if (useTabletStyle) {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable { onClick() }
    } else {
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    }

    Box(modifier = itemModifier) {
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(titleRes),
                    color = contentColor
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(descRes),
                    color = supportingColor
                )
            },
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
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
            onNavigateToTheme = {},
            onNavigateToAbout = {},
            onNavigateToOtherEvents = {},
            onNavigateBack = {}
        )
    }
}
