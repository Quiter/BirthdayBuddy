package com.heckmannch.birthdaybuddy

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.utils.FilterManager
import com.heckmannch.birthdaybuddy.utils.updateWidget
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMenuScreen(onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val filterManager = remember { FilterManager(context) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showCountDialog by remember { mutableStateOf(false) }
    val widgetCount by filterManager.widgetItemCountFlow.collectAsState(initial = 1)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Benachrichtigungen",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
            )
            SettingsBlock("Alarme", "Erinnerungszeiten konfigurieren", Icons.Default.DateRange, MaterialTheme.colorScheme.primaryContainer) { onNavigate("settings_alarms") }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Filter & Sichtbarkeit",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column {
                    SettingsBlockRow("Blockieren", "Labels komplett ignorieren", Icons.Default.Clear, MaterialTheme.colorScheme.errorContainer) { onNavigate("settings_block") }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsBlockRow("Menü", "Labels im Drawer verstecken", Icons.Default.Lock, MaterialTheme.colorScheme.secondaryContainer) { onNavigate("settings_hide") }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Widget",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column {
                    SettingsBlockRow("Anzahl", "Bis zu $widgetCount Personen", Icons.Default.List, MaterialTheme.colorScheme.tertiaryContainer) { showCountDialog = true }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsBlockRow("Anzeigen", "Nur diese Labels nutzen", Icons.Default.Done, MaterialTheme.colorScheme.tertiaryContainer) { onNavigate("settings_widget_include") }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsBlockRow("Blockieren", "Diese Labels ausschließen", Icons.Default.Clear, MaterialTheme.colorScheme.tertiaryContainer) { onNavigate("settings_widget_exclude") }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Version 0.6", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Quiter/BirthdayBuddy"))) }) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Projekt auf GitHub")
                }
            }
        }
    }

    if (showCountDialog) {
        AlertDialog(
            onDismissRequest = { showCountDialog = false },
            title = { Text("Widget Kapazität") },
            text = {
                Column {
                    listOf(1, 2, 3).forEach { count ->
                        ListItem(
                            headlineContent = { Text("$count ${if (count == 1) "Person" else "Personen"}") },
                            leadingContent = { RadioButton(selected = widgetCount == count, onClick = null) },
                            modifier = Modifier.clickable {
                                scope.launch {
                                    filterManager.saveWidgetItemCount(count)
                                    updateWidget(context)
                                }
                                showCountDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCountDialog = false }) { Text("Fertig") } }
        )
    }
}

@Composable
fun SettingsBlock(title: String, subtitle: String, icon: ImageVector, containerColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        SettingsBlockRow(title, subtitle, icon, containerColor, onClick = onClick)
    }
}

