package com.heckmannch.birthdaybuddy.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowAdaptiveInfo
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowSizeClass
import com.heckmannch.birthdaybuddy.ui.components.isHeightCompact
import com.heckmannch.birthdaybuddy.ui.components.isWidthCompact
import com.heckmannch.birthdaybuddy.ui.components.isWidthExpanded
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.components.actions.HomeFAB
import com.heckmannch.birthdaybuddy.ui.screens.home.components.labels.LabelFilterBar
import com.heckmannch.birthdaybuddy.ui.screens.home.components.labels.LabelSidebar
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.BirthdayDetailPane
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.BirthdayList
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.BirthdayQuotePlaceholder
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.FastScrollbar
import com.heckmannch.birthdaybuddy.ui.screens.home.components.topbar.SearchBar
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SidebarWidthCollapsed
import com.heckmannch.birthdaybuddy.ui.theme.SidebarWidthExpanded
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import kotlinx.serialization.Serializable

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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    homeState: HomeState,
    actions: HomeActions,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    var isSidebarExpanded by rememberSaveable(windowSizeClass.isWidthExpanded) {
        mutableStateOf(
            windowSizeClass.isWidthExpanded
        )
    }
    val showSidebar = !windowSizeClass.isWidthCompact && !windowSizeClass.isHeightCompact
    val showFilterBarInTopBar = !uiState.contacts.isNullOrEmpty() &&
            !windowSizeClass.isWidthCompact &&
            !windowSizeClass.isHeightCompact &&
            !showSidebar

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
            }
        ) {
            HomeMainContent(
                uiState = uiState,
                homeState = homeState,
                actions = actions,
                windowSizeClass = windowSizeClass,
                showSidebar = showSidebar,
                showFilterBarInTopBar = showFilterBarInTopBar,
                isSidebarExpanded = isSidebarExpanded,
                onToggleSidebar = { isSidebarExpanded = !isSidebarExpanded }
            )
        }
    } else {
        HomeMainContent(
            uiState = uiState,
            homeState = homeState,
            actions = actions,
            windowSizeClass = windowSizeClass,
            showSidebar = showSidebar,
            showFilterBarInTopBar = showFilterBarInTopBar,
            isSidebarExpanded = isSidebarExpanded,
            onToggleSidebar = { isSidebarExpanded = !isSidebarExpanded }
        )
    }
}

