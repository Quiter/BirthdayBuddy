package com.heckmannch.birthdaybuddy.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.navigation3.runtime.*
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import androidx.compose.ui.unit.dp
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import coil.imageLoader
import coil.request.ImageRequest
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.components.actions.HomeFAB
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.BirthdayDetailPane
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.BirthdayList
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.BirthdayQuotePlaceholder
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.FastScrollbar
import com.heckmannch.birthdaybuddy.ui.screens.home.components.topbar.HomeTopBar
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import com.heckmannch.birthdaybuddy.ui.util.ContactActions
import com.heckmannch.birthdaybuddy.viewmodel.HomeIntent
import com.heckmannch.birthdaybuddy.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration.Companion.milliseconds

/**
 * Der Hauptbildschirm der App.
 * Orchestriert die Suche, Filterung, Geburtstagsliste und die Fast-Scrollbar.
 *
 * Die Signatur akzeptiert nur noch reinen UI-State und einen Intent-Handler,
 * damit der Composable ohne Hilt/ViewModel isoliert testbar ist.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
    scrollToTopEvent: SharedFlow<Unit>,
    windowWidthSizeClass: WindowWidthSizeClass,
    onNavigateToSettings: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val homeState = rememberHomeState()
    val contactActions = remember(context) { ContactActions(context) }
    val currentOnNavigateToSettings by rememberUpdatedState(onNavigateToSettings)

    val appPlaceholder = stringResource(R.string.home_placeholder_app)
    val searchPlaceholder = stringResource(R.string.home_placeholder_search)

    // --- Launchers ---
    val writePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d("HomeScreen", "WRITE_CONTACTS permission granted silently")
                onIntent(HomeIntent.SyncContacts())
            }
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_CONTACTS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    writePermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                } else {
                    onIntent(HomeIntent.SyncContacts())
                }
            }
        }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            writePermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
        }
    }

    // --- UI-Koordination (Initialisierung & Fokus) ---
    LaunchedEffect(Unit) {
        homeState.animatedPlaceholder = appPlaceholder
        delay(2000.milliseconds)
        homeState.animatedPlaceholder = searchPlaceholder
    }

    LaunchedEffect(uiState.searchFocusRequested) {
        if (uiState.searchFocusRequested) {
            delay(500.milliseconds) // Zeit für Animationen lassen
            homeState.searchFocusRequester.requestFocus()
            keyboardController?.show()
            onIntent(HomeIntent.ConsumeSearchFocus)
        }
    }

    LaunchedEffect(uiState.newlyAddedIdeaId) {
        if (uiState.newlyAddedIdeaId != null) {
            delay(100.milliseconds) // Zeit für UI-Fokus-Anforderung lassen
            onIntent(HomeIntent.ConsumeNewlyAddedIdeaId)
        }
    }

    // --- Image Prefetching (UI Optimization) ---
    // Optimierung: Nur triggern, wenn sich die ersten IDs tatsächlich ändern, um redundantem
    // Prefetching während des Tippens in der Suche vorzubeugen.
    val firstContactIds = remember(uiState.contacts) {
        uiState.contacts?.take(20)?.map { it.id } ?: emptyList()
    }
    LaunchedEffect(firstContactIds) {
        val contacts = uiState.contacts
        if (!contacts.isNullOrEmpty()) {
            contacts.take(20)
                .mapNotNull { it.imageUri }
                .forEach { uri ->
                    val request = ImageRequest.Builder(context)
                        .data(uri)
                        .size(150)
                        .memoryCacheKey(uri) // Konsistente Keys nutzen
                        .build()
                    context.imageLoader.enqueue(request)
                }
        }
    }

    // --- Listen-Koordination (Scroll-Events & Keyboard) ---
    LaunchedEffect(scrollToTopEvent) {
        scrollToTopEvent.collectLatest {
            homeState.resetScrollRequested = true
        }
    }

    // Führt den Scroll-Reset durch, sobald Daten geladen wurden
    LaunchedEffect(uiState.contacts, homeState.resetScrollRequested) {
        if (homeState.resetScrollRequested && (uiState.contacts != null)) {
            homeState.performScrollReset {
                onIntent(HomeIntent.SetIsResettingFilter(isResetting = false))
            }
        }
    }

    val isListDragged by homeState.listState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(isListDragged) {
        if (isListDragged) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    val actions =
        remember(onIntent, contactActions, permissionLauncher, homeState) {
            HomeActions(
                onSearchQueryChange = { query -> onIntent(HomeIntent.SearchQueryChanged(query)) },
                onLabelSelected = { label -> onIntent(HomeIntent.LabelSelected(label)) },
                onClearSearch = {
                    onIntent(HomeIntent.SearchQueryChanged(""))
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },
                onNavigateToSettings = { currentOnNavigateToSettings() },
                onAddContact = contactActions::addContact,
                onRequestPermission = {
                    contactActions.requestContactPermission(
                        launcher = permissionLauncher,
                        hasAttemptedBefore = homeState.hasAttemptedContactPermission,
                    ) { homeState.hasAttemptedContactPermission = true }
                },
                onAddGiftIdea = { lookupKey -> onIntent(HomeIntent.AddGiftIdea(lookupKey)) },
                onToggleGiftIdea = { lookupKey, idea, isChecked ->
                    onIntent(HomeIntent.ToggleGiftIdea(lookupKey, idea, isChecked))
                },
                onUpdateGiftIdeaText = { lookupKey, ideaId, newText ->
                    onIntent(HomeIntent.UpdateGiftIdeaText(lookupKey, ideaId, newText))
                },
                onDeleteGiftIdea = { lookupKey, ideaId ->
                    onIntent(HomeIntent.DeleteGiftIdea(lookupKey, ideaId))
                },
                onUpdateBirthday = { contactId, birthday ->
                    onIntent(HomeIntent.UpdateBirthday(contactId, birthday))
                },
                onOpenContact = contactActions::openContact,
                onDial = contactActions::dialNumber,
                onSendSms = contactActions::sendSms,
                onOpenMessengerApp = contactActions::openMessengerApp,
                onRefresh = { onIntent(HomeIntent.SyncContacts(showLoading = true)) },
                onUnlinkCouple = { lookupKey -> onIntent(HomeIntent.UnlinkCouple(lookupKey)) },
                onLinkAsCouple = { key1, key2 -> onIntent(HomeIntent.LinkAsCouple(key1, key2)) },
                onIgnoreCoupleSuggestion = { key1, key2 ->
                    onIntent(HomeIntent.IgnoreCoupleSuggestion(key1, key2))
                },
            )
        }

    HomeContent(
        uiState = uiState,
        homeState = homeState,
        actions = actions,
        windowWidthSizeClass = windowWidthSizeClass
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    homeState: HomeState,
    actions: HomeActions,
    windowWidthSizeClass: WindowWidthSizeClass,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Optimierung: Filter-Sichtbarkeit in derivedStateOf kapseln, damit HomeContent 
    // nicht bei jedem Scroll-Pixel re-composed.
    val isFilterBarVisible by remember(uiState.isResettingFilter, homeState) {
        derivedStateOf { homeState.isFilterBarVisible(uiState.isResettingFilter) }
    }

    AppResponsiveScaffold(
        windowWidthSizeClass = windowWidthSizeClass,
        useAdaptiveWidth = false,
        snackbarHost = { SnackbarHost(hostState = homeState.snackbarHostState) },
        topBar = {
            HomeTopBar(
                searchQuery = uiState.searchQuery,
                animatedPlaceholder = homeState.animatedPlaceholder,
                availableLabels = uiState.availableLabels,
                selectedLabel = uiState.selectedLabel,
                isFilterBarVisible = isFilterBarVisible,
                actions = actions,
                searchFocusRequester = homeState.searchFocusRequester,
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
            val contacts = uiState.contacts

            val getScrollLabel: (ContactUiModel) -> String = remember(uiState.selectedLabel) {
                { contact ->
                    if (uiState.selectedLabel == HomeViewModel.LABEL_NO_BIRTHDAY) {
                        contact.fullName.firstOrNull()?.uppercaseChar()?.toString() ?: ""
                    } else {
                        contact.monthName
                    }
                }
            }

            if (contacts.isNullOrEmpty() || windowWidthSizeClass == WindowWidthSizeClass.Compact) {
                Box(modifier = Modifier.fillMaxSize()) {
                    BirthdayList(
                        contacts = contacts,
                        newlyAddedIdeaId = uiState.newlyAddedIdeaId,
                        listState = homeState.listState,
                        selectedLabel = uiState.selectedLabel,
                        searchQuery = uiState.searchQuery,
                        actions = actions,
                        coupleSuggestion = uiState.coupleSuggestion,
                        onInteraction = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                        contentPadding = paddingValues
                    )

                    FastScrollbar(
                        listState = homeState.listState,
                        contacts = contacts ?: emptyList(),
                        getLabel = getScrollLabel,
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
                val listDetailStrategy = rememberListDetailSceneStrategy<HomeNavKey>(directive = directive)

                val backStack = remember(selectedContactId) {
                    if (selectedContactId == null) {
                        listOf<HomeNavKey>(HomeNavKey.ContactList)
                    } else {
                        listOf<HomeNavKey>(HomeNavKey.ContactList, HomeNavKey.ContactDetail(selectedContactId!!))
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
                            is HomeNavKey.ContactList -> NavEntry(key, metadata = ListDetailSceneStrategy.listPane(
                                detailPlaceholder = {
                                    BirthdayQuotePlaceholder(
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            )) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    BirthdayList(
                                        contacts = contacts,
                                        newlyAddedIdeaId = null, // Idee wird im rechten Paneel hinzugefügt
                                        listState = homeState.listState,
                                        selectedLabel = uiState.selectedLabel,
                                        searchQuery = uiState.searchQuery,
                                        actions = actions,
                                        coupleSuggestion = uiState.coupleSuggestion,
                                        selectedContactId = selectedContactId,
                                        onContactSelected = { contact ->
                                            selectedContactId = contact.id
                                        },
                                        onInteraction = {
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        }
                                    )

                                    FastScrollbar(
                                        listState = homeState.listState,
                                        contacts = contacts,
                                        getLabel = getScrollLabel,
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight(),
                                        onSetFastScrolling = { homeState.onSetFastScrolling(it) },
                                    )
                                }
                            }
                            is HomeNavKey.ContactDetail -> NavEntry(key, metadata = ListDetailSceneStrategy.detailPane()) {
                                val contact = remember(contacts, key.contactId) {
                                    contacts.find { it.id == key.contactId }
                                }
                                if (contact != null) {
                                    BirthdayDetailPane(
                                        contact = contact,
                                        newlyAddedIdeaId = uiState.newlyAddedIdeaId,
                                        actions = actions,
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
            windowWidthSizeClass = WindowWidthSizeClass.Compact
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
