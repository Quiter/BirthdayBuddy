package com.heckmannch.birthdaybuddy.ui.screens.settings

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.layout.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.components.layout.LocalWindowAdaptiveInfo
import com.heckmannch.birthdaybuddy.ui.components.layout.LocalWindowSizeClass
import com.heckmannch.birthdaybuddy.ui.components.layout.isWidthCompact
import com.heckmannch.birthdaybuddy.ui.screens.settings.components.SettingsBody
import com.heckmannch.birthdaybuddy.ui.screens.settings.components.SettingsDetailPane
import com.heckmannch.birthdaybuddy.ui.screens.settings.components.SettingsDetailPlaceholder
import com.heckmannch.birthdaybuddy.ui.screens.settings.components.SettingsFooter
import com.heckmannch.birthdaybuddy.ui.screens.settings.components.SettingsHeader
import com.heckmannch.birthdaybuddy.ui.screens.settings.components.SettingsMenuList
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.labels.LabelIntent
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.labels.LabelViewModel
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.notifications.NotificationIntent
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.notifications.NotificationViewModel
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNone
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
    LICENSES
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
        onOtherEventsEnabledChanged = { notificationViewModel.onIntent(NotificationIntent.SetOtherEventsEnabled(it)) }
    )
}

/**
 * Displays the actual settings content with Navigation 3 multi-pane support,
 * header, menu sections, cards, and detail pane.
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

    val isSubscreenActive = remember(backStack.size, windowSizeClass.isWidthCompact) {
        windowSizeClass.isWidthCompact && backStack.lastOrNull() is SettingsNavKey.SettingsDetail
    }

    LaunchedEffect(isSubscreenActive) {
        onSubscreenChanged(isSubscreenActive)
    }

    val menuItems = rememberSettingsMenuItems()

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
                    detailPlaceholder = { SettingsDetailPlaceholder() }
                )
            ) {
                var isSearchActive by rememberSaveable { mutableStateOf(false) }
                var searchQuery by rememberSaveable { mutableStateOf("") }
                // Using LocalContext.current is acceptable for non-config-aware lookups,
                // but we've replaced the resource lookup logic inside the filter for the fix.
                val context = LocalContext.current

                val filteredMenuItems = remember(menuItems, searchQuery) {
                    if (searchQuery.isBlank()) menuItems
                    else menuItems.filter {
                        (context.resources.getString(it.titleRes)).contains(searchQuery, ignoreCase = true) ||
                        (context.resources.getString(it.descRes)).contains(searchQuery, ignoreCase = true)
                    }
                }

                AppResponsiveScaffold(
                    useAdaptiveWidth = false,
                    topBar = {
                        SettingsHeader(
                            title = stringResource(R.string.settings_title),
                            isSearchActive = isSearchActive,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            onSearchActiveChange = { isSearchActive = it },
                            onClearSearch = { searchQuery = "" },
                            showBackButton = showBackButton,
                            onNavigateBack = onNavigateBack,
                            showSearchButton = true
                        )
                    }
                ) { paddingValues ->
                    SettingsBody(
                        paddingValues = paddingValues,
                        bottomBarHeight = bottomBarHeight,
                        footer = {
                            SettingsFooter(
                                text = stringResource(R.string.settings_version, "2.1.0"),
                                bottomBarHeight = bottomBarHeight
                            )
                        }
                    ) {
                        SettingsMenuList(
                            menuItems = filteredMenuItems,
                            onItemClick = { item ->
                                backStack.removeIf { it is SettingsNavKey.SettingsDetail }
                                backStack.add(SettingsNavKey.SettingsDetail(item.tab))
                            },
                            labelsEnabled = labelsEnabled,
                            onLabelsEnabledChanged = onLabelsEnabledChanged,
                            otherEventsEnabled = otherEventsEnabled,
                            onOtherEventsEnabledChanged = onOtherEventsEnabledChanged
                        )
                    }
                }
            }

            entry<SettingsNavKey.SettingsDetail>(
                metadata = ListDetailSceneStrategy.detailPane()
            ) { key ->
                SettingsDetailPane(
                    tab = key.tab,
                    windowSizeClass = windowSizeClass,
                    backStack = backStack
                )
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