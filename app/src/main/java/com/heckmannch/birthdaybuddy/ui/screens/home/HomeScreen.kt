package com.heckmannch.birthdaybuddy.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.imageLoader
import coil.request.ImageRequest
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.components.actions.HomeFAB
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.BirthdayList
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.FastScrollbar
import com.heckmannch.birthdaybuddy.ui.screens.home.components.topbar.HomeTopBar
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.util.ContactActions
import com.heckmannch.birthdaybuddy.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * Der Hauptbildschirm der App.
 * Orchestriert die Suche, Filterung, Geburtstagsliste und die Fast-Scrollbar.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    windowWidthSizeClass: WindowWidthSizeClass,
    onNavigateToSettings: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val homeState = rememberHomeState()
    val contactActions = remember(context) { ContactActions(context) }

    val appPlaceholder = stringResource(R.string.home_placeholder_app)
    val searchPlaceholder = stringResource(R.string.home_placeholder_search)

    // --- Launchers ---
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) viewModel.syncContacts()
        }

    // --- UI-Koordination (Initialisierung & Fokus) ---
    LaunchedEffect(Unit) {
        homeState.animatedPlaceholder = appPlaceholder
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.syncContacts()
        }
        delay(2000)
        homeState.animatedPlaceholder = searchPlaceholder
    }

    LaunchedEffect(uiState.searchFocusRequested) {
        if (uiState.searchFocusRequested) {
            delay(500) // Zeit für Animationen lassen
            homeState.searchFocusRequester.requestFocus()
            keyboardController?.show()
            viewModel.consumeSearchFocus()
        }
    }

    // --- Listen-Koordination (Scroll-Events & Keyboard) ---
    LaunchedEffect(viewModel.scrollToTopEvent) {
        viewModel.scrollToTopEvent.collectLatest {
            homeState.resetScrollRequested = true
        }
    }

    // Führt den Scroll-Reset durch, sobald Daten geladen wurden
    LaunchedEffect(uiState.contacts, homeState.resetScrollRequested) {
        if (homeState.resetScrollRequested && (uiState.contacts != null)) {
            homeState.performScrollReset { viewModel.setIsResettingFilter(isResetting = false) }
        }
    }

    val isListDragged by homeState.listState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(isListDragged) {
        if (isListDragged) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    // --- Bild-Preloading für flüssigeres Scrollen ---
    val imageLoader = context.imageLoader
    LaunchedEffect(uiState.contacts) {
        // Preloade die ersten 25 Bilder, sobald die Liste geladen ist.
        // Das füllt den Memory- & Disk-Cache, bevor der User scrollt.
        uiState.contacts?.take(25)?.forEach { contact ->
            if (contact.imageUri != null) {
                val request = ImageRequest.Builder(context)
                    .data(contact.imageUri)
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }

    val actions =
        remember(viewModel, onNavigateToSettings, contactActions, permissionLauncher, homeState) {
            HomeActions(
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onLabelSelected = viewModel::onLabelSelected,
                onClearSearch = {
                    viewModel.onSearchQueryChange("")
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },
                onNavigateToSettings = onNavigateToSettings,
                onAddContact = contactActions::addContact,
                onRequestPermission = {
                    contactActions.requestContactPermission(
                        launcher = permissionLauncher,
                        hasAttemptedBefore = homeState.hasAttemptedContactPermission,
                    ) { homeState.hasAttemptedContactPermission = true }
                },
                onAddGiftIdea = viewModel::addGiftIdea,
                onToggleGiftIdea = viewModel::toggleGiftIdea,
                onUpdateGiftIdeaText = viewModel::updateGiftIdeaText,
                onDeleteGiftIdea = viewModel::deleteGiftIdea,
                onUpdateBirthday = viewModel::updateBirthday,
                onOpenContact = contactActions::openContact,
                onDial = contactActions::dialNumber,
                onSendSms = contactActions::sendSms,
                onOpenMessengerApp = contactActions::openMessengerApp,
                onRefresh = { viewModel.syncContacts(showLoading = true) },
            )
        }

    HomeContent(
        uiState = uiState,
        homeState = homeState,
        actions = actions,
        windowWidthSizeClass = windowWidthSizeClass
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
                windowWidthSizeClass = windowWidthSizeClass,
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
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    ) {
        PullToRefreshBox(
            isRefreshing = uiState.isSyncing,
            onRefresh = actions.onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            BirthdayList(
                contacts = uiState.contacts,
                newlyAddedIdeaId = uiState.newlyAddedIdeaId,
                listState = homeState.listState,
                selectedLabel = uiState.selectedLabel,
                searchQuery = uiState.searchQuery,
                actions = actions,
                onInteraction = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            )

            FastScrollbar(
                listState = homeState.listState,
                contacts = uiState.contacts ?: emptyList(),
                getLabel = { contact ->
                    if (uiState.selectedLabel == HomeViewModel.LABEL_NO_BIRTHDAY) {
                        contact.fullName.firstOrNull()?.uppercaseChar()?.toString() ?: ""
                    } else {
                        contact.monthName
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
                isResettingFilter = uiState.isResettingFilter,
                onSetFastScrolling = { homeState.onSetFastScrolling(it) },
            )
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
