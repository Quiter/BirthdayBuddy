package com.heckmannch.birthdaybuddy.ui.screens.settings

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.heckmannch.birthdaybuddy.BuildConfig
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.layout.LocalWindowAdaptiveInfo
import com.heckmannch.birthdaybuddy.ui.components.layout.LocalWindowSizeClass
import com.heckmannch.birthdaybuddy.ui.components.layout.PinnedHeaderPage
import com.heckmannch.birthdaybuddy.ui.components.layout.isWidthCompact
import com.heckmannch.birthdaybuddy.ui.components.settings.FoldingVisibility
import com.heckmannch.birthdaybuddy.ui.components.settings.LocalSettingsSearch
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsColumn
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsEmptyState
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsFooter
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsGroup
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsItem
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsPageHeader
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSearchBackHandler
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSection
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSwitchItem
import com.heckmannch.birthdaybuddy.ui.components.settings.rememberSettingsSearch
import com.heckmannch.birthdaybuddy.ui.components.settings.rememberSettingsSearchState
import com.heckmannch.birthdaybuddy.ui.components.settings.settingsPageBackground
import com.heckmannch.birthdaybuddy.ui.components.settings.verticalScrollFade
import com.heckmannch.birthdaybuddy.ui.screens.settings.components.SettingsDetailPane
import com.heckmannch.birthdaybuddy.ui.screens.settings.components.SettingsDetailPlaceholder
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.labels.LabelIntent
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.labels.LabelViewModel
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.notifications.NotificationIntent
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.notifications.NotificationViewModel
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusSmall
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeLarge
import com.heckmannch.birthdaybuddy.ui.theme.SettingsContentMaxWidth
import com.heckmannch.birthdaybuddy.ui.theme.SpacingLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNone
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import kotlinx.serialization.Serializable

/**
 * Represents the available settings sections (tabs/screens) in the app.
 */
@Serializable
enum class SettingsTab {
    NOTIFICATIONS,
    CALENDAR,
    BACKUP,
    THEME,
    SYNC,
    ABOUT,
    PRIVACY_POLICY,
    STORAGE,
    LICENSES;

    /** Localized screen title resource for this settings destination. */
    @get:StringRes
    val titleRes: Int
        get() = when (this) {
            THEME -> R.string.settings_theme_title
            NOTIFICATIONS -> R.string.settings_notifications_title
            CALENDAR -> R.string.settings_calendar_title
            SYNC -> R.string.settings_sync_title
            BACKUP -> R.string.settings_backup_title
            STORAGE -> R.string.storage_management_title
            ABOUT -> R.string.settings_about_title
            PRIVACY_POLICY -> R.string.settings_privacy_title
            LICENSES -> R.string.opensource_licenses
        }

    /** Localized short summary resource for this settings destination. */
    @get:StringRes
    val summaryRes: Int
        get() = when (this) {
            THEME -> R.string.settings_theme_desc
            NOTIFICATIONS -> R.string.settings_notifications_desc
            CALENDAR -> R.string.settings_calendar_desc
            SYNC -> R.string.settings_sync_desc
            BACKUP -> R.string.settings_backup_desc
            STORAGE -> R.string.storage_management_desc
            ABOUT -> R.string.settings_about_help_info_desc
            PRIVACY_POLICY -> R.string.about_privacy_policy
            LICENSES -> R.string.opensource_licenses
        }

    /** Rounded Material icon representing this settings destination. */
    val icon: ImageVector
        get() = when (this) {
            THEME -> Icons.Rounded.Palette
            NOTIFICATIONS -> Icons.Rounded.Notifications
            CALENDAR -> Icons.Rounded.CalendarMonth
            SYNC -> Icons.Rounded.Sync
            BACKUP -> Icons.Rounded.Backup
            STORAGE -> Icons.Rounded.Storage
            ABOUT -> Icons.Rounded.Info
            PRIVACY_POLICY -> Icons.Rounded.Policy
            LICENSES -> Icons.Rounded.Description
        }
}

/**
 * Type-safe navigation keys for Jetpack Navigation 3, annotated with @Serializable.
 */
@Serializable
internal sealed interface SettingsNavKey : NavKey {
    @Serializable
    data object SettingsMenu : SettingsNavKey

