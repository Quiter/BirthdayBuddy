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
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Der Hauptbildschirm der App, der die Liste der Geburtstage anzeigt.
 * Er verwaltet den State für Kontakte, Suche, Filter und Berechtigungen.
 * 
 * @param filterManager Zentrale Instanz zum Lesen/Schreiben von Filtereinstellungen.
 * @param onNavigateToSettings Callback zur Navigation in die Einstellungen.
 */
@Composable
fun MainScreen(
    filterManager: FilterManager,
    onNavigateToSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val context = LocalContext.current

    // State-Variablen für UI und Daten
    var contacts by remember { mutableStateOf<List<BirthdayContact>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isInitialized by remember { mutableStateOf(false) }

    // Beobachtet die Filter-Einstellungen aus dem FilterManager (DataStore/SharedPreferences)
    val savedSelectedLabels by filterManager.selectedLabelsFlow.collectAsState(initial = null)
    val excludedLabels by filterManager.excludedLabelsFlow.collectAsState(initial = emptySet())
    val hiddenDrawerLabels by filterManager.hiddenDrawerLabelsFlow.collectAsState(initial = emptySet())

    // Hilfsfunktion zum (Neu-)Laden der Kontakte aus dem System
    val reloadContactsAction = suspend {
        isLoading = true
        contacts = withContext(Dispatchers.IO) { fetchBirthdays(context) }
        isLoading = false
    }

    // Berechtigungs-Management für den Kontaktzugriff
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms[Manifest.permission.READ_CONTACTS] == true) hasPermission = true
    }

    // Prüft Berechtigungen beim Start und lädt ggf. die Daten
    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            val perms = mutableListOf(Manifest.permission.READ_CONTACTS)
            // Benachrichtigungs-Berechtigung ab Android 13 erforderlich
            if (android.os.Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
            permissionLauncher.launch(perms.toTypedArray())
        } else {
            reloadContactsAction()
        }
    }

    // Extrahiert alle verfügbaren Labels (Gruppen) aus den geladenen Kontakten
    val availableLabels = remember(contacts) { contacts.flatMap { it.labels }.toSortedSet() }

    // Falls die App zum ersten Mal startet, werden standardmäßig alle Labels aktiviert
    LaunchedEffect(availableLabels, savedSelectedLabels) {
        if (availableLabels.isNotEmpty() && savedSelectedLabels != null && !isInitialized) {
            if (savedSelectedLabels!!.isEmpty()) {
                filterManager.saveSelectedLabels(availableLabels)
            }
            isInitialized = true
        }
    }

    // Filter- und Sortier-Logik für die Kontaktliste
    val sortedContacts = remember(contacts, savedSelectedLabels, excludedLabels, searchQuery) {
        val selected = savedSelectedLabels ?: emptySet()
        contacts.filter { contact ->
            // 1. Globale Sperre (Labels, die in den Einstellungen blockiert wurden)
            if (contact.labels.any { excludedLabels.contains(it) }) return@filter false
            
            // 2. Suche nach Namen (Groß-/Kleinschreibung ignorieren)
            if (!contact.name.contains(searchQuery, ignoreCase = true)) return@filter false

            // 3. Whitelist-Check: Zeige nur Kontakte, die zu einem aktiven Label gehören
            contact.labels.any { selected.contains(it) }
        }.sortedBy { it.remainingDays } // Sortiere nach den Tagen bis zum Geburtstag
    }

    // UI-Struktur mit Drawer (Seitenmenü) und Scaffold (Grundlayout)
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MainDrawerContent(
                availableLabels = availableLabels,
                selectedLabels = savedSelectedLabels ?: emptySet(),
                hiddenDrawerLabels = hiddenDrawerLabels,
                onLabelToggle = { label, isCurrentlySelected ->
                    val newSelection = (savedSelectedLabels ?: emptySet()).toMutableSet()
                    if (isCurrentlySelected) newSelection.remove(label) else newSelection.add(label)
                    scope.launch { filterManager.saveSelectedLabels(newSelection) }
                },
                onReloadContacts = {
                    scope.launch {
                        drawerState.close()
                        reloadContactsAction()
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
                    onQueryChange = { searchQuery = it },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    // Die eigentliche Liste der Kontakte
                    BirthdayList(contacts = sortedContacts, listState = rememberLazyListState())
                }
            }
        }
    }
}
