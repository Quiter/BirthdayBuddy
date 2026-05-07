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
import com.heckmannch.birthdaybuddy2.ui.screens.home.components.HomeFAB
import com.heckmannch.birthdaybuddy2.ui.screens.home.components.HomeTopBar
import com.heckmannch.birthdaybuddy2.viewmodel.BirthdayViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val availableLabels by viewModel.availableLabels.collectAsState()
    val selectedLabel by viewModel.selectedLabel.collectAsState()
    val isFastScrolling by viewModel.isFastScrolling.collectAsState()
    
    var animatedPlaceholder by remember { mutableStateOf("BirthdayBuddy") }

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
            listState.animateScrollToItem(0)
        }
    }

    val showScrollUp by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    // Verhinderung des Flimmerns der Filterbar beim Deaktivieren eines Filters
    var isResettingFilter by remember { mutableStateOf(value = false) }
    LaunchedEffect(selectedLabel) {
        if (selectedLabel == null) {
            isResettingFilter = true
            snapshotFlow { listState.firstVisibleItemIndex }.filter { it == 0 }.first()
            isResettingFilter = false
        }
    }

    val isFilterBarVisible by remember {
        derivedStateOf { (!showScrollUp) || (selectedLabel != null) || isResettingFilter }
    }

    // Tastatur schließen, wenn die Liste gescrollt wird
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
            keyboardController?.hide()
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
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                onLabelSelected = { viewModel.onLabelSelected(it) },
                onNavigateToSettings = onNavigateToSettings,
            ) {
                viewModel.onSearchQueryChange("")
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isFastScrolling,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                HomeFAB(
                    showScrollUp = showScrollUp,
                    onScrollToTop = { scope.launch { listState.animateScrollToItem(0) } },
                    onAddContact = {
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            type = ContactsContract.Contacts.CONTENT_TYPE
                        }
                        context.startActivity(intent)
                    }
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
            BirthdayList(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
                listState = listState,
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
            )
            
            val contacts by viewModel.contacts.collectAsState()
            if (!contacts.isNullOrEmpty()) {
                FastScrollbar(
                    viewModel = viewModel,
                    listState = listState,
                    contacts = contacts!!,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
}
