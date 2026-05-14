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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
    val listState = rememberLazyListState()
    
    // UI State aus dem ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val appPlaceholder = stringResource(R.string.home_placeholder_app)
    val searchPlaceholder = stringResource(R.string.home_placeholder_search)
    
    // Lokaler UI State
    var animatedPlaceholder by remember { mutableStateOf(appPlaceholder) }
    var resetScrollRequested by remember { mutableStateOf(value = false) }

    // Berechtigungsprüfung & Initialisierung
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) viewModel.syncContacts()
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
            listState.scrollToItem(0)
            delay(100) // Puffer für UI-Stabilität
            listState.scrollToItem(0)
            viewModel.setIsResettingFilter(false)
            resetScrollRequested = false
        }
    }

    // Tastatur-Handling beim Scrollen
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
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
    val onSetFastScrolling = remember(viewModel) { { scrolling: Boolean -> viewModel.setFastScrolling(scrolling) } }

    HomeContent(
        uiState = uiState,
        animatedPlaceholder = animatedPlaceholder,
        listState = listState,
        onSearchQueryChange = onSearchQueryChange,
        onLabelSelected = onLabelSelected,
        onClearSearch = onClearSearch,
        onNavigateToSettings = onNavigateToSettings,
        onAddContact = onAddContact,
        onRequestPermission = onRequestPermission,
        onSetSwipeHintShown = onSetSwipeHintShown,
        onUpdateGiftIdeas = onUpdateGiftIdeas,
        onOpenContact = onOpenContact,
        onSetFastScrolling = onSetFastScrolling,
        onRefresh = { viewModel.syncContacts() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    animatedPlaceholder: String,
    listState: LazyListState,
    onSearchQueryChange: (String) -> Unit,
    onLabelSelected: (String?) -> Unit,
    onClearSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onAddContact: () -> Unit,
    onRequestPermission: () -> Unit,
    onSetSwipeHintShown: () -> Unit,
    onUpdateGiftIdeas: (String, String) -> Unit,
    onOpenContact: (String, String) -> Unit,
    onSetFastScrolling: (Boolean) -> Unit,
    onRefresh: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    // Verhindert Layout-Sprünge: Wir "merken" uns die Sichtbarkeit der Filterleiste,
    // sobald ein Schnell-Scrollvorgang startet, damit sie sich währenddessen nicht ändert.
    var filterVisibilityLock by remember { mutableStateOf<Boolean?>(null) }
    
    val onSetFastScrollingLocal = remember(listState, uiState.isResettingFilter) {
        { isScrolling: Boolean ->
            if (isScrolling) {
                // Bei Start des Drags Zustand einfrieren (basierend auf dem ersten sichtbaren Item)
                filterVisibilityLock = listState.firstVisibleItemIndex == 0
            } else {
                filterVisibilityLock = null
            }
            onSetFastScrolling(isScrolling)
        }
    }

    val isFilterBarVisible by remember(listState, uiState.isResettingFilter, filterVisibilityLock) {
        derivedStateOf {
            if (uiState.isResettingFilter) return@derivedStateOf true
            filterVisibilityLock ?: (listState.firstVisibleItemIndex == 0)
        }
    }
    
    val showScrollUp by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                searchQuery = uiState.searchQuery,
                animatedPlaceholder = animatedPlaceholder,
                availableLabels = uiState.availableLabels,
                selectedLabel = uiState.selectedLabel,
                isFilterBarVisible = isFilterBarVisible,
                onSearchQueryChange = onSearchQueryChange,
                onLabelSelected = onLabelSelected,
                onNavigateToSettings = onNavigateToSettings,
                onClearSearch = onClearSearch,
            )
        },
        floatingActionButton = {
            HomeFAB(
                showScrollUp = showScrollUp,
                onAddContact = onAddContact,
                onScrollToTop = {
                    scope.launch {
                        focusManager.clearFocus()
                        listState.animateScrollToItem(0)
                    }
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
                listState = listState,
                onRequestPermission = onRequestPermission,
                onSetSwipeHintShown = onSetSwipeHintShown,
                onUpdateGiftIdeas = onUpdateGiftIdeas,
                onOpenContact = onOpenContact,
            )

            FastScrollbar(
                listState = listState,
                contacts = uiState.contacts ?: emptyList(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
                isResettingFilter = uiState.isResettingFilter,
                onSetFastScrolling = onSetFastScrollingLocal,
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
                isFastScrolling = false,
                swipeHintShown = true,
                isResettingFilter = false,
                isSyncing = false
            ),
            animatedPlaceholder = "Suchen...",
            listState = rememberLazyListState(),
            onSearchQueryChange = {},
            onLabelSelected = {},
            onClearSearch = {},
            onNavigateToSettings = {},
            onAddContact = {},
            onRequestPermission = {},
            onSetSwipeHintShown = {},
            onUpdateGiftIdeas = { _, _ -> },
            onOpenContact = { _, _ -> },
            onSetFastScrolling = {},
            onRefresh = {}
        )
    }
}
