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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    // NEU: Die zu versteckenden Labels laden
    val hiddenDrawerLabels by filterManager.hiddenDrawerLabelsFlow.collectAsState(initial = emptySet())

    val context = androidx.compose.ui.platform.LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) hasPermission = true
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            contacts = fetchBirthdays(context)
        }
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(12.dp))
                    Text("Labels filtern", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp))

                    // NEU: Wir zeigen nur die an, die nicht in der "hiddenDrawerLabels"-Liste stehen!
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
                    val sortedContacts = filteredContacts.sortedBy { it.birthday.takeLast(5) }

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

                        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                            items(sortedContacts) { contact ->
                                BirthdayItem(contact = contact)
                            }
                        }
                    }
                }
            }
        }
    }
}