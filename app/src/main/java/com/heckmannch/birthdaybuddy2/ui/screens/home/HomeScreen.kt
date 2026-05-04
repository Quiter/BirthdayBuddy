package com.heckmannch.birthdaybuddy2.ui.screens.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.heckmannch.birthdaybuddy2.MainActivity
import com.heckmannch.birthdaybuddy2.ui.theme.BirthdayGold
import com.heckmannch.birthdaybuddy2.ui.theme.BirthdaySilver
import com.heckmannch.birthdaybuddy2.ui.theme.KidColors
import com.heckmannch.birthdaybuddy2.viewmodel.BirthdayViewModel
import com.heckmannch.birthdaybuddy2.viewmodel.ContactUiModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BirthdayViewModel,
    onNavigateToSettings: () -> Unit
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
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.syncContacts()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            viewModel.syncContacts()
        }
        
        delay(2000)
        animatedPlaceholder = "Kontakt suchen"
    }

    // Scroll to Top Event vom ViewModel oder Intent verarbeiten
    LaunchedEffect(Unit) {
        viewModel.scrollToTopEvent.collectLatest {
            listState.animateScrollToItem(0)
        }
    }

    // Falls die Activity bereits offen war und ein neuer Intent (via onNewIntent) reinkommt
    val activity = context as? MainActivity
    LaunchedEffect(activity?.intent) {
        if (activity?.intent?.getBooleanExtra("SCROLL_TO_TOP", false) == true) {
            listState.animateScrollToItem(0)
            // Intent extra zurücksetzen, damit nicht bei jedem Recompose gescrollt wird
            activity.intent.removeExtra("SCROLL_TO_TOP")
        }
    }

    val showScrollUp by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp)
            ) {
                // Suchleiste manuell aufgebaut für perfekte Ausrichtung auf allen Geräten
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
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                viewModel.onSearchQueryChange("")
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                scope.launch { listState.scrollToItem(0) }
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Suche löschen")
                            }
                        } else {
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                            }
                        }
                    }
                }

                // Filter-Chips: Nur anzeigen, wenn Labels vorhanden sind
                if (availableLabels.isNotEmpty()) {
                    AnimatedVisibility(
                        visible = !showScrollUp || selectedLabel != null,
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
                                    onClick = { 
                                        viewModel.onLabelSelected(label)
                                        scope.launch { listState.scrollToItem(0) }
                                    },
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
        },
        floatingActionButton = {
            val rotation by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (showScrollUp) 180f else 0f,
                label = "FAB Rotation"
            )

            FloatingActionButton(
                onClick = {
                    if (showScrollUp) {
                        scope.launch { listState.animateScrollToItem(0) }
                    } else {
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            type = ContactsContract.Contacts.CONTENT_TYPE
                        }
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.rotate(rotation)
            ) {
                AnimatedContent(
                    targetState = showScrollUp,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
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
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            BirthdayList(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
                listState = listState,
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
            )
            
            val contacts by viewModel.contacts.collectAsState()
            if (contacts.isNotEmpty()) {
                FastScrollbar(
                    listState = listState,
                    contacts = contacts,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun FastScrollbar(
    listState: LazyListState,
    contacts: List<ContactUiModel>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }

    // Berechne den Monat des ersten sichtbaren Items
    val currentMonth = remember {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex
            if (index in contacts.indices) contacts[index].monthName else ""
        }
    }

    // Scrollbar-Logik: Position berechnen
    BoxWithConstraints(modifier = modifier.width(150.dp)) {
        val totalItems = contacts.size

        val canScroll = remember {
            derivedStateOf {
                val visibleItems = listState.layoutInfo.visibleItemsInfo.size
                totalItems > visibleItems
            }
        }

        if (canScroll.value) {
            val viewHeight = maxHeight
            val thumbHeight = 48.dp
            val trackHeight = viewHeight - thumbHeight

            val thumbOffset = remember {
                derivedStateOf {
                    val scrollPercent = if (totalItems > 1) {
                        listState.firstVisibleItemIndex.toFloat() / (totalItems - 1)
                    } else 0f
                    trackHeight * scrollPercent
                }
            }

            val thumbWidth by animateDpAsState(
                targetValue = if (isDragging) 12.dp else 6.dp,
                label = "Thumb Width"
            )

            val thumbAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isDragging || listState.isScrollInProgress) 1f else 0.4f,
                label = "Thumb Alpha"
            )

            // Die Bubble (erscheint beim Scrollen oder Ziehen) - Links vom Griff
            AnimatedVisibility(
                visible = isDragging || (listState.isScrollInProgress && !isDragging),
                enter = fadeIn() + slideInHorizontally { it / 2 },
                exit = fadeOut() + slideOutHorizontally { it / 2 },
                modifier = Modifier
                    .offset { IntOffset(0, (thumbOffset.value.toPx() - 4.dp.toPx()).toInt()) }
                    .align(Alignment.TopStart)
            ) {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 24.dp,
                        bottomStart = 24.dp,
                        topEnd = 4.dp,
                        bottomEnd = 24.dp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 6.dp,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Text(
                        text = currentMonth.value,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Der Scroll-Griff (Thumb) mit vergrößerter Touch-Area
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, thumbOffset.value.toPx().toInt()) }
                    .align(Alignment.TopEnd)
                    .width(48.dp) // Großzügige Touch-Area (Material-Standard 48dp)
                    .height(thumbHeight)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onVerticalDrag = { change, _ ->
                                val dragY = change.position.y + thumbOffset.value.toPx()
                                val newScrollPercent = (dragY / viewHeight.toPx()).coerceIn(0f, 1f)
                                val targetIndex = (newScrollPercent * totalItems)
                                    .toInt()
                                    .coerceIn(0, totalItems - 1)
                                scope.launch {
                                    listState.scrollToItem(targetIndex)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.CenterEnd
            ) {
                // Visual Handle
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .width(thumbWidth)
                        .height(if (isDragging) 32.dp else 24.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = thumbAlpha)
                        )
                )
            }
        }
    }
}


@Composable
fun BirthdayList(
    viewModel: BirthdayViewModel,
    modifier: Modifier = Modifier,
    listState: LazyListState,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsState()
    
    val hasPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(contacts) {
        hasPermission.value = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    if (contacts.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!hasPermission.value) {
                Text(
                    text = "Um deine Geburtstage zu sehen, benötigt BirthdayBuddy Zugriff auf deine Kontakte.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onRequestPermission) {
                    Text("Berechtigung erteilen")
                }
            } else {
                Text(
                    text = "Keine Geburtstage gefunden. Synchronisiere deine Kontakte in den Einstellungen oder füge neue hinzu.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(
                items = contacts,
                key = { it.id },
                contentType = { "birthdayItem" }
            ) { contact ->
                BirthdayItem(contact)
            }
        }
    }
}

@Composable
fun BirthdayItem(contact: ContactUiModel) {
    val borderStroke = remember(contact) {
        if (contact.isToday && contact.nextAge != null) {
            when {
                contact.nextAge <= 10 -> {
                    BorderStroke(
                        width = 2.dp,
                        brush = Brush.linearGradient(KidColors)
                    )
                }
                contact.nextAge >= 20 && contact.nextAge % 10 == 0 -> {
                    BorderStroke(2.dp, BirthdayGold)
                }
                else -> {
                    BorderStroke(2.dp, BirthdaySilver)
                }
            }
        } else {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = borderStroke
    ) {
        ListItem(
            headlineContent = {
                Text(text = contact.fullName, style = MaterialTheme.typography.titleMedium)
            },
            supportingContent = {
                Text(
                    text = contact.dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingContent = {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    if (contact.imageUri != null) {
                        AsyncImage(
                            model = contact.imageUri,
                            contentDescription = "Kontaktbild von ${contact.fullName}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = contact.initials,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            },
            trailingContent = {
                Column(horizontalAlignment = Alignment.End) {
                    if (contact.nextAgeText != null) {
                        Text(
                            text = contact.nextAgeText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = contact.daysLeftText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (contact.isToday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}
