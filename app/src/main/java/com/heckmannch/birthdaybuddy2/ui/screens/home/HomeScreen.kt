package com.heckmannch.birthdaybuddy.ui.screens.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.screens.home.components.BirthdayList
import com.heckmannch.birthdaybuddy.ui.screens.home.components.FastScrollbar
import com.heckmannch.birthdaybuddy.ui.screens.home.components.HomeFAB
import com.heckmannch.birthdaybuddy.ui.screens.home.components.HomeTopBar
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.viewmodel.BirthdayViewModel
import com.heckmannch.birthdaybuddy.viewmodel.ContactUiModel
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
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val availableLabels by viewModel.availableLabels.collectAsStateWithLifecycle()
    val selectedLabel by viewModel.selectedLabel.collectAsStateWithLifecycle()
    val isFastScrolling by viewModel.isFastScrolling.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val swipeHintShown by viewModel.swipeHintShown.collectAsStateWithLifecycle()
    
    val appPlaceholder = stringResource(R.string.home_placeholder_app)
    val searchPlaceholder = stringResource(R.string.home_placeholder_search)
    
    // Lokaler UI State
    var animatedPlaceholder by remember { mutableStateOf(appPlaceholder) }
    var isResettingFilter by remember { mutableStateOf(value = false) }
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
            isResettingFilter = true
        }
    }

    // Zusätzlicher Trigger für manuelle Suche/Label Auswahl
    LaunchedEffect(searchQuery, selectedLabel) {
        resetScrollRequested = true
        isResettingFilter = true
    }

    // Präziser Scroll-Reset sobald Daten geladen sind
    LaunchedEffect(contacts) {
        if (resetScrollRequested && (contacts != null)) {
            listState.scrollToItem(0)
            delay(100) // Puffer für UI-Stabilität
            listState.scrollToItem(0)
            isResettingFilter = false
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
        searchQuery = searchQuery,
        animatedPlaceholder = animatedPlaceholder,
        availableLabels = availableLabels,
        selectedLabel = selectedLabel,
        isFastScrolling = isFastScrolling,
        contacts = contacts,
        swipeHintShown = swipeHintShown,
        isResettingFilter = isResettingFilter,
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
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    searchQuery: String,
    animatedPlaceholder: String,
    availableLabels: List<String>,
    selectedLabel: String?,
    isFastScrolling: Boolean,
    contacts: List<ContactUiModel>?,
    swipeHintShown: Boolean,
    isResettingFilter: Boolean,
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
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    // Verhindert Layout-Sprünge: Wir "merken" uns die Sichtbarkeit der Filterleiste,
    // sobald ein Schnell-Scrollvorgang startet, damit sie sich währenddessen nicht ändert.
    var filterVisibilityLock by remember { mutableStateOf<Boolean?>(null) }
    
    val onSetFastScrollingLocal = remember(listState, isResettingFilter) {
        { isScrolling: Boolean ->
            filterVisibilityLock = if (isScrolling) {
                // Sofort den aktuellen Zustand einfrieren
                (listState.firstVisibleItemIndex == 0) || isResettingFilter
            } else {
                // Erst nach dem Loslassen wieder freigeben
                null
            }
            onSetFastScrolling(isScrolling)
        }
    }

    val showScrollUp by remember {
        derivedStateOf { (listState.firstVisibleItemIndex > 0) && !isResettingFilter }
    }

    val isFilterBarVisible by remember {
        derivedStateOf { 
            filterVisibilityLock ?: ((!showScrollUp) || isResettingFilter || searchQuery.isNotEmpty())
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            HomeTopBar(
                searchQuery = searchQuery,
                animatedPlaceholder = animatedPlaceholder,
                availableLabels = availableLabels,
                selectedLabel = selectedLabel,
                isFilterBarVisible = isFilterBarVisible,
                onSearchQueryChange = onSearchQueryChange,
                onLabelSelected = onLabelSelected,
                onNavigateToSettings = onNavigateToSettings,
                onClearSearch = onClearSearch,
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isFastScrolling,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                HomeFAB(
                    showScrollUp = showScrollUp,
                    onScrollToTop = { scope.launch { listState.animateScrollToItem(0) } },
                    onAddContact = onAddContact,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTapGestures { focusManager.clearFocus() }
                },
        ) {
            AnimatedContent(
                targetState = contacts,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith
                    fadeOut(animationSpec = tween(400))
                },
                label = "ListCrossfade",
            ) { targetContacts ->
                BirthdayList(
                    contacts = targetContacts ?: emptyList(),
                    swipeHintShown = swipeHintShown,
                    modifier = Modifier.fillMaxSize(),
                    listState = listState,
                    onRequestPermission = onRequestPermission,
                    onSetSwipeHintShown = onSetSwipeHintShown,
                    onUpdateGiftIdeas = onUpdateGiftIdeas,
                    onOpenContact = onOpenContact,
                )
            }
            
            contacts?.let { contactList ->
                if (contactList.isNotEmpty()) {
                    FastScrollbar(
                        listState = listState,
                        contacts = contactList,
                        isResettingFilter = isResettingFilter,
                        onSetFastScrolling = onSetFastScrollingLocal,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    BirthdayBuddyTheme {
        HomeContent(
            searchQuery = "",
            animatedPlaceholder = "Kontakt suchen",
            availableLabels = listOf("Familie", "Freunde", "Arbeit"),
            selectedLabel = null,
            isFastScrolling = false,
            contacts = listOf(
                ContactUiModel(
                    id = "1",
                    contactId = "1",
                    lookupKey = "key1",
                    fullName = "Max Mustermann",
                    dateText = "15. Mai",
                    monthName = "Mai",
                    imageUri = null,
                    initials = "M",
                    nextAge = 30,
                    nextAgeText = "wird 30",
                    daysUntilNext = 0,
                    daysLeftText = "Heute!",
                    isToday = true,
                    labels = listOf("Familie"),
                    giftIdeas = emptyList()
                ),
                ContactUiModel(
                    id = "2",
                    contactId = "2",
                    lookupKey = "key2",
                    fullName = "Erika Musterfrau",
                    dateText = "20. Mai",
                    monthName = "Mai",
                    imageUri = null,
                    initials = "E",
                    nextAge = 25,
                    nextAgeText = "wird 25",
                    daysUntilNext = 5,
                    daysLeftText = "In 5 T.",
                    isToday = false,
                    labels = emptyList(),
                    giftIdeas = emptyList()
                )
            ),
            swipeHintShown = true,
            isResettingFilter = false,
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
            onSetFastScrolling = {}
        )
    }
}
