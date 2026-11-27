package com.heckmannch.birthdaybuddy.ui.screens.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.heckmannch.birthdaybuddy.ui.components.layout.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.components.layout.LocalWindowSizeClass
import com.heckmannch.birthdaybuddy.ui.components.layout.isWidthCompact
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
 * @param onDetailOpenChanged Callback invoked when a compact full-screen contact detail pane opens or closes.
 */
@Composable
fun HomeContent(
    uiState: HomeUiState,
    homeState: HomeState,
    actions: HomeActions,
    events: Flow<HomeUiEvent> = emptyFlow(),
    onIntent: (HomeIntent) -> Unit = {},
    bottomBarHeight: Dp = 0.dp,
    onDetailOpenChanged: (Boolean) -> Unit = {},
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
        onDetailOpenChanged = onDetailOpenChanged,
    )
}

/**
 * Main UI content layout for [HomeContent].
 *
 * On wide screens, [HomeHeader] spans above both list and detail panes. On compact screens,
 * [HomeHeader] is hosted inside the list pane of [HomeListDetailDisplay] so it crossfades away
 * cleanly together with the contact list whenever a contact detail page opens.
 *
 * @param uiState Current UI state containing contact list, search query, and sync info.
 * @param homeState Screen state holder managing scroll state, snackbar host, and focus.
 * @param actions Callbacks for user interactions.
 * @param windowSizeClass Current window size class for responsive layout adjustments.
 * @param modifier Optional [Modifier] for the root container.
 * @param bottomBarHeight Padding reserved for the floating pill bar at the bottom.
 * @param onDetailOpenChanged Callback invoked when a compact full-screen contact detail pane opens or closes.
 */
@Composable
private fun HomeMainContent(
    uiState: HomeUiState,
    homeState: HomeState,
    actions: HomeActions,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    bottomBarHeight: Dp = 0.dp,
    onDetailOpenChanged: (Boolean) -> Unit = {},
) {
    val isCompact = windowSizeClass.isWidthCompact
    val isCompactDetailOpen = isCompact && (homeState.selectedContactId != null)
    val layoutDirection = LocalLayoutDirection.current

    LaunchedEffect(isCompactDetailOpen) {
        onDetailOpenChanged(isCompactDetailOpen)
    }
    DisposableEffect(Unit) {
        onDispose { onDetailOpenChanged(false) }
    }

    val homeHeader: @Composable () -> Unit = {
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

    AppResponsiveScaffold(
        modifier = modifier,
        windowSizeClass = windowSizeClass,
        useAdaptiveWidth = false,
        snackbarHost = { SnackbarHost(hostState = homeState.snackbarHostState) },
        topBar = {
            if (!isCompact) {
                homeHeader()
            }
        },
    ) { paddingValues ->
        val combinedPadding = PaddingValues(
            start = paddingValues.calculateStartPadding(layoutDirection),
            top = paddingValues.calculateTopPadding(),
            end = paddingValues.calculateEndPadding(layoutDirection),
            bottom = paddingValues.calculateBottomPadding() + bottomBarHeight,
        )
        HomeListDetailDisplay(
            uiState = uiState,
            homeState = homeState,
            actions = actions,
            contentPadding = combinedPadding,
            compactListTopBar = if (isCompact) homeHeader else null,
            isRefreshing = uiState.isSyncing,
            onRefresh = actions.onRefresh,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    BirthdayBuddyTheme {
        CompositionLocalProvider(
            LocalWindowSizeClass provides WindowSizeClass(360, 640),
        ) {
            HomeContent(
                uiState = SampleData.homeUiState,
                homeState = rememberHomeState(),
                actions = SampleData.homeActions,
            )
        }
    }
}
