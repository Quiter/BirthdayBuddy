package com.heckmannch.birthdaybuddy.ui.screens.settings

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
import com.heckmannch.birthdaybuddy.ui.components.AppTopHeader
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowAdaptiveInfo
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowSizeClass
import com.heckmannch.birthdaybuddy.ui.components.SettingsColumn
import com.heckmannch.birthdaybuddy.ui.components.SettingsGroup
import com.heckmannch.birthdaybuddy.ui.components.SettingsItem
import com.heckmannch.birthdaybuddy.ui.components.SettingsSection
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
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNone
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import kotlinx.serialization.Serializable

/**
 * Represents the available settings sections (tabs/screens) in the app.
 */
@Serializable
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
 */
private data class SettingsMenuItemData(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val tab: SettingsTab,
    val section: SettingsSectionType
)

private enum class SettingsSectionType {
    GENERAL,
    DATA,
    ABOUT
}

/**
 * The main settings screen entry point.
 */
@Composable
fun SettingsScreen(
    initialTab: SettingsTab? = null,
    onNavigateBack: () -> Unit = {},
    showBackButton: Boolean = true,
    bottomBarHeight: Dp = 0.dp,
) {
    SettingsContent(
        initialTab = initialTab,
        onNavigateBack = onNavigateBack,
        showBackButton = showBackButton,
        bottomBarHeight = bottomBarHeight,
    )
}

