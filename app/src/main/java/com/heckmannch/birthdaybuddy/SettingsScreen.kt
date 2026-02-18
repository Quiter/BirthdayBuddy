package com.heckmannch.birthdaybuddy

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.heckmannch.birthdaybuddy.components.*
import com.heckmannch.birthdaybuddy.utils.*
import kotlinx.coroutines.launch

/**
 * Hauptmenü der App-Einstellungen.
 * Bietet eine Übersicht über Benachrichtigungen, Filter-Optionen und Widget-Konfigurationen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMenuScreen(onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val filterManager = remember { FilterManager(context) }
    // Verhalten für die TopAppBar, damit sie beim Scrollen einklappt
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
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            // Sektion für Alarme und Erinnerungen
            SectionHeader("Benachrichtigungen")
            SettingsBlock("Alarme", "Erinnerungszeiten konfigurieren", Icons.Default.Notifications, Color(0xFF4CAF50)) { onNavigate("settings_alarms") }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Sektion für die Sichtbarkeit von Kontakten und Labels in der App
            SectionHeader("Filter & Sichtbarkeit")
            SettingsCard {
                val menuOrange = Color(0xFFFF9800)
                SettingsBlockRow("Anzeigen", "Diese Labels im Drawer anzeigen", Icons.Default.Visibility, menuOrange) { onNavigate("settings_hide") }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingsBlockRow("Blockieren", "Labels komplett ignorieren", Icons.Default.VisibilityOff, menuOrange) { onNavigate("settings_block") }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Sektion für das Homescreen-Widget
            SectionHeader("Widget")
            SettingsCard {
                val widgetBlue = Color(0xFF2196F3)
                SettingsBlockRow("Anzahl", "Bis zu $widgetCount Personen", Icons.AutoMirrored.Filled.List, widgetBlue) { showCountDialog = true }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingsBlockRow("Anzeigen", "Diese Labels im Widget anzeigen", Icons.Default.Visibility, widgetBlue) { onNavigate("settings_widget_include") }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingsBlockRow("Blockieren", "Diese Labels im Widget ignorieren", Icons.Default.VisibilityOff, widgetBlue) { onNavigate("settings_widget_exclude") }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            // Footer mit Versionsnummer und Link zum GitHub-Repository
            SettingsFooter(versionName) {
                context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/Quiter/BirthdayBuddy".toUri()))
            }
        }
    }

    // Dialog zur Auswahl der Anzahl der im Widget anzuzeigenden Personen
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

/**
 * Bildschirm zur Konfiguration der Alarmzeiten.
 * Nutzer können die Uhrzeit und die Tage vor dem Geburtstag (Vorlaufzeit) einstellen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(filterManager: FilterManager, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val notifHour by filterManager.notificationHourFlow.collectAsState(initial = 9)
    val notifMinute by filterManager.notificationMinuteFlow.collectAsState(initial = 0)
    val notifDaysSet by filterManager.notificationDaysFlow.collectAsState(initial = setOf("0", "7"))

    var showAddDayDialog by remember { mutableStateOf(false) }
    var selectedDayByWheel by remember { mutableIntStateOf(3) }
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SectionHeader("Uhrzeit")
            // Zeigt die aktuell eingestellte Zeit an und öffnet bei Klick den Picker
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
                    // Formatiert die Anzeige der Tage (z.B. "Am Tag", "1 Tag vorher", "X Wochen vorher")
                    val text = when {
                        day == 0 -> "Am Tag des Geburtstags"
                        day == 1 -> "1 Tag vorher"
                        day % 7 == 0 -> {
                            val weeks = day / 7
                            if (weeks == 1) "1 Woche vorher" else "$weeks Wochen vorher"
                        }
                        else -> "$day Tage vorher"
                    }
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

    // Material 3 TimePicker Dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Erinnerungszeit wählen") },
            text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timePickerState) } },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        filterManager.saveNotificationTime(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                }) { Text("Speichern") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Abbrechen") } }
        )
    }

    // Dialog zum Hinzufügen einer neuen Vorlaufzeit (Tage vorher)
    if (showAddDayDialog) {
        AlertDialog(
            onDismissRequest = { showAddDayDialog = false },
            title = { Text("Vorlaufzeit wählen") },
            text = {
                WheelPicker(
                    range = (0..30).toList(),
                    initialValue = selectedDayByWheel,
                    onValueChange = { selectedDayByWheel = it }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newSet = notifDaysSet.toMutableSet()
                    newSet.add(selectedDayByWheel.toString())
                    scope.launch { filterManager.saveNotificationDays(newSet) }
                    showAddDayDialog = false
                }) { Text("Hinzufügen") }
            },
            dismissButton = { TextButton(onClick = { showAddDayDialog = false }) { Text("Abbrechen") } }
        )
    }
}

/**
 * Filter-Bildschirm: Labels komplett aus der App blockieren.
 */
@Composable fun BlockLabelsScreen(f: FilterManager, a: Set<String>, l: Boolean, b: () -> Unit) {
    val labels by f.excludedLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    LabelSelectionScreen("Blockieren", "Diese Kontakte werden komplett ausgeblendet.", a, labels, l, { label, checked ->
        val newSet = labels.toMutableSet()
        if (checked) newSet.add(label) else newSet.remove(label)
        scope.launch { f.saveExcludedLabels(newSet) }
    }, b)
}

/**
 * Filter-Bildschirm: Labels im Seitenmenü (Drawer) ein-/ausblenden.
 */
@Composable fun HideLabelsScreen(f: FilterManager, a: Set<String>, l: Boolean, b: () -> Unit) {
    val selectedLabels by f.selectedLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    LabelSelectionScreen("Anzeigen", "Diese Labels im Seitenmenü anzeigen.", a, selectedLabels, l, { label, checked ->
        val newSet = selectedLabels.toMutableSet()
        if (checked) newSet.add(label) else newSet.remove(label)
        scope.launch { f.saveSelectedLabels(newSet) }
    }, b)
}

/**
 * Filter-Bildschirm: Labels festlegen, die im Widget erscheinen sollen (Whitelist).
 */
@Composable fun WidgetIncludeLabelsScreen(f: FilterManager, a: Set<String>, l: Boolean, b: () -> Unit) {
    val context = LocalContext.current
    val selectedLabels by f.widgetSelectedLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    LabelSelectionScreen("Anzeigen", "Diese Labels im Widget anzeigen.", a, selectedLabels, l, { label, checked ->
        val newSet = selectedLabels.toMutableSet()
        if (checked) newSet.add(label) else newSet.remove(label)
        scope.launch { 
            f.saveWidgetSelectedLabels(newSet)
            updateWidget(context)
        }
    }, b)
}

/**
 * Filter-Bildschirm: Labels festlegen, die im Widget ignoriert werden sollen (Blacklist).
 */
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
