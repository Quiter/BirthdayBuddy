package com.heckmannch.birthdaybuddy.ui.screens.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowSizeClass
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.components.header.HomeHeader
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.HomeListDetailDisplay
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Root UI content composable for the home dashboard.
 *
 * @param uiState Current UI state containing contact list, search query, labels, and sync status.
 * @param homeState State holder managing scroll state, snackbar host, search focus, and UI animations.
 * @param actions Callbacks for handling user actions and events.
 * @param events Flow of one-shot [HomeUiEvent] instances consumed via [LaunchedEffect].
 * @param onIntent Callback for dispatching MVI intents (e.g. consuming one-shot events).
 * @param bottomBarHeight Padding reserved for the floating pill bar at the bottom.
 */
@Composable
fun HomeContent(
    uiState: HomeUiState,
    homeState: HomeState,
    actions: HomeActions,
    events: Flow<HomeUiEvent> = emptyFlow(),
    onIntent: (HomeIntent) -> Unit = {},
    bottomBarHeight: Dp = 0.dp,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is HomeUiEvent.RequestSearchFocus -> {
                    runCatching {
                        homeState.searchFocusRequester.requestFocus()
                    }
                    keyboardController?.show()
                    onIntent(HomeIntent.ConsumeSearchFocus)
                }
                is HomeUiEvent.FocusNewlyAddedIdea -> {
                    homeState.newlyAddedIdeaId = event.ideaId
                    onIntent(HomeIntent.ConsumeNewlyAddedIdeaId)
                }
                is HomeUiEvent.ScrollToTop -> {
                    homeState.scrollToTop()
                }
            }
        }
    }
    val windowSizeClass = LocalWindowSizeClass.current

    HomeMainContent(
        uiState = uiState,
        homeState = homeState,
        actions = actions,
        windowSizeClass = windowSizeClass,
        bottomBarHeight = bottomBarHeight,
    )
}

/**
 * Main UI content layout for [HomeContent].
 *
 * Displays the [HomeHeader], list/detail navigation display ([HomeListDetailDisplay]),
 * and pull-to-refresh container.
 *
 * @param uiState Current UI state containing contact list, search query, and sync info.
 * @param homeState Screen state holder managing scroll state, snackbar host, and focus.
 * @param actions Callbacks for user interactions.
 * @param windowSizeClass Current window size class for responsive layout adjustments.
 * @param modifier Optional [Modifier] for the root container.
 */
@Composable
private fun HomeMainContent(
    uiState: HomeUiState,
    homeState: HomeState,
    actions: HomeActions,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    bottomBarHeight: Dp = 0.dp,
) {
    AppResponsiveScaffold(
        modifier = modifier,
        windowSizeClass = windowSizeClass,
        useAdaptiveWidth = false,
        snackbarHost = { SnackbarHost(hostState = homeState.snackbarHostState) },
        topBar = {
            HomeHeader(
                searchQuery = uiState.searchQuery,
                isSearchActive = uiState.isSearchActive,
                placeholder = homeState.animatedPlaceholder,
                showSidebar = false,
                focusRequester = homeState.searchFocusRequester,
                actions = actions,
                onToggleSearch = actions.onToggleSearch,
            )
        }
    ) { paddingValues ->
        val pullToRefreshState = rememberPullToRefreshState()
        val combinedPadding = PaddingValues(
            start = paddingValues.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
            top = paddingValues.calculateTopPadding(),
            end = paddingValues.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
            bottom = paddingValues.calculateBottomPadding() + bottomBarHeight,
        )
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
                contentPadding = combinedPadding,
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
