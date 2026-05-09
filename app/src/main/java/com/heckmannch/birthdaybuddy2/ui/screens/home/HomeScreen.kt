package com.heckmannch.birthdaybuddy2.ui.screens.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy2.ui.screens.home.components.BirthdayList
import com.heckmannch.birthdaybuddy2.ui.screens.home.components.FastScrollbar
import com.heckmannch.birthdaybuddy2.ui.screens.home.components.HomeFAB
import com.heckmannch.birthdaybuddy2.ui.screens.home.components.HomeTopBar
import com.heckmannch.birthdaybuddy2.viewmodel.BirthdayViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Der Hauptbildschirm der App.
 * Enthält die Suchleiste, Filter-Chips, die Geburtstagsliste und die Fast-Scrollbar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BirthdayViewModel,
    onNavigateToSettings: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val availableLabels by viewModel.availableLabels.collectAsStateWithLifecycle()
    val selectedLabel by viewModel.selectedLabel.collectAsStateWithLifecycle()
    val isFastScrolling by viewModel.isFastScrolling.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val swipeHintShown by viewModel.swipeHintShown.collectAsStateWithLifecycle()
    
    var animatedPlaceholder by remember { mutableStateOf("BirthdayBuddy") }
    
    // Verhinderung des Flimmerns der Filterbar beim Deaktivieren eines Filters oder Suche
    var isResettingFilter by remember { mutableStateOf(value = false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) viewModel.syncContacts()
    }

    // Initialisierung & Berechtigungsprüfung
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            viewModel.syncContacts()
        }
        
        delay(2000)
        animatedPlaceholder = "Kontakt suchen"
    }

    // ViewModel Events verarbeiten (z.B. Scroll-to-Top vom Widget)
    LaunchedEffect(viewModel.scrollToTopEvent) {
        viewModel.scrollToTopEvent.collectLatest {
            isResettingFilter = true
            // Wir nutzen scrollToItem(0) für Filter/Suche, weil es robuster gegen Inhaltsänderungen ist
            listState.scrollToItem(0)
            // Kurze Verzögerung, um auf das asynchrone Update der Kontaktliste zu warten
            delay(150)
            listState.scrollToItem(0)
            isResettingFilter = false
        }
    }

    val showScrollUp by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 && !isResettingFilter }
    }

    val isFilterBarVisible by remember {
        derivedStateOf { (!showScrollUp) || isResettingFilter }
    }

    // Tastatur schließen, wenn die Liste gescrollt wird
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    val onSearchQueryChange = remember(viewModel) {
        { query: String -> viewModel.onSearchQueryChange(query) }
    }

    val onLabelSelected = remember(viewModel) {
        { label: String? ->
            isResettingFilter = true
            viewModel.onLabelSelected(label)
        }
    }

    val onClearSearch = remember(viewModel, focusManager, keyboardController) {
        {
            isResettingFilter = true
            viewModel.onSearchQueryChange("")
            focusManager.clearFocus()
            keyboardController?.hide()
            Unit
        }
    }

    val onSetSwipeHintShown = remember(viewModel) {
        { viewModel.setSwipeHintShown() }
    }

    val onUpdateGiftIdeas = remember(viewModel) {
        { key: String, ideas: String -> viewModel.updateGiftIdeas(key, ideas) }
    }

    val onOpenContact = remember(context) {
        { id: String, key: String ->
            try {
                val lookupUri = ContactsContract.Contacts.getLookupUri(id.toLong(), key)
                context.startActivity(Intent(Intent.ACTION_VIEW, lookupUri))
            } catch (_: Exception) {
            }
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
                    onScrollToTop = { scope.launch { listState.animateScrollToItem(0) } }
                ) {
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        type = ContactsContract.Contacts.CONTENT_TYPE
                    }
                    context.startActivity(intent)
                }
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
            BirthdayList(
                contacts = contacts ?: emptyList(),
                swipeHintShown = swipeHintShown,
                modifier = Modifier.fillMaxSize(),
                listState = listState,
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                onSetSwipeHintShown = onSetSwipeHintShown,
                onUpdateGiftIdeas = onUpdateGiftIdeas,
                onOpenContact = onOpenContact,
            )
            
            contacts?.let { contactList ->
                if (contactList.isNotEmpty()) {
                    FastScrollbar(
                        listState = listState,
                        contacts = contactList,
                        isResettingFilter = isResettingFilter,
                        onSetFastScrolling = { viewModel.setFastScrolling(it) },
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
