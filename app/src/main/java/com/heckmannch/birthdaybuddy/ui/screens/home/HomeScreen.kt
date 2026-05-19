package com.heckmannch.birthdaybuddy.ui.screens.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.screens.home.components.*
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.viewmodel.HomeViewModel
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.model.GiftIdea
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * Der Hauptbildschirm der App.
 * Orchestriert die Suche, Filterung, Geburtstagsliste und die Fast-Scrollbar.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val homeState = rememberHomeState()
    
    val appPlaceholder = stringResource(R.string.home_placeholder_app)
    val searchPlaceholder = stringResource(R.string.home_placeholder_search)

    // --- Launchers ---
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) viewModel.syncContacts()
    }

    // --- Effekte ---
    LaunchedEffect(Unit) {
        homeState.animatedPlaceholder = appPlaceholder
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            viewModel.syncContacts()
        }
        
        delay(2000)
        homeState.animatedPlaceholder = searchPlaceholder
    }

    // Scroll-Reset Trigger (Widget Events, Suche oder Filterwechsel)
    LaunchedEffect(viewModel.scrollToTopEvent) {
        viewModel.scrollToTopEvent.collectLatest {
            homeState.resetScrollRequested = true
        }
    }

    LaunchedEffect(uiState.searchFocusRequested) {
        if (uiState.searchFocusRequested) {
            delay(500)
            homeState.searchFocusRequester.requestFocus()
            keyboardController?.show()
            viewModel.consumeSearchFocus()
        }
    }

    // Durchführung des Scroll-Resets sobald Daten da sind
    LaunchedEffect(uiState.contacts) {
        if (homeState.resetScrollRequested && (uiState.contacts != null)) {
            homeState.performScrollReset { viewModel.setIsResettingFilter(isResetting = false) }
        }
    }

    LaunchedEffect(homeState.listState.isScrollInProgress) {
        if (homeState.listState.isScrollInProgress && uiState.searchQuery.isNotEmpty()) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    // --- Callbacks ---
    val onRequestPermission = {
        val activity = context as? Activity
        val shouldShowRationale = activity?.let { 
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.READ_CONTACTS) 
        } ?: false

        if (shouldShowRationale || !homeState.hasAttemptedContactPermission) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            homeState.hasAttemptedContactPermission = true
        } else {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { 
                    data = Uri.fromParts("package", context.packageName, null) 
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    val onAddContact = {
        val intent = Intent(Intent.ACTION_INSERT).apply { 
            type = ContactsContract.Contacts.CONTENT_TYPE 
        }
        context.startActivity(intent)
    }

    val onOpenContact = { id: String, key: String ->
        try {
            id.toLongOrNull()?.let { numericId ->
                val lookupUri = ContactsContract.Contacts.getLookupUri(numericId, key)
                context.startActivity(Intent(Intent.ACTION_VIEW, lookupUri))
            }
        } catch (_: Exception) {}
        Unit
    }

    HomeContent(
        uiState = uiState,
        homeState = homeState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onLabelSelected = viewModel::onLabelSelected,
        onClearSearch = {
            viewModel.onSearchQueryChange("")
            focusManager.clearFocus()
            keyboardController?.hide()
        },
        onNavigateToSettings = onNavigateToSettings,
        onAddContact = onAddContact,
        onRequestPermission = onRequestPermission,
        onAddGiftIdea = viewModel::addGiftIdea,
        onToggleGiftIdea = viewModel::toggleGiftIdea,
        onUpdateGiftIdeaText = viewModel::updateGiftIdeaText,
        onDeleteGiftIdea = viewModel::deleteGiftIdea,
        onOpenContact = onOpenContact,
        onRefresh = { viewModel.syncContacts(showLoading = true) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    homeState: HomeState,
    onSearchQueryChange: (String) -> Unit,
    onLabelSelected: (String?) -> Unit,
    onClearSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onAddContact: () -> Unit,
    onRequestPermission: () -> Unit,
    onAddGiftIdea: (String) -> Unit,
    onToggleGiftIdea: (String, GiftIdea, Boolean) -> Unit,
    onUpdateGiftIdeaText: (String, String, String) -> Unit,
    onDeleteGiftIdea: (String, String) -> Unit,
    onOpenContact: (String, String) -> Unit,
    onRefresh: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    
    // Optimierung: Filter-Sichtbarkeit in derivedStateOf kapseln, damit HomeContent 
    // nicht bei jedem Scroll-Pixel re-composed.
    val isFilterBarVisible by remember(uiState.isResettingFilter, homeState) {
        derivedStateOf { homeState.isFilterBarVisible(uiState.isResettingFilter) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = homeState.snackbarHostState) },
        topBar = {
            HomeTopBar(
                searchQuery = uiState.searchQuery,
                animatedPlaceholder = homeState.animatedPlaceholder,
                availableLabels = uiState.availableLabels,
                selectedLabel = uiState.selectedLabel,
                isFilterBarVisible = isFilterBarVisible,
                onSearchQueryChange = onSearchQueryChange,
                onLabelSelected = onLabelSelected,
                onNavigateToSettings = onNavigateToSettings,
                onClearSearch = onClearSearch,
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
                    onAddContact = onAddContact,
                    onScrollToTop = {
                        focusManager.clearFocus()
                        homeState.scrollToTop()
                    },
                    modifier = Modifier.padding(8.dp) // Bewegt den FAB zusätzlich 8dp vom Rand weg
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isSyncing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            BirthdayList(
                contacts = uiState.contacts, 
                newlyAddedIdeaId = uiState.newlyAddedIdeaId,
                listState = homeState.listState,
                selectedLabel = uiState.selectedLabel,
                searchQuery = uiState.searchQuery,
                onRequestPermission = onRequestPermission,
                onAddGiftIdea = onAddGiftIdea,
                onToggleGiftIdea = onToggleGiftIdea,
                onUpdateGiftIdeaText = onUpdateGiftIdeaText,
                onDeleteGiftIdea = onDeleteGiftIdea,
                onOpenContact = onOpenContact,
            )

            FastScrollbar(
                listState = homeState.listState,
                contacts = uiState.contacts ?: emptyList(),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
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
    val sampleContacts = listOf(
        ContactUiModel(
            id = "1",
            contactId = "1",
            lookupKey = "k1",
            fullName = "Max Mustermann",
            dateText = "12. Mai",
            monthName = "Mai",
            imageUri = null,
            phoneNumber = null,
            initials = "M",
            nextAge = 30,
            daysUntilNext = 5,
            isToday = false,
            hasWhatsApp = true,
            hasSignal = false,
            labels = listOf("Freunde"),
            giftIdeas = emptyList(),
        ),
        ContactUiModel(
            id = "2",
            contactId = "2",
            lookupKey = "k2",
            fullName = "Erika Mustermann",
            dateText = "Heute",
            monthName = "Mai",
            imageUri = null,
            phoneNumber = null,
            initials = "E",
            nextAge = 40,
            daysUntilNext = 0,
            isToday = true,
            hasWhatsApp = false,
            hasSignal = false,
            labels = listOf("Familie"),
            giftIdeas = emptyList(),
        ),
    )
    BirthdayBuddyTheme {
        HomeContent(
            uiState = HomeUiState(
                contacts = sampleContacts,
                availableLabels = listOf("Familie", "Freunde", "Arbeit"),
            ),
            homeState = rememberHomeState(),
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
            onOpenContact = { _, _ -> },
            onRefresh = {},
        )
    }
}