/**
 * Displays the actual settings content with Morphe-inspired SettingsColumn, SettingsSection,
 * SettingsGroup, and SettingsItem, with glassmorphic CardBorder outlines and correct header/bottom padding.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun SettingsContent(
    initialTab: SettingsTab? = null,
    onNavigateBack: () -> Unit = {},
    showBackButton: Boolean = true,
    bottomBarHeight: Dp = 0.dp,
) {
    val windowAdaptiveInfo = LocalWindowAdaptiveInfo.current
    val windowSizeClass = LocalWindowSizeClass.current

    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = SpacingNone)
    }

    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
    val backStack = if (initialTab != null) {
        rememberNavBackStack(SettingsNavKey.SettingsMenu, SettingsNavKey.SettingsDetail(initialTab))
    } else {
        rememberNavBackStack(SettingsNavKey.SettingsMenu)
    }

    val menuItems = remember {
        listOf(
            SettingsMenuItemData(
                titleRes = R.string.settings_theme_title,
                descRes = R.string.settings_theme_desc,
                icon = Icons.Default.Palette,
                tab = SettingsTab.THEME,
                section = SettingsSectionType.GENERAL
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_notifications_title,
                descRes = R.string.settings_notifications_desc,
                icon = Icons.Default.Notifications,
                tab = SettingsTab.NOTIFICATIONS,
                section = SettingsSectionType.GENERAL
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_calendar_title,
                descRes = R.string.settings_calendar_desc,
                icon = Icons.Default.DateRange,
                tab = SettingsTab.CALENDAR,
                section = SettingsSectionType.GENERAL
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_labels_title,
                descRes = R.string.settings_labels_desc,
                icon = Icons.AutoMirrored.Filled.Label,
                tab = SettingsTab.LABELS,
                section = SettingsSectionType.GENERAL
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_other_events_title,
                descRes = R.string.settings_other_events_desc,
                icon = Icons.Default.Star,
                tab = SettingsTab.OTHER_EVENTS,
                section = SettingsSectionType.GENERAL
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_sync_title,
                descRes = R.string.settings_sync_desc,
                icon = Icons.Default.Refresh,
                tab = SettingsTab.SYNC,
                section = SettingsSectionType.DATA
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_backup_title,
                descRes = R.string.settings_backup_desc,
                icon = Icons.Default.Share,
                tab = SettingsTab.BACKUP,
                section = SettingsSectionType.DATA
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_about_title,
                descRes = R.string.settings_about_desc,
                icon = Icons.Default.Info,
                tab = SettingsTab.ABOUT,
                section = SettingsSectionType.ABOUT
            )
        )
    }

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
        modifier = Modifier.fillMaxSize(),
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
                var isSearchActive by rememberSaveable { mutableStateOf(false) }
                var searchQuery by rememberSaveable { mutableStateOf("") }
                val context = LocalContext.current

                val filteredMenuItems = remember(menuItems, searchQuery) {
                    if (searchQuery.isBlank()) menuItems
                    else menuItems.filter {
                        context.getString(it.titleRes).contains(searchQuery, ignoreCase = true) ||
                        context.getString(it.descRes).contains(searchQuery, ignoreCase = true)
                    }
                }

                AppResponsiveScaffold(
                    useAdaptiveWidth = false,
                    topBar = {
                        AppTopHeader(
                            title = stringResource(R.string.settings_title),
                            isSearchActive = isSearchActive,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            onSearchActiveChange = { isSearchActive = it },
                            searchPlaceholder = stringResource(R.string.settings_search),
                            onClearSearch = { searchQuery = "" },
                            navigationIcon = if (showBackButton) {
                                {
                                    IconButton(
                                        onClick = onNavigateBack,
                                        modifier = Modifier.testTag("settings_back_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.notifications_back),
                                        )
                                    }
                                }
                            } else null
                        )
                    }
                ) { paddingValues ->
                    val layoutDirection = LocalLayoutDirection.current
                    val combinedPadding = PaddingValues(
                        start = paddingValues.calculateStartPadding(layoutDirection),
                        top = paddingValues.calculateTopPadding(),
                        end = paddingValues.calculateEndPadding(layoutDirection),
                        bottom = paddingValues.calculateBottomPadding() + bottomBarHeight,
                    )

                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        SettingsColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(combinedPadding)
                        ) {
                            val generalItems = filteredMenuItems.filter { it.section == SettingsSectionType.GENERAL }
                            if (generalItems.isNotEmpty()) {
                                SettingsSection(
                                    title = stringResource(R.string.settings_theme_title),
                                    icon = Icons.Default.Palette
                                ) {
                                    SettingsGroup {
                                        generalItems.forEach { item ->
                                            SettingsItem(
                                                onClick = {
                                                    backStack.removeIf { it is SettingsNavKey.SettingsDetail }
                                                    backStack.add(SettingsNavKey.SettingsDetail(item.tab))
                                                },
                                                title = stringResource(item.titleRes),
                                                subtitle = stringResource(item.descRes),
                                                icon = item.icon
                                            )
                                        }
                                    }
                                }
                            }

                            val dataItems = filteredMenuItems.filter { it.section == SettingsSectionType.DATA }
                            if (dataItems.isNotEmpty()) {
                                SettingsSection(
                                    title = stringResource(R.string.settings_sync_title),
                                    icon = Icons.Default.Refresh
                                ) {
                                    SettingsGroup {
                                        dataItems.forEach { item ->
                                            SettingsItem(
                                                onClick = {
                                                    backStack.removeIf { it is SettingsNavKey.SettingsDetail }
                                                    backStack.add(SettingsNavKey.SettingsDetail(item.tab))
                                                },
                                                title = stringResource(item.titleRes),
                                                subtitle = stringResource(item.descRes),
                                                icon = item.icon
                                            )
                                        }
                                    }
                                }
                            }

                            val aboutItems = filteredMenuItems.filter { it.section == SettingsSectionType.ABOUT }
                            if (aboutItems.isNotEmpty()) {
                                SettingsSection(
                                    title = stringResource(R.string.settings_about_title),
                                    icon = Icons.Default.Info
                                ) {
                                    SettingsGroup {
                                        aboutItems.forEach { item ->
                                            SettingsItem(
                                                onClick = {
                                                    backStack.removeIf { it is SettingsNavKey.SettingsDetail }
                                                    backStack.add(SettingsNavKey.SettingsDetail(item.tab))
                                                },
                                                title = stringResource(item.titleRes),
                                                subtitle = stringResource(item.descRes),
                                                icon = item.icon
                                            )
                                        }
                                    }
                                }
                            }
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
    @Serializable
    data object SettingsMenu : SettingsNavKey

    @Serializable
    data class SettingsDetail(val tab: SettingsTab) : SettingsNavKey
}
