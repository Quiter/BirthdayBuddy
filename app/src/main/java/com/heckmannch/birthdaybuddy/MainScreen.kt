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

@Composable
fun MainScreen(
    filterManager: FilterManager,
    onNavigateToSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val context = LocalContext.current

    var contacts by remember { mutableStateOf<List<BirthdayContact>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    val hiddenFilterLabels by filterManager.hiddenFilterLabelsFlow.collectAsState(initial = emptySet())
    val excludedLabels by filterManager.excludedLabelsFlow.collectAsState(initial = emptySet())
    val hiddenDrawerLabels by filterManager.hiddenDrawerLabelsFlow.collectAsState(initial = emptySet())

    val notifHour by filterManager.notificationHourFlow.collectAsState(initial = 9)
    val notifMinute by filterManager.notificationMinuteFlow.collectAsState(initial = 0)

    // Hilfsfunktion zum Laden der Kontakte (vermeidet Warnungen und Code-Duplikate)
    val reloadContactsAction = suspend {
        isLoading = true
        val loadedContacts = withContext(Dispatchers.IO) { fetchBirthdays(context) }
        contacts = loadedContacts
        isLoading = false
    }

    // Berechtigungs-Logik
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms[Manifest.permission.READ_CONTACTS] == true) hasPermission = true
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            val perms = mutableListOf(Manifest.permission.READ_CONTACTS)
            if (android.os.Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
            permissionLauncher.launch(perms.toTypedArray())
        } else {
            reloadContactsAction()
        }
    }

    LaunchedEffect(notifHour, notifMinute) { scheduleDailyAlarm(context, notifHour, notifMinute) }

    val availableLabels = remember(contacts) { contacts.flatMap { it.labels }.toSortedSet() }
    val sortedContacts = remember(contacts, hiddenFilterLabels, excludedLabels, searchQuery) {
        contacts.filter { contact ->
            val isHidden = contact.labels.any { hiddenFilterLabels.contains(it) }
            val isExcluded = contact.labels.any { excludedLabels.contains(it) }
            val matches = contact.name.contains(searchQuery, ignoreCase = true)
            !isHidden && !isExcluded && matches
        }.sortedBy { it.remainingDays }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MainDrawerContent(
                availableLabels = availableLabels,
                hiddenFilterLabels = hiddenFilterLabels,
                hiddenDrawerLabels = hiddenDrawerLabels,
                onLabelToggle = { label, _ ->
                    val newSet = hiddenFilterLabels.toMutableSet()
                    if (hiddenFilterLabels.contains(label)) newSet.remove(label) else newSet.add(label)
                    scope.launch { filterManager.saveHiddenFilterLabels(newSet) }
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
                    BirthdayList(contacts = sortedContacts, listState = rememberLazyListState())
                }
            }
        }
    }
}