    @Serializable
    data class SettingsDetail(val tab: SettingsTab) : SettingsNavKey
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
    onSubscreenChanged: (Boolean) -> Unit = {},
    labelViewModel: LabelViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
) {
    val labelUiState by labelViewModel.uiState.collectAsStateWithLifecycle()
    val notificationUiState by notificationViewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        initialTab = initialTab,
        onNavigateBack = onNavigateBack,
        showBackButton = showBackButton,
        bottomBarHeight = bottomBarHeight,
        onSubscreenChanged = onSubscreenChanged,
        labelsEnabled = labelUiState.labelsEnabled,
        onLabelsEnabledChanged = { labelViewModel.onIntent(LabelIntent.SetLabelsEnabled(it)) },
        otherEventsEnabled = notificationUiState.otherEventsEnabled,
        onOtherEventsEnabledChanged = { notificationViewModel.onIntent(NotificationIntent.SetOtherEventsEnabled(it)) },
    )
}

/**
 * Displays the Morphe-inspired expressive settings experience with Navigation 3 multi-pane
 * support, morphing header search across main and subpages, and segmented card groups.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun SettingsContent(
    initialTab: SettingsTab? = null,
    onNavigateBack: () -> Unit = {},
    showBackButton: Boolean = true,
    bottomBarHeight: Dp = 0.dp,
    onSubscreenChanged: (Boolean) -> Unit = {},
    labelsEnabled: Boolean = true,
    onLabelsEnabledChanged: (Boolean) -> Unit = {},
    otherEventsEnabled: Boolean = false,
    onOtherEventsEnabledChanged: (Boolean) -> Unit = {},
) {
    val windowAdaptiveInfo = LocalWindowAdaptiveInfo.current
    val windowSizeClass = LocalWindowSizeClass.current

    val directive = remember(windowAdaptiveInfo, windowSizeClass) {
        val base = calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = SpacingNone)
        if (windowSizeClass.isWidthCompact) {
            base.copy(maxHorizontalPartitions = 1)
        } else {
            base
        }
    }

    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
    val backStack = if (initialTab != null) {
        rememberNavBackStack(SettingsNavKey.SettingsMenu, SettingsNavKey.SettingsDetail(initialTab))
    } else {
        rememberNavBackStack(SettingsNavKey.SettingsMenu)
    }

    val isSubscreenActive = remember(backStack.size, windowSizeClass.isWidthCompact) {
        windowSizeClass.isWidthCompact && backStack.lastOrNull() is SettingsNavKey.SettingsDetail
    }

    LaunchedEffect(isSubscreenActive) {
        onSubscreenChanged(isSubscreenActive)
    }

    val openTab: (SettingsTab) -> Unit = remember(backStack) {
        { tab ->
            backStack.removeIf { it is SettingsNavKey.SettingsDetail }
            backStack.add(SettingsNavKey.SettingsDetail(tab))
        }
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
            rememberViewModelStoreNavEntryDecorator(),
        ),
        sceneStrategies = listOf(listDetailStrategy),
        modifier = Modifier.fillMaxSize(),
        entryProvider = entryProvider {
            entry<SettingsNavKey.SettingsMenu>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { SettingsDetailPlaceholder() },
                ),
            ) {
                SettingsOverviewPage(
                    showBackButton = showBackButton,
                    bottomBarHeight = bottomBarHeight,
                    labelsEnabled = labelsEnabled,
                    onLabelsEnabledChanged = onLabelsEnabledChanged,
                    otherEventsEnabled = otherEventsEnabled,
                    onOtherEventsEnabledChanged = onOtherEventsEnabledChanged,
                    onOpenTab = openTab,
                    onNavigateBack = onNavigateBack,
                )
            }

            entry<SettingsNavKey.SettingsDetail>(
                metadata = ListDetailSceneStrategy.detailPane(),
            ) { key ->
                SettingsDetailPane(
                    tab = key.tab,
                    windowSizeClass = windowSizeClass,
                    backStack = backStack,
                )
            }
        },
    )
}

@Composable
private fun SettingsOverviewPage(
    showBackButton: Boolean,
    bottomBarHeight: Dp,
    labelsEnabled: Boolean,
    onLabelsEnabledChanged: (Boolean) -> Unit,
    otherEventsEnabled: Boolean,
    onOtherEventsEnabledChanged: (Boolean) -> Unit,
    onOpenTab: (SettingsTab) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val searchState = rememberSettingsSearchState()
    val settingsSearch = rememberSettingsSearch(searchState.query)
    val scrollState = rememberScrollState()

    SettingsSearchBackHandler(searchState)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(settingsPageBackground()),
    ) {
        PinnedHeaderPage(
            scrollState = scrollState,
            header = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    SettingsPageHeader(
                        title = stringResource(R.string.settings_title),
                        showBackButton = showBackButton,
                        onNavigateBack = onNavigateBack,
                        showSearchButton = true,
                        isSearchActive = searchState.visible,
                        searchQuery = searchState.query,
                        searchPlaceholder = stringResource(R.string.settings_search),
                        onSearchQueryChange = { searchState.query = it },
                        onSearchActiveChange = { active ->
                            if (active) searchState.open() else searchState.collapse()
                        },
                        onClearSearch = { searchState.clear() },
                    )
                }
            },
        ) { insets ->
            CompositionLocalProvider(LocalSettingsSearch provides settingsSearch) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScrollFade(fadeTop = { scrollState.value > 0 })
                        .verticalScroll(scrollState)
                        .imePadding(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = SettingsContentMaxWidth)
                            .fillMaxWidth()
                            .padding(
                                start = insets.horizontal,
                                end = insets.horizontal,
                                top = insets.top + SpacingSmall,
                                bottom = SpacingLarge + bottomBarHeight,
                            )
                            .navigationBarsPadding(),
                    ) {
                        SettingsColumn {
                            GeneralSettingsSection(
                                labelsEnabled = labelsEnabled,
                                onLabelsEnabledChanged = onLabelsEnabledChanged,
                                otherEventsEnabled = otherEventsEnabled,
                                onOtherEventsEnabledChanged = onOtherEventsEnabledChanged,
                                onOpenTab = onOpenTab,
                            )

                            DataSettingsSection(onOpenTab = onOpenTab)

                            AboutSettingsSection(onOpenTab = onOpenTab)

                            if (settingsSearch.isActive) {
                                SubpageSearchMatches(onOpenTab = onOpenTab)
                            }

                            FoldingVisibility(
                                visible = settingsSearch.isActive && settingsSearch.shown == 0,
                            ) {
                                SettingsEmptyState(
                                    title = stringResource(R.string.settings_search_no_results),
                                )
                            }

                            SettingsFooter(
                                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                            )
                        }
                        Spacer(modifier = Modifier.height(SpacingSmall))
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralSettingsSection(
    labelsEnabled: Boolean,
    onLabelsEnabledChanged: (Boolean) -> Unit,
    otherEventsEnabled: Boolean,
    onOtherEventsEnabledChanged: (Boolean) -> Unit,
    onOpenTab: (SettingsTab) -> Unit,
) {
    SettingsSection(title = stringResource(R.string.settings_section_general)) {
        SettingsGroup {
            SettingsSubpageLink(tab = SettingsTab.THEME, onOpenTab = onOpenTab)
            SettingsSubpageLink(tab = SettingsTab.NOTIFICATIONS, onOpenTab = onOpenTab)
            SettingsSubpageLink(tab = SettingsTab.CALENDAR, onOpenTab = onOpenTab)
            SettingsSwitchItem(
                title = stringResource(R.string.settings_labels_title),
                subtitle = stringResource(R.string.settings_labels_desc),
                icon = Icons.AutoMirrored.Rounded.Label,
                checked = labelsEnabled,
                onCheckedChange = onLabelsEnabledChanged,
                searchKeywords = listOf(
                    stringResource(R.string.labels_enable),
                    stringResource(R.string.labels_enable_desc),
                ),
            )
            SettingsSwitchItem(
                title = stringResource(R.string.settings_other_events_title),
                subtitle = stringResource(R.string.settings_other_events_desc),
                icon = Icons.Rounded.Star,
                checked = otherEventsEnabled,
                onCheckedChange = onOtherEventsEnabledChanged,
                searchKeywords = listOf(
                    stringResource(R.string.other_events_enable),
                    stringResource(R.string.other_events_desc),
                ),
            )
        }
    }
}

@Composable
private fun DataSettingsSection(onOpenTab: (SettingsTab) -> Unit) {
    SettingsSection(title = stringResource(R.string.settings_section_data)) {
        SettingsGroup {
            SettingsSubpageLink(tab = SettingsTab.SYNC, onOpenTab = onOpenTab)
            SettingsSubpageLink(tab = SettingsTab.BACKUP, onOpenTab = onOpenTab)
            SettingsSubpageLink(tab = SettingsTab.STORAGE, onOpenTab = onOpenTab)
        }
    }
}

@Composable
private fun AboutSettingsSection(onOpenTab: (SettingsTab) -> Unit) {
    SettingsSection(title = stringResource(R.string.settings_section_about)) {
        SettingsGroup {
            SettingsItem(
                onClick = { onOpenTab(SettingsTab.ABOUT) },
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                leadingContent = {
                    Image(
                        painter = painterResource(R.drawable.ic_app_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(IconSizeLarge)
                            .clip(RoundedCornerShape(CornerRadiusSmall)),
                    )
                },
            )
            SettingsSubpageLink(tab = SettingsTab.ABOUT, onOpenTab = onOpenTab)
        }
    }
}

@Composable
private fun SettingsSubpageLink(
    tab: SettingsTab,
    onOpenTab: (SettingsTab) -> Unit,
) {
    SettingsItem(
        onClick = { onOpenTab(tab) },
        title = stringResource(tab.titleRes),
        subtitle = stringResource(tab.summaryRes),
        icon = tab.icon,
    )
}

/**
 * Full-depth subpage search index rendered exclusively while a search query is active.
 * Each subpage section folds out automatically when none of its items match, and clicking
 * either a section breadcrumb or any item opens the corresponding subpage.
 */