@Composable
fun SettingsBlockRow(title: String, subtitle: String, icon: ImageVector, containerColor: Color, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Box(
                modifier = Modifier.size(40.dp).background(containerColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelSelectionScreen(title: String, description: String, availableLabels: Set<String>, activeLabels: Set<String>, isLoading: Boolean, onToggle: (String, Boolean) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(availableLabels.toList()) { label ->
                        ListItem(
                            headlineContent = { Text(label) },
                            trailingContent = { Switch(checked = activeLabels.contains(label), onCheckedChange = { onToggle(label, it) }) }
                        )
                    }
                }
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

    // NEU: Steuert, ob die große Uhr sichtbar ist
    var showTimePicker by remember { mutableStateOf(false) }
    // NEU: Merkt sich den Zustand der Material 3 Uhr
    val timePickerState = rememberTimePickerState(
        initialHour = notifHour,
        initialMinute = notifMinute,
        is24Hour = true
    )

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
            Text("Uhrzeit", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
            ListItem(
                headlineContent = { Text("Standard-Uhrzeit") },
                supportingContent = { Text(String.format(Locale.getDefault(), "%02d:%02d Uhr", notifHour, notifMinute)) },
                leadingContent = { Icon(Icons.Default.DateRange, null) },
                // HIER: Wir öffnen nicht mehr den alten Dialog, sondern setzen unsere Variable auf true
                modifier = Modifier.clickable { showTimePicker = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Vorlaufzeiten", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)

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

    // --- NEU: DER MODERNE MATERIAL 3 TIME PICKER ---
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Erinnerungszeit wählen") },
            text = {
                // Die Uhr wird zentriert in den Dialog gesetzt
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    // Zeit absolut kugelsicher speichern!
                    scope.launch {
                        filterManager.saveNotificationTime(timePickerState.hour, timePickerState.minute)
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // --- DEIN ALTER DIALOG FÜR DIE TAGE BLEIBT GENAU GLEICH ---
    if (showAddDayDialog) {
        AlertDialog(
            onDismissRequest = { showAddDayDialog = false },
            title = { Text("Vorlaufzeit hinzufügen") },
            text = {
                OutlinedTextField(
                    value = newDayInput,
                    onValueChange = { if (it.all { char -> char.isDigit() }) newDayInput = it },
                    label = { Text("Tage vorher") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    newDayInput.toIntOrNull()?.let {
                        val newSet = notifDaysSet.toMutableSet()
                        newSet.add(it.toString())
                        scope.launch { filterManager.saveNotificationDays(newSet) }
                    }
                    newDayInput = ""
                    showAddDayDialog = false
                }) { Text("Speichern") }
            },
            dismissButton = { TextButton(onClick = { showAddDayDialog = false }) { Text("Abbrechen") } }
        )
    }
}

// Wrapper-Funktionen für die Navigation erhalten (falls benötigt)
@Composable fun BlockLabelsScreen(f: FilterManager, a: Set<String>, l: Boolean, b: () -> Unit) = BlockLabelsScreenImpl(f, a, l, b)
@Composable fun HideLabelsScreen(f: FilterManager, a: Set<String>, l: Boolean, b: () -> Unit) = HideLabelsScreenImpl(f, a, l, b)
@Composable fun WidgetIncludeLabelsScreen(f: FilterManager, a: Set<String>, l: Boolean, b: () -> Unit) = WidgetIncludeLabelsScreenImpl(f, a, l, b)
@Composable fun WidgetExcludeLabelsScreen(f: FilterManager, a: Set<String>, l: Boolean, b: () -> Unit) = WidgetExcludeLabelsScreenImpl(f, a, l, b)

@Composable private fun BlockLabelsScreenImpl(filterManager: FilterManager, availableLabels: Set<String>, isLoading: Boolean, onBack: () -> Unit) {
    val excludedLabels by filterManager.excludedLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    LabelSelectionScreen("Blockieren", "Diese Kontakte werden komplett ausgeblendet.", availableLabels, excludedLabels, isLoading, { label, isChecked ->
        val newSet = excludedLabels.toMutableSet()
        if (isChecked) newSet.add(label) else newSet.remove(label)
        scope.launch { filterManager.saveExcludedLabels(newSet) }
    }, onBack)
}

@Composable private fun HideLabelsScreenImpl(filterManager: FilterManager, availableLabels: Set<String>, isLoading: Boolean, onBack: () -> Unit) {
    val hiddenLabels by filterManager.hiddenDrawerLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    LabelSelectionScreen("Menü", "Diese Labels im Seitenmenü verstecken.", availableLabels, hiddenLabels, isLoading, { label, isChecked ->
        val newSet = hiddenLabels.toMutableSet()
        if (isChecked) newSet.add(label) else newSet.remove(label)
        scope.launch { filterManager.saveHiddenDrawerLabels(newSet) }
    }, onBack)
}

@Composable private fun WidgetIncludeLabelsScreenImpl(filterManager: FilterManager, availableLabels: Set<String>, isLoading: Boolean, onBack: () -> Unit) {
    val includedLabels by filterManager.widgetIncludedLabelsFlow.collectAsState(initial = setOf("ALL_DEFAULT"))
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activeLabels = if (includedLabels.contains("ALL_DEFAULT")) availableLabels else includedLabels
    LabelSelectionScreen("Widget: Anzeigen", "Nur diese Labels im Widget zeigen.", availableLabels, activeLabels, isLoading, { label, isChecked ->
        val newSet = activeLabels.toMutableSet()
        if (isChecked) newSet.add(label) else newSet.remove(label)
        scope.launch { filterManager.saveWidgetIncludedLabels(newSet) }
        updateWidget(context)
    }, onBack)
}

@Composable private fun WidgetExcludeLabelsScreenImpl(filterManager: FilterManager, availableLabels: Set<String>, isLoading: Boolean, onBack: () -> Unit) {
    val excludedLabels by filterManager.widgetExcludedLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    LabelSelectionScreen("Widget: Blockieren", "Diese Labels im Widget immer ignorieren.", availableLabels, excludedLabels, isLoading, { label, isChecked ->
        val newSet = excludedLabels.toMutableSet()
        if (isChecked) newSet.add(label) else newSet.remove(label)
        scope.launch { filterManager.saveWidgetExcludedLabels(newSet) }
        updateWidget(context)
    }, onBack)
}
