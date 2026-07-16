package com.heckmannch.birthdaybuddy.ui.screens.settings

import androidx.window.core.layout.WindowSizeClass
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowSizeClass
import com.heckmannch.birthdaybuddy.ui.components.isWidthCompact
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeViewModel
import com.heckmannch.birthdaybuddy.ui.screens.settings.about.AboutScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.about.PrivacyPolicyScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.backup.BackupScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.backup.BackupViewModel
import com.heckmannch.birthdaybuddy.ui.screens.settings.calendar.CalendarSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.calendar.CalendarViewModel
import com.heckmannch.birthdaybuddy.ui.screens.settings.labels.LabelSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.labels.LabelViewModel
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.NotificationSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.NotificationViewModel
import com.heckmannch.birthdaybuddy.ui.screens.settings.otherevents.OtherEventsSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.sync.SyncSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.theme.ThemeSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.theme.ThemeViewModel
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisMedium
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingTiny
import kotlinx.serialization.Serializable

/**
 * Represents the available settings sections (tabs/screens) in the app.
 */
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

/**
 * Data holder for items displayed in the settings menu list.
 *
 * @property titleRes String resource ID for the item's title.
 * @property descRes String resource ID for the item's description.
 * @property icon The icon vector to be displayed next to the title.
 * @property tab The corresponding [SettingsTab] for this menu item.
 * @property onClick Action to trigger when the menu item is clicked.
 */
private data class SettingsMenuItemData(
    val titleRes: Int,
    val descRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tab: SettingsTab,
    val onClick: () -> Unit
)

/**
 * The main settings screen entry point.
 * It forwards the navigation callbacks and parameters to [SettingsContent].
 */