@Composable
private fun SubpageSearchMatches(onOpenTab: (SettingsTab) -> Unit) {
    // Theme subpage items
    SettingsSection(
        title = stringResource(SettingsTab.THEME.titleRes),
        icon = SettingsTab.THEME.icon,
        onTitleClick = { onOpenTab(SettingsTab.THEME) },
    ) {
        SettingsGroup {
            SettingsItem(
                onClick = { onOpenTab(SettingsTab.THEME) },
                title = stringResource(R.string.settings_theme_mode_header),
                subtitle = stringResource(R.string.settings_theme_mode_system_desc),
                icon = Icons.Rounded.DarkMode,
                searchKeywords = listOf(
                    stringResource(R.string.settings_theme_mode_system),
                    stringResource(R.string.settings_theme_mode_light),
                    stringResource(R.string.settings_theme_mode_dark),
                ),
            )
            SettingsItem(
                onClick = { onOpenTab(SettingsTab.THEME) },
                title = stringResource(R.string.settings_theme_amoled),
                subtitle = stringResource(R.string.settings_theme_amoled_desc),
                icon = Icons.Rounded.DarkMode,
            )
            SettingsItem(
                onClick = { onOpenTab(SettingsTab.THEME) },
                title = stringResource(R.string.settings_theme_accent_header),
                subtitle = stringResource(R.string.theme_accent_custom),
                icon = Icons.Rounded.ColorLens,
            )
        }
    }

    // Notifications subpage items
    SettingsSection(
        title = stringResource(SettingsTab.NOTIFICATIONS.titleRes),
        icon = SettingsTab.NOTIFICATIONS.icon,
        onTitleClick = { onOpenTab(SettingsTab.NOTIFICATIONS) },
    ) {
        SettingsGroup {
            SettingsItem(
                onClick = { onOpenTab(SettingsTab.NOTIFICATIONS) },
                title = stringResource(R.string.notifications_persistent_header),
                subtitle = stringResource(R.string.notifications_persistent_desc),
                icon = Icons.Rounded.NotificationsActive,
            )
            SettingsItem(
                onClick = { onOpenTab(SettingsTab.NOTIFICATIONS) },
                title = stringResource(R.string.notifications_planned_header),
                subtitle = stringResource(R.string.notifications_add_rule),
                icon = Icons.Rounded.Schedule,
            )
        }
    }

    // Calendar subpage items
    SettingsSection(
        title = stringResource(SettingsTab.CALENDAR.titleRes),
        icon = SettingsTab.CALENDAR.icon,
        onTitleClick = { onOpenTab(SettingsTab.CALENDAR) },
    ) {
        SettingsGroup {
            SettingsItem(
                onClick = { onOpenTab(SettingsTab.CALENDAR) },
                title = stringResource(R.string.calendar_settings_enable),
                subtitle = stringResource(R.string.calendar_settings_desc),
                icon = Icons.Rounded.CalendarMonth,
            )
            SettingsItem(
                onClick = { onOpenTab(SettingsTab.CALENDAR) },
                title = stringResource(R.string.calendar_colors_section_title),
                subtitle = stringResource(R.string.calendar_color_birthdays),
                icon = Icons.Rounded.ColorLens,
                searchKeywords = listOf(
                    stringResource(R.string.calendar_color_anniversaries),
                    stringResource(R.string.calendar_color_namedays),
                ),
            )
        }
    }

    // Sync subpage items
    SettingsSection(
        title = stringResource(SettingsTab.SYNC.titleRes),
        icon = SettingsTab.SYNC.icon,
        onTitleClick = { onOpenTab(SettingsTab.SYNC) },
    ) {
        SettingsGroup {
            SettingsItem(
                onClick = { onOpenTab(SettingsTab.SYNC) },
                title = stringResource(R.string.sync_button_title),
                subtitle = stringResource(R.string.sync_button_desc),
                icon = Icons.Rounded.Sync,
            )
        }
    }

    // Backup subpage items
    SettingsSection(
        title = stringResource(SettingsTab.BACKUP.titleRes),
        icon = SettingsTab.BACKUP.icon,
        onTitleClick = { onOpenTab(SettingsTab.BACKUP) },
    ) {
        SettingsGroup {
            SettingsItem(
                onClick = { onOpenTab(SettingsTab.BACKUP) },
                title = stringResource(R.string.backup_export_title),
                subtitle = stringResource(R.string.backup_export_desc),
                icon = Icons.Rounded.Upload,
            )
            SettingsItem(
                onClick = { onOpenTab(SettingsTab.BACKUP) },
                title = stringResource(R.string.backup_import_title),
                subtitle = stringResource(R.string.backup_import_desc),
                icon = Icons.Rounded.Download,
            )
        }
    }

    // Storage subpage items
    SettingsSection(
        title = stringResource(SettingsTab.STORAGE.titleRes),
        icon = SettingsTab.STORAGE.icon,
        onTitleClick = { onOpenTab(SettingsTab.STORAGE) },
    ) {
        SettingsGroup {
            SettingsItem(
                onClick = { onOpenTab(SettingsTab.STORAGE) },
                title = stringResource(R.string.storage_clear_cache),
                subtitle = stringResource(R.string.storage_clear_cache_desc),
                icon = Icons.Rounded.DeleteOutline,
            )
            SettingsItem(
                onClick = { onOpenTab(SettingsTab.STORAGE) },
                title = stringResource(R.string.storage_app_info),
                subtitle = stringResource(R.string.storage_app_info_desc),
                icon = Icons.Rounded.Info,
            )
        }
    }

    // About subpage items
    SettingsSection(
        title = stringResource(SettingsTab.ABOUT.titleRes),
        icon = SettingsTab.ABOUT.icon,
        onTitleClick = { onOpenTab(SettingsTab.ABOUT) },
    ) {
        SettingsGroup {
            SettingsItem(
                onClick = { onOpenTab(SettingsTab.PRIVACY_POLICY) },
                title = stringResource(R.string.about_privacy_policy),
                icon = Icons.Rounded.Policy,
            )
            SettingsItem(
                onClick = { onOpenTab(SettingsTab.LICENSES) },
                title = stringResource(R.string.opensource_licenses),
                icon = Icons.Rounded.Description,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    BirthdayBuddyTheme {
        CompositionLocalProvider(
            LocalWindowSizeClass provides WindowSizeClass(360, 640),
        ) {
            SettingsContent(
                onNavigateBack = {},
            )
        }
    }
}
