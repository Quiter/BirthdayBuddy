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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
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
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.ui.components.AppHeightSizeClass
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.components.AppWidthSizeClass
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowHeightSizeClass
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.components.actions.HomeFAB
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.BirthdayDetailPane
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.BirthdayList
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.BirthdayQuotePlaceholder
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.FastScrollbar
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.LabelFilterBar
import com.heckmannch.birthdaybuddy.ui.screens.home.components.topbar.SearchBar
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    homeState: HomeState,
    actions: HomeActions,
    windowWidthSizeClass: AppWidthSizeClass,
) {
    val windowHeightSizeClass = LocalWindowHeightSizeClass.current
    val showFilterBarInTopBar = !uiState.contacts.isNullOrEmpty() &&
            windowWidthSizeClass != AppWidthSizeClass.COMPACT &&
            windowHeightSizeClass != AppHeightSizeClass.COMPACT

    val currentUiState by rememberUpdatedState(uiState)
    val currentActions by rememberUpdatedState(actions)

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    AppResponsiveScaffold(
        windowWidthSizeClass = windowWidthSizeClass,
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

            if (contacts.isNullOrEmpty() || windowWidthSizeClass == AppWidthSizeClass.COMPACT) {
                Box(modifier = Modifier.fillMaxSize()) {
                    BirthdayList(
                        contacts = contacts,
                        newlyAddedIdeaId = uiState.newlyAddedIdeaId,
                        hasContactPermission = uiState.hasContactPermission,
                        listState = homeState.listState,
                        availableLabels = uiState.availableLabels,
                        selectedLabel = uiState.selectedLabel,
                        searchQuery = uiState.searchQuery,
                        actions = actions,
                        coupleSuggestion = uiState.coupleSuggestion,
                        onInteraction = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                        contentPadding = paddingValues,
                        showLabelFilter = !showFilterBarInTopBar
                    )

                    val showLabelFilter =
                        uiState.availableLabels.isNotEmpty() && !showFilterBarInTopBar
                    val showCoupleSuggestion =
                        uiState.selectedLabel == ContactLabels.LABEL_ANNIVERSARY && uiState.coupleSuggestion != null
                    val headerCount =
                        (if (showLabelFilter) 1 else 0) + (if (showCoupleSuggestion) 1 else 0)

                    FastScrollbar(
                        listState = homeState.listState,
                        contacts = contacts ?: emptyList(),
                        getLabel = getScrollLabel,
                        headerCount = headerCount,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(top = paddingValues.calculateTopPadding()),
                        onSetFastScrolling = { homeState.onSetFastScrolling(it) },
                    )
                }
            } else {
                var selectedContactId by rememberSaveable { mutableStateOf<String?>(null) }

                // Deselektieren, wenn der ausgewählte Kontakt nicht mehr in der Liste ist
                LaunchedEffect(contacts, selectedContactId) {
                    if (selectedContactId != null && contacts.none { it.id == selectedContactId }) {
                        selectedContactId = null
                    }
                }

                val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
                val directive = remember(windowAdaptiveInfo) {
                    calculatePaneScaffoldDirective(windowAdaptiveInfo)
                        .copy(horizontalPartitionSpacerSize = 0.dp)
                }
                val listDetailStrategy =
                    rememberListDetailSceneStrategy<HomeNavKey>(directive = directive)

                val backStack = remember(selectedContactId) {
                    if (selectedContactId == null) {
                        listOf<HomeNavKey>(HomeNavKey.ContactList)
                    } else {
                        listOf<HomeNavKey>(
                            HomeNavKey.ContactList,
                            HomeNavKey.ContactDetail(selectedContactId!!)
                        )
                    }
                }

                NavDisplay(
                    backStack = backStack,
                    onBack = { selectedContactId = null },
                    sceneStrategies = listOf(listDetailStrategy),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    entryProvider = { key ->
                        when (key) {
                            is HomeNavKey.ContactList -> NavEntry(
                                key,
                                metadata = ListDetailSceneStrategy.listPane(
                                    detailPlaceholder = {
                                        BirthdayQuotePlaceholder(
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                )) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    BirthdayList(
                                        contacts = currentUiState.contacts,
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
                                            selectedContactId = contact.id
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

                            is HomeNavKey.ContactDetail -> NavEntry(
                                key,
                                metadata = ListDetailSceneStrategy.detailPane()
                            ) {
                                val contact = remember(currentUiState.contacts, key.contactId) {
                                    currentUiState.contacts?.find { it.id == key.contactId }
                                }
                                if (contact != null) {
                                    BirthdayDetailPane(
                                        contact = contact,
                                        newlyAddedIdeaId = currentUiState.newlyAddedIdeaId,
                                        actions = currentActions,
                                        onClose = {
                                            selectedContactId = null
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
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
        HomeContent(
            uiState = SampleData.homeUiState,
            homeState = rememberHomeState(),
            actions = actions,
            windowWidthSizeClass = AppWidthSizeClass.COMPACT
        )
    }
}

@Serializable
private sealed interface HomeNavKey : NavKey {
    @Serializable
    data object ContactList : HomeNavKey

    @Serializable
    data class ContactDetail(val contactId: String) : HomeNavKey
}
