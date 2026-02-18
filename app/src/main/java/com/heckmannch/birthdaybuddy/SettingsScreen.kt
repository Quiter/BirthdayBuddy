package com.heckmannch.birthdaybuddy

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.components.*
import com.heckmannch.birthdaybuddy.utils.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMenuScreen(onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val filterManager = remember { FilterManager(context) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val widgetCount by filterManager.widgetItemCountFlow.collectAsState(initial = 1)
    var showCountDialog by remember { mutableStateOf(false) }
    val versionName = getAppVersionName()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp)) {
            SectionHeader("Benachrichtigungen")
            SettingsBlock("Alarme", "Erinnerungszeiten konfigurieren", Icons.Default.Notifications, Color(0xFF4CAF50)) { onNavigate("settings_alarms") }

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader("Filter & Sichtbarkeit")
            SettingsCard {
                val menuOrange = Color(0xFFFF9800)
                SettingsBlockRow("Anzeigen", "Diese Labels im Drawer anzeigen", Icons.Default.Visibility, menuOrange) { onNavigate("settings_hide") }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingsBlockRow("Blockieren", "Labels komplett ignorieren", Icons.Default.VisibilityOff, menuOrange) { onNavigate("settings_block") }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader("Widget")
            SettingsCard {
                val widgetBlue = Color(0xFF2196F3)
                SettingsBlockRow("Anzahl", "Bis zu $widgetCount Personen", Icons.Default.List, widgetBlue) { showCountDialog = true }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingsBlockRow("Anzeigen", "Diese Labels im Widget anzeigen", Icons.Default.Visibility, widgetBlue) { onNavigate("settings_widget_include") }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingsBlockRow("Blockieren", "Diese Labels im Widget ignorieren", Icons.Default.VisibilityOff, widgetBlue) { onNavigate("settings_widget_exclude") }
            }

            Spacer(modifier = Modifier.weight(1f))
            SettingsFooter(versionName) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Quiter/BirthdayBuddy")))
            }
        }
    }

    if (showCountDialog) {
        WidgetCountDialog(widgetCount, onDismiss = { showCountDialog = false }) { count ->
            scope.launch {
                filterManager.saveWidgetItemCount(count)
                updateWidget(context)
                showCountDialog = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(filterManager: FilterManager, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val notifHour by filterManager.notificationHourFlow.collectAsState(initial = 9)
    val notifMinute by filterManager.notificationMinuteFlow.collectAsState(initial = 0)
    val notifDaysSet by filterManager.notificationDaysFlow.collectAsState(initial = setOf("0", "7"))

    var showAddDayDialog by remember { mutableStateOf(false) }
    var newDayInput by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(initialHour = notifHour, initialMinute = notifMinute, is24Hour = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarme") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Hinzufügen") },
                icon = { Icon(Icons.Default.Add, null) },
                onClick = { showAddDayDialog = true }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            SectionHeader("Uhrzeit")
            ListItem(
                headlineContent = { Text("Standard-Uhrzeit") },
                supportingContent = { Text(String.format(java.util.Locale.getDefault(), "%02d:%02d Uhr", notifHour, notifMinute)) },
                leadingContent = { Icon(Icons.Default.Notifications, null) },
                modifier = Modifier.clickable { showTimePicker = true }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionHeader("Vorlaufzeiten")
            val sortedDays = notifDaysSet.mapNotNull { it.toIntOrNull() }.sorted()
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sortedDays) { day ->
                    val text = when (day) { 0 -> "Am Tag des Geburtstags"; 1 -> "1 Tag vorher"; 7 -> "1 Woche vorher"; else -> "$day Tage vorher" }
                    ListItem(
                        headlineContent = { Text(text) },
                        trailingContent = {
                            IconButton(onClick = {
                                val newSet = notifDaysSet.toMutableSet()
                                newSet.remove(day.toString())
                                scope.launch { filterManager.saveNotificationDays(newSet) }
                            }) { Icon(Icons.Default.Delete, "Löschen", tint = MaterialTheme.colorScheme.error) }
                        }
                    )
                }
            }
        }
    }
    // ... Dialoge für TimePicker und AddDay bleiben hier (identisch zu vorher) ...
}

// Navigations-Wrapper
@Composable fun BlockLabelsScreen(f: FilterManager, a: Set<String>, l: Boolean, b: () -> Unit) {
    val labels by f.excludedLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    LabelSelectionScreen("Blockieren", "Diese Kontakte werden komplett ausgeblendet.", a, labels, l, { label, checked ->
        val newSet = labels.toMutableSet()
        if (checked) newSet.add(label) else newSet.remove(label)
        scope.launch { f.saveExcludedLabels(newSet) }
    }, b)
}

@Composable fun HideLabelsScreen(f: FilterManager, a: Set<String>, l: Boolean, b: () -> Unit) {
    val hiddenLabels by f.hiddenDrawerLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    val activeLabels = a.filterNot { hiddenLabels.contains(it) }.toSet()
    LabelSelectionScreen("Anzeigen", "Diese Labels im Seitenmenü anzeigen.", a, activeLabels, l, { label, checked ->
        val newSet = hiddenLabels.toMutableSet()
        if (checked) newSet.remove(label) else newSet.add(label)
        scope.launch { f.saveHiddenDrawerLabels(newSet) }
    }, b)
}

@Composable fun WidgetIncludeLabelsScreen(f: FilterManager, a: Set<String>, l: Boolean, b: () -> Unit) {
    val context = LocalContext.current
    val hiddenLabels by f.widgetHiddenLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    val activeLabels = a.filterNot { hiddenLabels.contains(it) }.toSet()
    LabelSelectionScreen("Anzeigen", "Diese Labels im Widget anzeigen.", a, activeLabels, l, { label, checked ->
        val newSet = hiddenLabels.toMutableSet()
        if (checked) newSet.remove(label) else newSet.add(label)
        scope.launch {
            f.saveWidgetHiddenLabels(newSet)
            updateWidget(context)
        }
    }, b)
}

@Composable fun WidgetExcludeLabelsScreen(f: FilterManager, a: Set<String>, l: Boolean, b: () -> Unit) {
    val context = LocalContext.current
    val labels by f.widgetExcludedLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    LabelSelectionScreen("Blockieren", "Diese Labels im Widget immer ignorieren.", a, labels, l, { label, checked ->
        val newSet = labels.toMutableSet()
        if (checked) newSet.add(label) else newSet.remove(label)
        scope.launch {
            f.saveWidgetExcludedLabels(newSet)
            updateWidget(context)
        }
    }, b)
}