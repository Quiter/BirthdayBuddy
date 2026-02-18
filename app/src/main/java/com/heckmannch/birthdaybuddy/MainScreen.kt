package com.heckmannch.birthdaybuddy

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy.components.BirthdayItem
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.utils.FilterManager
import com.heckmannch.birthdaybuddy.utils.fetchBirthdays
import com.heckmannch.birthdaybuddy.utils.scheduleDailyAlarm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MainScreen(
    filterManager: FilterManager,
    onNavigateToSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val keyboardController = LocalSoftwareKeyboardController.current

    var contacts by remember { mutableStateOf<List<BirthdayContact>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedLabels by remember { mutableStateOf(emptySet<String>()) }
    var isInitialized by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) } // Optischer Ladekreis

    val savedLabels by filterManager.selectedLabelsFlow.collectAsState(initial = null)
    val excludedLabels by filterManager.excludedLabelsFlow.collectAsState(initial = emptySet())
    val hiddenDrawerLabels by filterManager.hiddenDrawerLabelsFlow.collectAsState(initial = emptySet())

    val notifHour by filterManager.notificationHourFlow.collectAsState(initial = 9)
    val notifMinute by filterManager.notificationMinuteFlow.collectAsState(initial = 0)

    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_CONTACTS] == true) {
            hasPermission = true
        }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            val permsToRequest = mutableListOf(Manifest.permission.READ_CONTACTS)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionLauncher.launch(permsToRequest.toTypedArray())
        } else {
            isLoading = true
            // FIX 1: Das initiale Lesen aus der Datenbank auf den Hintergrund-Thread verlagern
            withContext(Dispatchers.IO) {
                contacts = fetchBirthdays(context)
            }
            isLoading = false
        }
    }

    LaunchedEffect(notifHour, notifMinute) {
        scheduleDailyAlarm(context, notifHour, notifMinute)
    }

    // FIX 2: Das "Gedächtnis" (remember) einbauen. Diese Liste wird nur neu berechnet,
    // wenn sich das Telefonbuch (contacts) wirklich ändert.
    val availableLabels = remember(contacts) {
        contacts.flatMap { it.labels }.toSortedSet()
    }

    // FIX: Wir warten mit der Zuweisung, bis das Laden der Kontakte (!isLoading) beendet ist!
    LaunchedEffect(availableLabels, savedLabels, isLoading) {
        if (savedLabels != null && !isLoading && !isInitialized) {
            selectedLabels = if (savedLabels!!.isEmpty() && availableLabels.isNotEmpty()) {
                availableLabels.also { scope.launch { filterManager.saveSelectedLabels(it) } }
            } else {
                savedLabels!!.filter { availableLabels.contains(it) }.toSet()
            }
            isInitialized = true
        }
    }

    val listState = rememberLazyListState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(12.dp))
                    Text("Labels filtern", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp))

                    // Performance-Fix auch für das Menü
                    val labelsToShow = remember(availableLabels, hiddenDrawerLabels) {
                        availableLabels.filterNot { hiddenDrawerLabels.contains(it) }
                    }

                    labelsToShow.forEach { label ->
                        NavigationDrawerItem(
                            label = { Text(label) },
                            selected = selectedLabels.contains(label),
                            onClick = {
                                val newSelection = selectedLabels.toMutableSet()
                                if (selectedLabels.contains(label)) newSelection.remove(label) else newSelection.add(label)
                                selectedLabels = newSelection
                                scope.launch { filterManager.saveSelectedLabels(newSelection) }
                            },
                            icon = { Checkbox(checked = selectedLabels.contains(label), onCheckedChange = null) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 28.dp))

                    // NEU: Der Reload-Button
                    NavigationDrawerItem(
                        label = { Text("Kontakte neu laden") },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close() // Menü sanft schließen
                                isLoading = true    // Ladekreis anzeigen
                                // Im Hintergrund das neue Telefonbuch holen
                                withContext(Dispatchers.IO) {
                                    contacts = fetchBirthdays(context)
                                }
                                isLoading = false   // Ladekreis wieder weg
                            }
                        },
                        icon = { Icon(Icons.Default.Refresh, contentDescription = "Neu laden") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    // Der bestehende Einstellungen-Button
                    NavigationDrawerItem(
                        label = { Text("Einstellungen") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToSettings()
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                }
            }
        }
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                if (hasPermission) {

                    // FIX 3: Der allerwichtigste Fix!
                    // Diese extrem rechenintensive Liste wird jetzt im Gedächtnis behalten.
                    // Sie rechnet nur neu, wenn du tippst, suchst oder Filter änderst!
                    val sortedContacts = remember(contacts, selectedLabels, excludedLabels, searchQuery) {
                        contacts.filter { contact ->
                            val hasActiveLabel = contact.labels.any { label -> selectedLabels.contains(label) }
                            val isBlacklisted = contact.labels.any { label -> excludedLabels.contains(label) }
                            val matchesSearch = contact.name.contains(searchQuery, ignoreCase = true)

                            hasActiveLabel && !isBlacklisted && matchesSearch
                        }.sortedBy { it.remainingDays }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier.padding(16.dp).fillMaxWidth().height(56.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    keyboardController?.hide()
                                    scope.launch { drawerState.open() }
                                }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("In Kontakten suchen...", color = Color.Gray) },
                                    modifier = Modifier.weight(1f),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words
                                    ),
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "Suche löschen"
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary, // Die gestohlene Hauptfarbe
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant, // Die sichtbare Hintergrund-Spur
                                    strokeWidth = 4.dp, // Schön griffig und dick
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round // Macht die Enden butterweich abgerundet!
                                ) // Während die DB anfangs lädt
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    items(
                                        items = sortedContacts,
                                        // HIER: Die perfekte, stabile ID aus Name und Datum!
                                        key = { contact -> contact.name + contact.birthday }
                                    ) { contact ->
                                        BirthdayItem(
                                            contact = contact,
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}