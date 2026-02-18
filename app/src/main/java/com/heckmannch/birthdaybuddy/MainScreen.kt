package com.heckmannch.birthdaybuddy

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    filterManager: FilterManager,
    onNavigateToSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var contacts by remember { mutableStateOf<List<BirthdayContact>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Wir nutzen jetzt die Blacklist-Flows
    val hiddenFilterLabels by filterManager.hiddenFilterLabelsFlow.collectAsState(initial = emptySet())
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
            withContext(Dispatchers.IO) {
                contacts = fetchBirthdays(context)
            }
            isLoading = false
        }
    }

    LaunchedEffect(notifHour, notifMinute) {
        scheduleDailyAlarm(context, notifHour, notifMinute)
    }

    val availableLabels = remember(contacts) {
        contacts.flatMap { it.labels }.toSortedSet()
    }

    val listState = rememberLazyListState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Labels filtern",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                )

                val labelsToShowInDrawer = remember(availableLabels, hiddenDrawerLabels) {
                    availableLabels.filterNot { hiddenDrawerLabels.contains(it) }
                }

                labelsToShowInDrawer.forEach { label ->
                    val isChecked = !hiddenFilterLabels.contains(label)
                    NavigationDrawerItem(
                        label = { Text(label) },
                        selected = isChecked,
                        onClick = {
                            val newHiddenSet = hiddenFilterLabels.toMutableSet()
                            if (isChecked) newHiddenSet.add(label) else newHiddenSet.remove(label)
                            scope.launch { filterManager.saveHiddenFilterLabels(newHiddenSet) }
                        },
                        icon = { Checkbox(checked = isChecked, onCheckedChange = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 28.dp))

                NavigationDrawerItem(
                    label = { Text("Kontakte neu laden") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            isLoading = true
                            withContext(Dispatchers.IO) {
                                contacts = fetchBirthdays(context)
                            }
                            isLoading = false
                        }
                    },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = "Neu laden") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

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
    ) {
        Scaffold(
            topBar = {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { },
                    active = false,
                    onActiveChange = { },
                    placeholder = { Text("In Kontakten suchen...") },
                    leadingIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Suche löschen")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                ) { }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (hasPermission) {
                    val sortedContacts = remember(contacts, hiddenFilterLabels, excludedLabels, searchQuery) {
                        contacts.filter { contact ->
                            // Kontakt anzeigen, wenn KEINES seiner Labels versteckt oder blockiert ist
                            val isHiddenByFilter = contact.labels.any { hiddenFilterLabels.contains(it) }
                            val isExcludedGlobally = contact.labels.any { excludedLabels.contains(it) }
                            val matchesSearch = contact.name.contains(searchQuery, ignoreCase = true)
                            
                            !isHiddenByFilter && !isExcludedGlobally && matchesSearch
                        }.sortedBy { it.remainingDays }
                    }

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(
                                items = sortedContacts,
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
