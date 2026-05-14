package com.heckmannch.birthdaybuddy.ui.screens.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.screens.home.components.*
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.viewmodel.BirthdayViewModel
import com.heckmannch.birthdaybuddy.viewmodel.ContactUiModel
import com.heckmannch.birthdaybuddy.viewmodel.HomeUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.os.Build

/**
 * Der Hauptbildschirm der App.
 * Orchestriert die Suche, Filterung, Geburtstagsliste und die Fast-Scrollbar.
 */
@Composable
fun HomeScreen(
    viewModel: BirthdayViewModel,
    onNavigateToSettings: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    
    // UI State aus dem ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
    
    // Unser neuer Plain State Holder für die UI-Logik
    val homeState = rememberHomeState()
    
    val appPlaceholder = stringResource(R.string.home_placeholder_app)
    val searchPlaceholder = stringResource(R.string.home_placeholder_search)
    val enabledMsg = stringResource(R.string.onboarding_notif_enabled_msg)
    
    // Lokaler UI State
    var animatedPlaceholder by remember { mutableStateOf(appPlaceholder) }
    var resetScrollRequested by remember { mutableStateOf(value = false) }
    var onboardingDismissed by remember { mutableStateOf(false) }
    
    val hasContactPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    val showOnboarding = !onboardingCompleted && hasContactPermission && !onboardingDismissed

    // Berechtigungsprüfung & Initialisierung
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            viewModel.syncContacts()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        viewModel.setOnboardingCompleted(true)
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            viewModel.syncContacts()
        }
        
        delay(2000)
        animatedPlaceholder = searchPlaceholder
    }

    // --- Onboarding Dialog ---
    if (showOnboarding) {
        OnboardingDialog(
            onConfirm = {
                viewModel.setNotificationsEnabled(true)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.setOnboardingCompleted(true)
                }
                
                onboardingDismissed = true
                scope.launch {
                    homeState.snackbarHostState.showSnackbar(enabledMsg)
                }
            },
            onDismiss = {
                onboardingDismissed = true
                viewModel.setOnboardingCompleted(true)
            }
        )
    }

    // --- Scroll- & Filter-Logik ---

    // Reaktion auf externe Events (Widget) oder interne Filter-Wechsel
    LaunchedEffect(viewModel.scrollToTopEvent) {
        viewModel.scrollToTopEvent.collectLatest {
            resetScrollRequested = true
            viewModel.setIsResettingFilter(true)
        }
    }

    // Zusätzlicher Trigger für manuelle Suche/Label Auswahl
    LaunchedEffect(uiState.searchQuery, uiState.selectedLabel) {
        resetScrollRequested = true
        viewModel.setIsResettingFilter(true)
    }

    // Präziser Scroll-Reset sobald Daten geladen sind
    LaunchedEffect(uiState.contacts) {
        if (resetScrollRequested && (uiState.contacts != null)) {
            homeState.scrollToTop(animate = false)
            delay(100) // Puffer für UI-Stabilität
            homeState.scrollToTop(animate = false)
            viewModel.setIsResettingFilter(false)
            resetScrollRequested = false
        }
    }

    // Tastatur-Handling beim Scrollen
    LaunchedEffect(homeState.listState.isScrollInProgress) {
        if (homeState.listState.isScrollInProgress) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    // --- Callbacks stabilisieren ---
    val onSearchQueryChange = remember(viewModel) { { query: String -> viewModel.onSearchQueryChange(query) } }
    val onLabelSelected = remember(viewModel) { { label: String? -> viewModel.onLabelSelected(label) } }
    val onClearSearch = remember(viewModel, focusManager, keyboardController) {
        {
            viewModel.onSearchQueryChange("")
            focusManager.clearFocus()
            keyboardController?.hide()
            Unit
        }
    }
    val onAddContact = remember(context) {
        {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                type = ContactsContract.Contacts.CONTENT_TYPE
            }
            context.startActivity(intent)
        }
    }
    val onRequestPermission = remember(permissionLauncher) { { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) } }
    val onSetSwipeHintShown = remember(viewModel) { { viewModel.setSwipeHintShown(); Unit } }
    val onUpdateGiftIdeas = remember(viewModel) { { key: String, ideas: String -> viewModel.updateGiftIdeas(key, ideas); Unit } }
    val onOpenContact = remember(context) {
        { id: String, key: String ->
            try {
                val lookupUri = ContactsContract.Contacts.getLookupUri(id.toLong(), key)
                context.startActivity(Intent(Intent.ACTION_VIEW, lookupUri))
            } catch (_: Exception) {}
        }
    }

    HomeContent(
        uiState = uiState,
        homeState = homeState,
        animatedPlaceholder = animatedPlaceholder,
        onSearchQueryChange = onSearchQueryChange,
        onLabelSelected = onLabelSelected,
        onClearSearch = onClearSearch,
        onNavigateToSettings = onNavigateToSettings,
        onAddContact = onAddContact,
        onRequestPermission = onRequestPermission,
        onSetSwipeHintShown = onSetSwipeHintShown,
        onUpdateGiftIdeas = onUpdateGiftIdeas,
        onOpenContact = onOpenContact,
        onRefresh = { viewModel.syncContacts() },
    )
}