@Composable
fun SettingsScreen(
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

/**
 * Displays the actual settings content, switching dynamically between a single-column layout
 * for compact screens (phones) and a side-by-side split-pane layout for wider screens (tablets/foldables).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SettingsContent(
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
    val windowSizeClass = LocalWindowSizeClass.current
    // List of menu items to display in the main settings screen list.
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

    if (windowSizeClass.isWidthCompact) {
        // --- MOBILE/COMPACT LAYOUT ---
        // Displays a scrollable settings list on a single screen.
        // Clicking an item navigates away to a separate screen.
        val scrollBehavior =
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
        AppResponsiveScaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.notifications_back),
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = paddingValues
                ) {
                    items(menuItems, key = { it.tab }) { item ->
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
        // --- TABLET/EXPANDED LAYOUT ---
        // Implements a split-pane Master-Detail screen using Jetpack Navigation 3.
        // Left side displays the list (master), right side displays the selected screen (detail).

        val windowAdaptiveInfo = LocalWindowAdaptiveInfo.current

        // Define how panes partition the screen. We disable partition spacing for a seamless look.
        val directive = remember(windowAdaptiveInfo) {
            calculatePaneScaffoldDirective(windowAdaptiveInfo)
                .copy(horizontalPartitionSpacerSize = 0.dp)
        }

        // Strategy used by Navigation 3 to determine pane transitions/positioning.
        val listDetailStrategy =
            rememberListDetailSceneStrategy<SettingsNavKey>(directive = directive)

        // Tracks the currently active/selected settings tab on tablets.
        var activeTab by rememberSaveable { mutableStateOf(SettingsTab.NOTIFICATIONS) }

        // Construct the backstack keys dynamically.
        // For standard tabs: [SettingsMenu, SettingsDetail(activeTab)]
        // For Privacy Policy: [SettingsMenu, SettingsDetail(ABOUT), SettingsDetail(PRIVACY_POLICY)] (hierarchical flow)
        val backStack = remember(activeTab) {
            if (activeTab == SettingsTab.PRIVACY_POLICY) {
                listOf<SettingsNavKey>(
                    SettingsNavKey.SettingsMenu,
                    SettingsNavKey.SettingsDetail(SettingsTab.ABOUT),
                    SettingsNavKey.SettingsDetail(SettingsTab.PRIVACY_POLICY)
                )
            } else {
                listOf<SettingsNavKey>(
                    SettingsNavKey.SettingsMenu,
                    SettingsNavKey.SettingsDetail(activeTab)
                )
            }
        }

        // ViewModels instantiated once here and shared with the active detail screens.
        val notificationViewModel: NotificationViewModel = hiltViewModel()
        val calendarViewModel: CalendarViewModel = hiltViewModel()
        val labelViewModel: LabelViewModel = hiltViewModel()
        val backupViewModel: BackupViewModel = hiltViewModel()
        val themeViewModel: ThemeViewModel = hiltViewModel()

        AppResponsiveScaffold(
            useAdaptiveWidth = false,
            topBar = {}
        ) { paddingValues ->
            // NavDisplay manages the rendering of screens corresponding to the backStack.
            NavDisplay(
                backStack = backStack,
                onBack = {
                    if (activeTab == SettingsTab.PRIVACY_POLICY) {
                        activeTab = SettingsTab.ABOUT
                    }
                },
                sceneStrategies = listOf(listDetailStrategy),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                entryProvider = { key ->
                    when (key) {
                        is SettingsNavKey.SettingsMenu -> NavEntry(
                            key,
                            metadata = ListDetailSceneStrategy.listPane(
                                detailPlaceholder = {
                                    Box(modifier = Modifier.fillMaxSize())
                                }
                            )) {
                            // Left Pane: Settings Master Menu
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            ) {
                                TopAppBar(
                                    title = { Text(stringResource(R.string.settings_title)) },
                                    navigationIcon = {
                                        IconButton(
                                            onClick = onNavigateBack,
                                            modifier = Modifier.testTag("settings_back_button")
                                        ) {
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
                                    items(menuItems, key = { it.tab }) { item ->
                                        // Highlights the item if it matches the current active tab
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
                                            onClick = {
                                                activeTab = item.tab
                                            }
                                        )
                                    }
                                }

                                SettingsFooter()
                            }
                        }

                        is SettingsNavKey.SettingsDetail -> NavEntry(
                            key,
                            metadata = ListDetailSceneStrategy.detailPane()
                        ) {
                            // Right Pane: Active Settings Detail Screen
                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                when (key.tab) {
                                    SettingsTab.NOTIFICATIONS -> {
                                        NotificationSettingsScreen(
                                            viewModel = notificationViewModel,
                                            showBackButton = false,
                                            onNavigateBack = {}
                                        )
                                    }

                                    SettingsTab.CALENDAR -> {
                                        CalendarSettingsScreen(
                                            viewModel = calendarViewModel,
                                            showBackButton = false,
                                            onNavigateBack = {}
                                        )
                                    }

                                    SettingsTab.LABELS -> {
                                        LabelSettingsScreen(
                                            viewModel = labelViewModel,
                                            showBackButton = false,
                                            onNavigateBack = {}
                                        )
                                    }

                                    SettingsTab.BACKUP -> {
                                        BackupScreen(
                                            viewModel = backupViewModel,
                                            showBackButton = false,
                                            onNavigateBack = {}
                                        )
                                    }

                                    SettingsTab.THEME -> {
                                        ThemeSettingsScreen(
                                            viewModel = themeViewModel,
                                            showBackButton = false,
                                            onNavigateBack = {}
                                        )
                                    }

                                    SettingsTab.SYNC -> {
                                        if (homeViewModel != null) {
                                            SyncSettingsScreen(
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
                                            viewModel = notificationViewModel,
                                            showBackButton = false,
                                            onNavigateBack = {}
                                        )
                                    }

                                    SettingsTab.ABOUT -> {
                                        AboutScreen(
                                            showBackButton = false,
                                            onNavigateBack = {},
                                            onNavigateToPrivacyPolicy = {
                                                activeTab = SettingsTab.PRIVACY_POLICY
                                            }
                                        )
                                    }

                                    SettingsTab.PRIVACY_POLICY -> {
                                        PrivacyPolicyScreen(
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
            )
        }
    }
}

@Composable
private fun SettingsFooter() {
    // Optional footer placeholder (e.g., for version info or copyright).
}

/**
 * A menu item row inside the settings menu.
 *
 * @param titleRes String resource for the label.
 * @param descRes String resource for the description.
 * @param icon The leading icon to display.
 * @param isSelected Whether this item is currently highlighted (used in tablet mode).
 * @param useTabletStyle If true, applies extra padding, background highlight, and rounded corners.
 * @param onClick Action to run on item tap.
 */
@Composable
private fun SettingsMenuItem(
    titleRes: Int,
    descRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean = false,
    useTabletStyle: Boolean = false,
    onClick: () -> Unit
) {
    // Set container color based on selection state and style context (tablet/mobile).
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
        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = AlphaEmphasisMedium)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val iconColor = if (isSelected && useTabletStyle) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Tablet style items are padded and clipped inside the container for a modern card-like look.
    val itemModifier = if (useTabletStyle) {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingMedium, vertical = SpacingTiny)
            .clip(RoundedCornerShape(SpacingNormal))
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
        CompositionLocalProvider(
            LocalWindowSizeClass provides WindowSizeClass(360, 640)
        ) {
            SettingsContent(
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
}

/**
 * Type-safe navigation keys for Jetpack Navigation 3, annotated with @Serializable.
 */
@Serializable
private sealed interface SettingsNavKey : NavKey {
    /**
     * Represents the settings menu pane list.
     */
    @Serializable
    data object SettingsMenu : SettingsNavKey

    /**
     * Represents a specific detail screen shown on the right side of the split-pane.
     */
    @Serializable
    data class SettingsDetail(val tab: SettingsTab) : SettingsNavKey
}
