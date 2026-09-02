package com.heckmannch.birthdaybuddy.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.window.core.layout.WindowSizeClass
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowSizeClass
import com.heckmannch.birthdaybuddy.ui.components.isHeightCompact
import com.heckmannch.birthdaybuddy.ui.components.isWidthCompact
import com.heckmannch.birthdaybuddy.ui.components.isWidthExpanded
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.components.actions.HomeFAB
import com.heckmannch.birthdaybuddy.ui.screens.home.components.labels.LabelSidebar
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.HomeListDetailDisplay
import com.heckmannch.birthdaybuddy.ui.screens.home.components.topbar.HomeTopBar
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SidebarWidthCollapsed
import com.heckmannch.birthdaybuddy.ui.theme.SidebarWidthExpanded
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * Root UI content composable for the home dashboard.
 *
 * Handles adaptive layouts across different screen size classes, rendering either a permanent
 * navigation drawer with a [LabelSidebar] on larger screens or hosting [HomeMainContent] directly.
 *
 * @param uiState Current UI state containing contact list, search query, labels, and sync status.
 * @param homeState State holder managing scroll state, snackbar host, search focus, and UI animations.
 * @param actions Callbacks for handling user actions and events.
 */
@Composable
fun HomeContent(
    uiState: HomeUiState,
    homeState: HomeState,
    actions: HomeActions,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    var isSidebarExpanded by rememberSaveable(windowSizeClass.isWidthExpanded) {
        mutableStateOf(windowSizeClass.isWidthExpanded)
    }
    val showSidebar = !windowSizeClass.isWidthCompact && !windowSizeClass.isHeightCompact
    val showFilterBarInTopBar = !uiState.contacts.isNullOrEmpty() &&
            !windowSizeClass.isWidthCompact &&
            !windowSizeClass.isHeightCompact &&
            !showSidebar

    val mainContent = @Composable {
        HomeMainContent(
            uiState = uiState,
            homeState = homeState,
            actions = actions,
            windowSizeClass = windowSizeClass,
            showSidebar = showSidebar,
            showFilterBarInTopBar = showFilterBarInTopBar,
            onToggleSidebar = { isSidebarExpanded = !isSidebarExpanded }
        )
    }

    if (showSidebar) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(
                    modifier = Modifier.width(if (isSidebarExpanded) SidebarWidthExpanded else SidebarWidthCollapsed),
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    LabelSidebar(
                        labels = uiState.availableLabels,
                        selectedLabel = uiState.selectedLabel,
                        onLabelSelected = actions.onLabelSelected,
                        isExpanded = isSidebarExpanded
                    )
                }
            },
            content = mainContent
        )
    } else {
        mainContent()
    }
}

/**
 * Main UI content layout for [HomeContent].
 *
 * Displays the [HomeTopBar], list/detail navigation display ([HomeListDetailDisplay]),
 * pull-to-refresh container, and [HomeFAB].
 *
 * @param uiState Current UI state containing contact list, search query, and sync info.
 * @param homeState Screen state holder managing scroll state, snackbar host, and focus.
 * @param actions Callbacks for user interactions.
 * @param windowSizeClass Current window size class for responsive layout adjustments.
 * @param showSidebar Whether the navigation sidebar toggle icon is displayed.
 * @param showFilterBarInTopBar Whether the label filter bar should be rendered in the top bar.
 * @param onToggleSidebar Callback invoked when the user clicks the menu icon to toggle the sidebar.
 * @param modifier Optional [Modifier] for the root container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeMainContent(
    uiState: HomeUiState,
    homeState: HomeState,
    actions: HomeActions,
    windowSizeClass: WindowSizeClass,
    showSidebar: Boolean,
    showFilterBarInTopBar: Boolean,
    onToggleSidebar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    AppResponsiveScaffold(
        modifier = modifier,
        windowSizeClass = windowSizeClass,
        useAdaptiveWidth = false,
        snackbarHost = { SnackbarHost(hostState = homeState.snackbarHostState) },
        topBar = {
            HomeTopBar(
                searchQuery = uiState.searchQuery,
                placeholder = homeState.animatedPlaceholder,
                availableLabels = uiState.availableLabels,
                selectedLabel = uiState.selectedLabel,
                showSidebar = showSidebar,
                showFilterBar = showFilterBarInTopBar,
                focusRequester = homeState.searchFocusRequester,
                actions = actions,
                onToggleSidebar = onToggleSidebar,
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !homeState.isFastScrolling,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                HomeFAB(
                    showScrollUp = homeState.showScrollUp,
                    actions = actions,
                    onScrollToTop = {
                        focusManager.clearFocus()
                        homeState.scrollToTop()
                    },
                    modifier = Modifier.padding(SpacingSmall)
                )
            }
        }
    ) { paddingValues ->
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = uiState.isSyncing,
            onRefresh = actions.onRefresh,
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = uiState.isSyncing,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = paddingValues.calculateTopPadding())
                )
            }
        ) {
            HomeListDetailDisplay(
                uiState = uiState,
                homeState = homeState,
                actions = actions,
                showFilterBarInTopBar = showFilterBarInTopBar,
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    BirthdayBuddyTheme {
        CompositionLocalProvider(
            LocalWindowSizeClass provides WindowSizeClass(360, 640)
        ) {
            HomeContent(
                uiState = SampleData.homeUiState,
                homeState = rememberHomeState(),
                actions = SampleData.homeActions,
            )
        }
    }
}
