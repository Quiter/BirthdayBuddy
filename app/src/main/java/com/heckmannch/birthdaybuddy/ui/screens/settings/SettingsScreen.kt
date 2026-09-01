package com.heckmannch.birthdaybuddy.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowAdaptiveInfo
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowSizeClass
import com.heckmannch.birthdaybuddy.ui.components.isWidthCompact
import com.heckmannch.birthdaybuddy.ui.screens.settings.about.AboutScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.about.PrivacyPolicyScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.backup.BackupScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.calendar.CalendarSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.labels.LabelSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.NotificationSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.otherevents.OtherEventsSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.sync.SyncSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.theme.ThemeSettingsScreen
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisMedium
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingLarge
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
 */
private data class SettingsMenuItemData(
    val titleRes: Int,
    val descRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tab: SettingsTab
)

/**
 * The main settings screen entry point.
 * It forwards the navigation callbacks and parameters to [SettingsContent].
 */
@Composable
fun SettingsScreen(
    initialTab: SettingsTab? = null,
    onNavigateBack: () -> Unit,
) {
    SettingsContent(
        initialTab = initialTab,
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
    initialTab: SettingsTab? = null,
    onNavigateBack: () -> Unit,
) {
    val windowAdaptiveInfo = LocalWindowAdaptiveInfo.current
    val windowSizeClass = LocalWindowSizeClass.current
    
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }

    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
    val backStack = if (initialTab != null) {
        rememberNavBackStack(SettingsNavKey.SettingsMenu, SettingsNavKey.SettingsDetail(initialTab))
    } else {
        rememberNavBackStack(SettingsNavKey.SettingsMenu)
    }

    val activeTab = (backStack.lastOrNull() as? SettingsNavKey.SettingsDetail)?.tab

    val menuItems = remember {
        listOf(
            SettingsMenuItemData(
                titleRes = R.string.settings_notifications_title,
                descRes = R.string.settings_notifications_desc,
                icon = Icons.Default.Notifications,
                tab = SettingsTab.NOTIFICATIONS
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_calendar_title,
                descRes = R.string.settings_calendar_desc,
                icon = Icons.Default.DateRange,
                tab = SettingsTab.CALENDAR
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_labels_title,
                descRes = R.string.settings_labels_desc,
                icon = Icons.AutoMirrored.Filled.Label,
                tab = SettingsTab.LABELS
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_backup_title,
                descRes = R.string.settings_backup_desc,
                icon = Icons.Default.Share,
                tab = SettingsTab.BACKUP
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_theme_title,
                descRes = R.string.settings_theme_desc,
                icon = Icons.Default.Palette,
                tab = SettingsTab.THEME
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_sync_title,
                descRes = R.string.settings_sync_desc,
                icon = Icons.Default.Refresh,
                tab = SettingsTab.SYNC
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_other_events_title,
                descRes = R.string.settings_other_events_desc,
                icon = Icons.Default.Star,
                tab = SettingsTab.OTHER_EVENTS
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_about_title,
                descRes = R.string.settings_about_desc,
                icon = Icons.Default.Info,
                tab = SettingsTab.ABOUT
            )
        )
    }

    AppResponsiveScaffold(
        useAdaptiveWidth = false,
        topBar = {}
    ) { paddingValues ->
        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeLastOrNull()
                } else {
                    onNavigateBack()
                }
            },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            sceneStrategies = listOf(listDetailStrategy),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            entryProvider = entryProvider {
                entry<SettingsNavKey.SettingsMenu>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(SpacingLarge),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(IconSizeExtraLarge),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(SpacingNormal))
                                Text(
                                    text = stringResource(R.string.settings_select_item_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    )
                ) {
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
                                val isSelected = activeTab == item.tab || (activeTab == SettingsTab.PRIVACY_POLICY && item.tab == SettingsTab.ABOUT)
                                SettingsMenuItem(
                                    titleRes = item.titleRes,
                                    descRes = item.descRes,
                                    icon = item.icon,
                                    isSelected = isSelected,
                                    useTabletStyle = !windowSizeClass.isWidthCompact,
                                    onClick = {
                                        backStack.removeIf { it is SettingsNavKey.SettingsDetail }
                                        backStack.add(SettingsNavKey.SettingsDetail(item.tab))
                                    }
                                )
                            }
                        }
                    }
                }

                entry<SettingsNavKey.SettingsDetail>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) { key ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (key.tab) {
                            SettingsTab.NOTIFICATIONS -> {
                                NotificationSettingsScreen(
                                    viewModel = hiltViewModel(),
                                    showBackButton = windowSizeClass.isWidthCompact,
                                    onNavigateBack = { backStack.removeLastOrNull() }
                                )
                            }
                            SettingsTab.CALENDAR -> {
                                CalendarSettingsScreen(
                                    viewModel = hiltViewModel(),
                                    showBackButton = windowSizeClass.isWidthCompact,
                                    onNavigateBack = { backStack.removeLastOrNull() }
                                )
                            }
                            SettingsTab.LABELS -> {
                                LabelSettingsScreen(
                                    viewModel = hiltViewModel(),
                                    showBackButton = windowSizeClass.isWidthCompact,
                                    onNavigateBack = { backStack.removeLastOrNull() }
                                )
                            }
                            SettingsTab.BACKUP -> {
                                BackupScreen(
                                    viewModel = hiltViewModel(),
                                    showBackButton = windowSizeClass.isWidthCompact,
                                    onNavigateBack = { backStack.removeLastOrNull() }
                                )
                            }
                            SettingsTab.THEME -> {
                                ThemeSettingsScreen(
                                    viewModel = hiltViewModel(),
                                    showBackButton = windowSizeClass.isWidthCompact,
                                    onNavigateBack = { backStack.removeLastOrNull() }
                                )
                            }
                            SettingsTab.SYNC -> {
                                SyncSettingsScreen(
                                    viewModel = hiltViewModel(),
                                    showBackButton = windowSizeClass.isWidthCompact,
                                    onNavigateBack = { backStack.removeLastOrNull() }
                                )
                            }
                            SettingsTab.OTHER_EVENTS -> {
                                OtherEventsSettingsScreen(
                                    viewModel = hiltViewModel(),
                                    showBackButton = windowSizeClass.isWidthCompact,
                                    onNavigateBack = { backStack.removeLastOrNull() }
                                )
                            }
                            SettingsTab.ABOUT -> {
                                AboutScreen(
                                    showBackButton = windowSizeClass.isWidthCompact,
                                    onNavigateBack = { backStack.removeLastOrNull() },
                                    onNavigateToPrivacyPolicy = {
                                        backStack.add(SettingsNavKey.SettingsDetail(SettingsTab.PRIVACY_POLICY))
                                    }
                                )
                            }
                            SettingsTab.PRIVACY_POLICY -> {
                                PrivacyPolicyScreen(
                                    showBackButton = true,
                                    onNavigateBack = { backStack.removeLastOrNull() }
                                )
                            }
                        }
                    }
                }
            }
        )
    }
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

    ListItem(
        modifier = itemModifier,
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

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    BirthdayBuddyTheme {
        CompositionLocalProvider(
            LocalWindowSizeClass provides WindowSizeClass(360, 640)
        ) {
            SettingsContent(
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

