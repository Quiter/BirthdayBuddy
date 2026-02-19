package com.heckmannch.birthdaybuddy

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy.components.*
import com.heckmannch.birthdaybuddy.utils.*
import kotlinx.coroutines.launch

/**
 * Der Hauptbildschirm der App. 
 * Er nutzt das MainViewModel für eine saubere Trennung von Logik und UI.
 */
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    filterManager: FilterManager,
    onNavigateToSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val context = LocalContext.current

    // Daten aus dem ViewModel beobachten (als State für Compose)
    val contacts by mainViewModel.filteredContacts.collectAsState()
    val isLoading by mainViewModel.isLoading.collectAsState()
    val searchQuery by mainViewModel.searchQuery.collectAsState()
    val availableLabels by mainViewModel.availableLabels.collectAsState()
    
    // Filter-States für den Drawer
    val savedSelectedLabels by filterManager.selectedLabelsFlow.collectAsState(initial = emptySet())
    val hiddenDrawerLabels by filterManager.hiddenDrawerLabelsFlow.collectAsState(initial = emptySet())

    // Berechtigungs-Management
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms[Manifest.permission.READ_CONTACTS] == true) {
            hasPermission = true
            mainViewModel.loadContacts() // Kontakte laden, sobald Berechtigung erteilt wurde
        }
    }

    // Berechtigung beim Start prüfen
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            val perms = mutableListOf(Manifest.permission.READ_CONTACTS)
            if (android.os.Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
            permissionLauncher.launch(perms.toTypedArray())
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MainDrawerContent(
                availableLabels = availableLabels,
                selectedLabels = savedSelectedLabels ?: emptySet(),
                hiddenDrawerLabels = hiddenDrawerLabels,
                onLabelToggle = { label, _ -> mainViewModel.toggleLabel(label) },
                onReloadContacts = {
                    scope.launch {
                        drawerState.close()
                        mainViewModel.loadContacts()
                    }
                },
                onSettingsClick = {
                    scope.launch { drawerState.close() }
                    onNavigateToSettings()
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                MainSearchBar(
                    query = searchQuery,
                    onQueryChange = { mainViewModel.updateSearchQuery(it) },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (contacts.isEmpty() && hasPermission) {
                    // Optional: Leerer State, wenn keine Kontakte gefunden wurden
                    Text(
                        text = "Keine Geburtstage gefunden.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    BirthdayList(
                        contacts = contacts, 
                        listState = rememberLazyListState()
                    )
                }
            }
        }
    }
}