/**
 * Main UI content layout for [HomeContent].
 *
 * Displays the search bar, filter options, list/detail navigation display,
 * pull-to-refresh container, and floating action button.
 *
 * @param uiState Current UI state containing contact list, search query, and sync info.
 * @param homeState Screen state holder managing scroll state, snackbar host, and focus.
 * @param actions Callbacks for user interactions.
 * @param windowSizeClass Current window size class for responsive layout adjustments.
 * @param showSidebar Whether the navigation sidebar is displayed.
 * @param showFilterBarInTopBar Whether the label filter bar should be rendered in the top bar.
 * @param isSidebarExpanded Whether the navigation sidebar is currently expanded.
 * @param onToggleSidebar Callback invoked when the user clicks the menu icon to toggle the sidebar.
 * @param modifier Optional [Modifier] for the root container.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun HomeMainContent(
    uiState: HomeUiState,
    homeState: HomeState,
    actions: HomeActions,
    windowSizeClass: WindowSizeClass,
    showSidebar: Boolean,
    showFilterBarInTopBar: Boolean,
    isSidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    AppResponsiveScaffold(
        modifier = modifier,
        windowSizeClass = windowSizeClass,
        useAdaptiveWidth = false,
        snackbarHost = { SnackbarHost(hostState = homeState.snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                Column(
                    modifier = Modifier.padding(top = topPadding)
                ) {
                    SearchBar(
                        query = uiState.searchQuery,
                        placeholder = homeState.animatedPlaceholder,
                        onQueryChange = actions.onSearchQueryChange,
                        onClearQuery = actions.onClearSearch,
                        onSettingsClick = actions.onNavigateToSettings,
                        focusRequester = homeState.searchFocusRequester,
                        navigationIcon = if (showSidebar) {
                            {
                                IconButton(onClick = onToggleSidebar) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Toggle Sidebar"
                                    )
                                }
                            }
                        } else null,
                        modifier = Modifier.padding(bottom = SpacingSmall),
                    )
                    if (showFilterBarInTopBar && uiState.availableLabels.isNotEmpty()) {
                        LabelFilterBar(
                            visible = true,
                            labels = uiState.availableLabels,
                            selectedLabel = uiState.selectedLabel,
                            onLabelSelected = actions.onLabelSelected,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
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
            val contacts = uiState.contacts

            val getScrollLabel: (ContactUiModel) -> String = remember(uiState.selectedLabel) {
                { contact ->
                    if (uiState.selectedLabel == ContactLabels.LABEL_NO_BIRTHDAY) {
                        contact.fullName.firstOrNull()?.uppercaseChar()?.toString() ?: ""
                    } else {
                        contact.monthName
                    }
                }
            }


            val windowAdaptiveInfo = LocalWindowAdaptiveInfo.current
            val directive = remember(windowAdaptiveInfo) {
                calculatePaneScaffoldDirective(windowAdaptiveInfo)
                    .copy(horizontalPartitionSpacerSize = 0.dp)
            }
            val listDetailStrategy =
                rememberListDetailSceneStrategy<NavKey>(directive = directive)

            val backStack = rememberNavBackStack(HomeNavKey.ContactList)
            val selectedContactId =
                (backStack.lastOrNull() as? HomeNavKey.ContactDetail)?.contactId

            LaunchedEffect(contacts, selectedContactId) {
                if (selectedContactId != null && contacts?.none { it.id == selectedContactId } == true) {
                    backStack.removeLastOrNull()
                }
            }

            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                sceneStrategies = listOf(listDetailStrategy),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                entryProvider = entryProvider {
                    entry<HomeNavKey.ContactList>(
                        metadata = ListDetailSceneStrategy.listPane(
                            detailPlaceholder = {
                                BirthdayQuotePlaceholder(
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            BirthdayList(
                                contacts = contacts,
                                newlyAddedIdeaId = null, // Gift idea is added in the right detail pane
                                hasContactPermission = uiState.hasContactPermission,
                                listState = homeState.listState,
                                availableLabels = uiState.availableLabels,
                                selectedLabel = uiState.selectedLabel,
                                searchQuery = uiState.searchQuery,
                                actions = actions,
                                coupleSuggestion = uiState.coupleSuggestion,
                                selectedContactId = selectedContactId,
                                onContactSelected = { contact ->
                                    backStack.navigateToContactDetail(contact.id)
                                },
                                onInteraction = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                },
                                showLabelFilter = !showFilterBarInTopBar
                            )

                            val currentHeaderCount = remember(
                                uiState.availableLabels,
                                showFilterBarInTopBar,
                                uiState.selectedLabel,
                                uiState.coupleSuggestion
                            ) {
                                val currentShowLabelFilter =
                                    uiState.availableLabels.isNotEmpty() && !showFilterBarInTopBar
                                val currentShowCoupleSuggestion =
                                    uiState.selectedLabel == ContactLabels.LABEL_ANNIVERSARY && uiState.coupleSuggestion != null
                                (if (currentShowLabelFilter) 1 else 0) + (if (currentShowCoupleSuggestion) 1 else 0)
                            }

                            FastScrollbar(
                                listState = homeState.listState,
                                contacts = uiState.contacts ?: emptyList(),
                                getLabel = getScrollLabel,
                                headerCount = currentHeaderCount,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight(),
                                onSetFastScrolling = { homeState.onSetFastScrolling(it) },
                            )
                        }
                    }

                    entry<HomeNavKey.ContactDetail>(
                        metadata = ListDetailSceneStrategy.detailPane()
                    ) { key ->
                        val contact = remember(contacts, key.contactId) {
                            contacts?.find { it.id == key.contactId }
                        }
                        if (contact != null) {
                            BirthdayDetailPane(
                                contact = contact,
                                newlyAddedIdeaId = uiState.newlyAddedIdeaId,
                                actions = actions,
                                onClose = {
                                    backStack.removeLastOrNull()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            )
        }
    }
}

/**
 * Safely updates the backstack with the selected contact detail pane,
 * replacing any existing detail entry to prevent backstack growth.
 */
private fun MutableList<NavKey>.navigateToContactDetail(contactId: String) {
    if (lastOrNull() is HomeNavKey.ContactDetail) {
        removeLastOrNull()
    }
    add(HomeNavKey.ContactDetail(contactId))
}

@OptIn(ExperimentalMaterial3Api::class)
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

/**
 * Navigation keys for list-detail adaptive navigation in the home screen.
 */
@Serializable
private sealed interface HomeNavKey : NavKey {
    /**
     * Represents the primary contact list pane.
     */
    @Serializable
    data object ContactList : HomeNavKey

    /**
     * Represents the contact detail pane for a specific contact.
     *
     * @property contactId Unique identifier of the selected contact.
     */
    @Serializable
    data class ContactDetail(val contactId: String) : HomeNavKey
}
