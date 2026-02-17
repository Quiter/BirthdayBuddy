package com.heckmannch.birthdaybuddy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.heckmannch.birthdaybuddy.components.BirthdayItem
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.utils.FilterManager
import com.heckmannch.birthdaybuddy.utils.fetchBirthdays
import com.heckmannch.birthdaybuddy.utils.scheduleDailyAlarm // NEU IMPORTIERT
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()
        enableEdgeToEdge()

        setContent {
            BirthdayBuddyTheme {
                val navController = rememberNavController()
                val filterManager = remember { FilterManager(this@MainActivity) }

                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            filterManager = filterManager,
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }

                    composable("settings") {
                        var contacts by remember { mutableStateOf<List<BirthdayContact>>(emptyList()) }
                        LaunchedEffect(Unit) {
                            contacts = fetchBirthdays(this@MainActivity)
                        }
                        val availableLabels = contacts.flatMap { it.labels }.toSortedSet()

                        SettingsScreen(
                            filterManager = filterManager,
                            availableLabels = availableLabels,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Geburtstags-Erinnerungen"
            val descriptionText = "Benachrichtigungen für anstehende Geburtstage"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel("birthday_channel", name, importance).apply {
                description = descriptionText
            }

            val notificationManager: android.app.NotificationManager =
                getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

@Composable
fun MainScreen(
    filterManager: FilterManager,
    onNavigateToSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var contacts by remember { mutableStateOf<List<BirthdayContact>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedLabels by remember { mutableStateOf(emptySet<String>()) }
    var isInitialized by remember { mutableStateOf(false) }

    val savedLabels by filterManager.selectedLabelsFlow.collectAsState(initial = null)
    val excludedLabels by filterManager.excludedLabelsFlow.collectAsState(initial = emptySet())
    val hiddenDrawerLabels by filterManager.hiddenDrawerLabelsFlow.collectAsState(initial = emptySet())

    // NEU: Alarmzeiten laden
    val notifHour by filterManager.notificationHourFlow.collectAsState(initial = 9)
    val notifMinute by filterManager.notificationMinuteFlow.collectAsState(initial = 0)

    val context = androidx.compose.ui.platform.LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    // NEU: Multi-Permission-Abfrage (für Kontakte UND Benachrichtigungen)
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
            // Ab Android 13 nach Benachrichtigungen fragen
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionLauncher.launch(permsToRequest.toTypedArray())
        } else {
            contacts = fetchBirthdays(context)
        }
    }

    // NEU: Jedes Mal, wenn die Uhrzeit geändert wird oder die App startet -> Wecker (neu) stellen!
    LaunchedEffect(notifHour, notifMinute) {
        scheduleDailyAlarm(context, notifHour, notifMinute)
    }

    val availableLabels = contacts.flatMap { it.labels }.toSortedSet()

    LaunchedEffect(availableLabels, savedLabels) {
        if (savedLabels != null && !isInitialized) {
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

                    val labelsToShow = availableLabels.filterNot { hiddenDrawerLabels.contains(it) }

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
                    val filteredContacts = contacts.filter { contact ->
                        val hasActiveLabel = contact.labels.any { label -> selectedLabels.contains(label) }
                        val isBlacklisted = contact.labels.any { label -> excludedLabels.contains(label) }
                        val matchesSearch = contact.name.contains(searchQuery, ignoreCase = true)

                        hasActiveLabel && !isBlacklisted && matchesSearch
                    }
                    val sortedContacts = filteredContacts.sortedBy { it.remainingDays }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier.padding(16.dp).fillMaxWidth().height(56.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
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
                                    singleLine = true
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(sortedContacts) { contact ->
                                    BirthdayItem(contact = contact)
                                }
                            }

                            if (sortedContacts.size > 10) {
                                val listProgress by remember {
                                    derivedStateOf {
                                        if (sortedContacts.isEmpty()) 0f
                                        else listState.firstVisibleItemIndex.toFloat() / sortedContacts.size
                                    }
                                }

                                BoxWithConstraints(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .fillMaxHeight()
                                        .padding(end = 4.dp, top = 8.dp, bottom = 8.dp)
                                        .width(4.dp)
                                        .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                                ) {
                                    val thumbHeight = 40.dp
                                    val thumbHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { thumbHeight.toPx() }
                                    val maxOffsetPx = constraints.maxHeight.toFloat() - thumbHeightPx
                                    val yOffset = (listProgress * maxOffsetPx).toInt().coerceAtLeast(0)

                                    Box(
                                        modifier = Modifier
                                            .offset { IntOffset(0, yOffset) }
                                            .size(width = 4.dp, height = thumbHeight)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
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