/**
 * Plain State Holder für die UI-Logik des HomeScreens.
 * Kapselt Scroll-Zustand, SnackBar-Management und Sichtbarkeiten.
 */
@Stable
class HomeState(
    val listState: LazyListState,
    val snackbarHostState: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    // Verhindert Layout-Sprünge beim Fast-Scrolling
    var filterVisibilityLock by mutableStateOf<Boolean?>(null)

    fun isFilterBarVisible(isResetting: Boolean): Boolean {
        if (isResetting) return true
        return filterVisibilityLock ?: (listState.firstVisibleItemIndex == 0)
    }

    val showScrollUp by derivedStateOf {
        listState.firstVisibleItemIndex > 0
    }

    fun onSetFastScrolling(isScrolling: Boolean) {
        filterVisibilityLock = if (isScrolling) {
            // Bei Start des Drags Zustand einfrieren (basierend auf dem ersten sichtbaren Item)
            listState.firstVisibleItemIndex == 0
        } else {
            null
        }
    }

    fun scrollToTop(animate: Boolean = true) {
        scope.launch {
            if (animate) listState.animateScrollToItem(0)
            else listState.scrollToItem(0)
        }
    }
}

@Composable
fun rememberHomeState(
    listState: LazyListState = rememberLazyListState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    scope: CoroutineScope = rememberCoroutineScope(),
): HomeState {
    return remember(listState, snackbarHostState, scope) {
        HomeState(listState, snackbarHostState, scope)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    homeState: HomeState,
    animatedPlaceholder: String,
    onSearchQueryChange: (String) -> Unit,
    onLabelSelected: (String?) -> Unit,
    onClearSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onAddContact: () -> Unit,
    onRequestPermission: () -> Unit,
    onSetSwipeHintShown: () -> Unit,
    onUpdateGiftIdeas: (String, String) -> Unit,
    onOpenContact: (String, String) -> Unit,
    onRefresh: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = homeState.snackbarHostState) },
        topBar = {
            HomeTopBar(
                searchQuery = uiState.searchQuery,
                animatedPlaceholder = animatedPlaceholder,
                availableLabels = uiState.availableLabels,
                selectedLabel = uiState.selectedLabel,
                isFilterBarVisible = homeState.isFilterBarVisible(uiState.isResettingFilter),
                onSearchQueryChange = onSearchQueryChange,
                onLabelSelected = onLabelSelected,
                onNavigateToSettings = onNavigateToSettings,
                onClearSearch = onClearSearch,
            )
        },
        floatingActionButton = {
            HomeFAB(
                showScrollUp = homeState.showScrollUp,
                onAddContact = onAddContact,
                onScrollToTop = {
                    focusManager.clearFocus()
                    homeState.scrollToTop()
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isSyncing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            BirthdayList(
                contacts = uiState.contacts ?: emptyList(),
                swipeHintShown = uiState.swipeHintShown,
                listState = homeState.listState,
                onRequestPermission = onRequestPermission,
                onSetSwipeHintShown = onSetSwipeHintShown,
                onUpdateGiftIdeas = onUpdateGiftIdeas,
                onOpenContact = onOpenContact,
            )

            FastScrollbar(
                listState = homeState.listState,
                contacts = uiState.contacts ?: emptyList(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
                isResettingFilter = uiState.isResettingFilter,
                onSetFastScrolling = { homeState.onSetFastScrolling(it) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    BirthdayBuddyTheme {
        HomeContent(
            uiState = HomeUiState(
                contacts = listOf(
                    ContactUiModel(
                        id = "1",
                        contactId = "1",
                        lookupKey = "key1",
                        fullName = "Max Mustermann",
                        dateText = "12. Mai",
                        monthName = "Mai",
                        imageUri = null,
                        initials = "M",
                        nextAge = 30,
                        nextAgeText = "wird 30",
                        daysUntilNext = 5,
                        daysLeftText = "In 5 T.",
                        isToday = false,
                        labels = listOf("Freunde"),
                        giftIdeas = emptyList()
                    )
                ),
                searchQuery = "",
                availableLabels = listOf("Freunde", "Familie"),
                selectedLabel = null,
                swipeHintShown = true,
                isResettingFilter = false,
                isSyncing = false
            ),
            homeState = rememberHomeState(),
            animatedPlaceholder = "Suchen...",
            onSearchQueryChange = {},
            onLabelSelected = {},
            onClearSearch = {},
            onNavigateToSettings = {},
            onAddContact = {},
            onRequestPermission = {},
            onSetSwipeHintShown = {},
            onUpdateGiftIdeas = { _, _ -> },
            onOpenContact = { _, _ -> },
            onRefresh = {}
        )
    }
}
