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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy2.MainActivity
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
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val availableLabels by viewModel.availableLabels.collectAsState()
    val selectedLabel by viewModel.selectedLabel.collectAsState()
    
    var animatedPlaceholder by remember { mutableStateOf("BirthdayBuddy") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            viewModel.syncContacts()
        }
    }

    // Initialisierung & Berechtigungsprüfung
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            // Nur syncen, wenn Berechtigung bereits da ist
            viewModel.syncContacts()
        }
        
        delay(2000)
        animatedPlaceholder = "Kontakt suchen"
    }

    // ViewModel Events verarbeiten (z.B. Scroll-to-Top vom Widget)
    LaunchedEffect(Unit) {
        viewModel.scrollToTopEvent.collectLatest {
            listState.animateScrollToItem(0)
        }
    }

    // Intent-Handling (z.B. App-Start via Widget)
    val activity = context as? MainActivity
    LaunchedEffect(activity?.intent) {
        if (activity?.intent?.getBooleanExtra("SCROLL_TO_TOP", false) == true) {
            listState.animateScrollToItem(0)
            activity.intent.removeExtra("SCROLL_TO_TOP")
        }
    }

    val showScrollUp by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
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
                showScrollUp = showScrollUp,
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
            HomeFAB(
                showScrollUp = showScrollUp,
                onScrollToTop = { scope.launch { listState.animateScrollToItem(0) } },
            ) {
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    type = ContactsContract.Contacts.CONTENT_TYPE
                }
                context.startActivity(intent)
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    // Fokus verlieren, wenn man neben die Liste tippt
                    detectTapGestures { focusManager.clearFocus() }
                },
        ) {
            BirthdayList(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
                listState = listState,
            ) {
                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
            
            val contacts by viewModel.contacts.collectAsState()
            if (contacts?.isNotEmpty() == true) {
                FastScrollbar(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    searchQuery: String,
    animatedPlaceholder: String,
    availableLabels: List<String>,
    selectedLabel: String?,
    showScrollUp: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onLabelSelected: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onClearSearch: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 16.dp)
    ) {
        // Suchleiste
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        AnimatedContent(
                            targetState = animatedPlaceholder,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "Placeholder"
                        ) { text ->
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(Icons.Default.Close, contentDescription = "Suche löschen")
                    }
                } else {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                }
            }
        }

        // Filter-Chips
        if (availableLabels.isNotEmpty()) {
            AnimatedVisibility(
                visible = ((!showScrollUp) || (selectedLabel != null)),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableLabels) { label ->
                        FilterChip(
                            selected = selectedLabel == label,
                            onClick = { onLabelSelected(label) },
                            label = { Text(label) },
                            leadingIcon = if (selectedLabel == label) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeFAB(
    showScrollUp: Boolean,
    onScrollToTop: () -> Unit,
    onAddContact: () -> Unit,
) {
    val rotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (showScrollUp) 180f else 0f,
        label = "FAB Rotation"
    )

    FloatingActionButton(
        onClick = { if (showScrollUp) onScrollToTop() else onAddContact() },
        modifier = Modifier.rotate(rotation)
    ) {
        AnimatedContent(
            targetState = showScrollUp,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "FAB Icon Animation"
        ) { isUp ->
            if (isUp) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Nach oben")
            } else {
                Icon(Icons.Default.Add, contentDescription = "Kontakt hinzufügen")
            }
        }
    }
}
