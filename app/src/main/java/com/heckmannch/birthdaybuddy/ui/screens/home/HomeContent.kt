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
import androidx.compose.runtime.rememberUpdatedState
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    homeState: HomeState,
    actions: HomeActions,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    var isSidebarExpanded by rememberSaveable(windowSizeClass.isWidthExpanded) { mutableStateOf(windowSizeClass.isWidthExpanded) }
    val showSidebar = !windowSizeClass.isWidthCompact && !windowSizeClass.isHeightCompact
    val showFilterBarInTopBar = !uiState.contacts.isNullOrEmpty() &&
            !windowSizeClass.isWidthCompact &&
            !windowSizeClass.isHeightCompact &&
            !showSidebar

    val currentUiState by rememberUpdatedState(uiState)
    val currentActions by rememberUpdatedState(actions)

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val mainContent = @Composable {
        AppResponsiveScaffold(
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
                                    IconButton(onClick = { isSidebarExpanded = !isSidebarExpanded }) {
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
                val selectedContactId = (backStack.lastOrNull() as? HomeNavKey.ContactDetail)?.contactId

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
                                    newlyAddedIdeaId = null, // Idee wird im rechten Paneel hinzugefügt
                                    hasContactPermission = currentUiState.hasContactPermission,
                                    listState = homeState.listState,
                                    availableLabels = currentUiState.availableLabels,
                                    selectedLabel = currentUiState.selectedLabel,
                                    searchQuery = currentUiState.searchQuery,
                                    actions = currentActions,
                                    coupleSuggestion = currentUiState.coupleSuggestion,
                                    selectedContactId = selectedContactId,
                                    onContactSelected = { contact ->
                                        // Remove any existing detail to prevent backstack growth
                                        if (backStack.lastOrNull() is HomeNavKey.ContactDetail) {
                                            backStack.removeLastOrNull()
                                        }
                                        backStack.add(HomeNavKey.ContactDetail(contact.id))
                                    },
                                    onInteraction = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    },
                                    showLabelFilter = !showFilterBarInTopBar
                                )

                                val currentShowLabelFilter =
                                    currentUiState.availableLabels.isNotEmpty() && !showFilterBarInTopBar
                                val currentShowCoupleSuggestion =
                                    currentUiState.selectedLabel == ContactLabels.LABEL_ANNIVERSARY && currentUiState.coupleSuggestion != null
                                val currentHeaderCount =
                                    (if (currentShowLabelFilter) 1 else 0) + (if (currentShowCoupleSuggestion) 1 else 0)

                                FastScrollbar(
                                    listState = homeState.listState,
                                    contacts = currentUiState.contacts ?: emptyList(),
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
                                    newlyAddedIdeaId = currentUiState.newlyAddedIdeaId,
                                    actions = currentActions,
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
            mainContent()
        }
    } else {
        mainContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HomePreview() {
    val actions = HomeActions(
        onSearchQueryChange = {},
        onLabelSelected = {},
        onClearSearch = {},
        onNavigateToSettings = {},
        onAddContact = {},
        onRequestPermission = {},
        onAddGiftIdea = {},
        onToggleGiftIdea = { _, _, _ -> },
        onUpdateGiftIdeaText = { _, _, _ -> },
        onDeleteGiftIdea = { _, _ -> },
        onUpdateBirthday = { _, _ -> },
        onOpenContact = { _, _ -> },
        onDial = {},
        onSendSms = {},
        onOpenMessengerApp = { _, _ -> },
        onRefresh = {},
    )

    BirthdayBuddyTheme {
        CompositionLocalProvider(
            LocalWindowSizeClass provides WindowSizeClass(360, 640)
        ) {
            HomeContent(
                uiState = SampleData.homeUiState,
                homeState = rememberHomeState(),
                actions = actions,
            )
        }
    }
}

@Serializable
private sealed interface HomeNavKey : NavKey {
    @Serializable
    data object ContactList : HomeNavKey

    @Serializable
    data class ContactDetail(val contactId: String) : HomeNavKey
}

