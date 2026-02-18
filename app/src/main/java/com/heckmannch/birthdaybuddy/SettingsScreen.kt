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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

    // Status für das Widget-PopUp
    var showCountDialog by remember { mutableStateOf(false) }
    val widgetCount by filterManager.widgetItemCountFlow.collectAsState(initial = 1)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Einstellungen") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Alarme
            SettingsBlock("Alarme", "Benachrichtigungen einrichten", Icons.Default.DateRange, Color(0xFF4CAF50)) { onNavigate("settings_alarms") }
            Spacer(modifier = Modifier.height(24.dp))

            // 2. Filter & Sichtbarkeit
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column {
                    SettingsBlockRow("Blockieren", "Labels komplett ausschließen", Icons.Default.Clear, Color(0xFFE53935)) { onNavigate("settings_block") }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    SettingsBlockRow("Menü", "Labels im Drawer verstecken", Icons.Default.Lock, Color(0xFFE53935)) { onNavigate("settings_hide") }
                }
            }

            // 3. Widget (Jetzt mit Anzahl-Option!)
            Spacer(modifier = Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column {
                    SettingsBlockRow("Widget: Anzahl", "Zeigt bis zu $widgetCount Personen", Icons.Default.List, Color(0xFF2196F3)) { showCountDialog = true }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    SettingsBlockRow("Widget: Anzeigen", "Nur diese Labels zeigen", Icons.Default.Done, Color(0xFF2196F3)) { onNavigate("settings_widget_include") }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    SettingsBlockRow("Widget: Blockieren", "Diese Labels verstecken", Icons.Default.Clear, Color(0xFF2196F3)) { onNavigate("settings_widget_exclude") }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("Entwickelt von heckmannch", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Quiter/BirthdayBuddy"))) }) {
                    Icon(Icons.Default.Info, "GitHub")
                    Spacer(Modifier.width(8.dp))
                    Text("Projekt auf GitHub ansehen")
                }
            }
        }
    }

    // Das PopUp für die Widget-Anzahl
    if (showCountDialog) {
        AlertDialog(
            onDismissRequest = { showCountDialog = false },
            title = { Text("Anzahl Geburtstage") },
            text = {
                Column {
                    listOf(1, 2, 3).forEach { count ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                scope.launch {
                                    filterManager.saveWidgetItemCount(count)
                                    updateWidget(context) // Widget live updaten!
                                }
                                showCountDialog = false
                            }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = widgetCount == count, onClick = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("$count ${if (count == 1) "Person" else "Personen"}")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCountDialog = false }) { Text("Abbrechen") } }
        )
    }
}

// --- HELFER FÜR DESIGN ---
@Composable
fun SettingsBlock(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconBgColor: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), onClick = onClick) {
        SettingsBlockRow(title, subtitle, icon, iconBgColor, onClick = {})
    }
}

@Composable
fun SettingsBlockRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconBgColor: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).background(iconBgColor, shape = CircleShape), contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = null, tint = Color.White) }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}

// --- BESTEHENDE SCREENS ---
@Composable
fun BlockLabelsScreen(filterManager: FilterManager, availableLabels: Set<String>, isLoading: Boolean, onBack: () -> Unit) {
    val excludedLabels by filterManager.excludedLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    LabelSelectionScreen("Blockieren", "Kontakte mit diesen Labels werden ignoriert.", availableLabels, excludedLabels, isLoading, { label, isChecked ->
        val newSet = excludedLabels.toMutableSet()
        if (isChecked) newSet.add(label) else newSet.remove(label)
        scope.launch { filterManager.saveExcludedLabels(newSet) }
    }, onBack)
}

@Composable
fun HideLabelsScreen(filterManager: FilterManager, availableLabels: Set<String>, isLoading: Boolean, onBack: () -> Unit) {
    val hiddenLabels by filterManager.hiddenDrawerLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    LabelSelectionScreen("Menü-Sichtbarkeit", "Diese Labels tauchen im Menü nicht auf.", availableLabels, hiddenLabels, isLoading, { label, isChecked ->
        val newSet = hiddenLabels.toMutableSet()
        if (isChecked) newSet.add(label) else newSet.remove(label)
        scope.launch { filterManager.saveHiddenDrawerLabels(newSet) }
    }, onBack)
}

// --- WIDGET SCREENS ---
@Composable
fun WidgetIncludeLabelsScreen(filterManager: FilterManager, availableLabels: Set<String>, isLoading: Boolean, onBack: () -> Unit) {
    val includedLabels by filterManager.widgetIncludedLabelsFlow.collectAsState(initial = setOf("ALL_DEFAULT"))
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activeLabels = if (includedLabels.contains("ALL_DEFAULT")) availableLabels else includedLabels

    LabelSelectionScreen("Widget: Anzeigen", "Nur Kontakte mit diesen Labels erscheinen im Widget.", availableLabels, activeLabels, isLoading, { label, isChecked ->
        val newSet = activeLabels.toMutableSet()
        if (isChecked) newSet.add(label) else newSet.remove(label)
        scope.launch {
            filterManager.saveWidgetIncludedLabels(newSet)
            updateWidget(context)
        }
    }, onBack)
}

@Composable
fun WidgetExcludeLabelsScreen(filterManager: FilterManager, availableLabels: Set<String>, isLoading: Boolean, onBack: () -> Unit) {
    val excludedLabels by filterManager.widgetExcludedLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LabelSelectionScreen("Widget: Blockieren", "Diese Labels werden im Widget immer ignoriert.", availableLabels, excludedLabels, isLoading, { label, isChecked ->
        val newSet = excludedLabels.toMutableSet()
        if (isChecked) newSet.add(label) else newSet.remove(label)
        scope.launch {
            filterManager.saveWidgetExcludedLabels(newSet)
            updateWidget(context)
        }
    }, onBack)
}

// --- GENERISCHE VORLAGE ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelSelectionScreen(title: String, description: String, availableLabels: Set<String>, activeLabels: Set<String>, isLoading: Boolean, onToggle: (String, Boolean) -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(availableLabels.toList()) { label ->
                        ListItem(headlineContent = { Text(label) }, trailingContent = { Switch(checked = activeLabels.contains(label), onCheckedChange = { onToggle(label, it) }) })
                    }
                }
            }
        }
    }
}

// --- ALARME ---
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Alarme") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } }) },
        floatingActionButton = { FloatingActionButton(onClick = { showAddDayDialog = true }) { Icon(Icons.Default.Add, "Hinzufügen") } }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text("Uhrzeit für alle Alarme", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            ListItem(headlineContent = { Text("Standard-Uhrzeit") }, supportingContent = { Text(String.format(Locale.getDefault(), "%02d:%02d Uhr", notifHour, notifMinute)) }, modifier = Modifier.clickable { TimePickerDialog(context, { _, h, m -> scope.launch { filterManager.saveNotificationTime(h, m) } }, notifHour, notifMinute, true).show() })
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Vorlaufzeiten", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            val sortedDays = notifDaysSet.mapNotNull { it.toIntOrNull() }.sorted()
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sortedDays) { day ->
                    val text = when (day) { 0 -> "Am Tag des Geburtstags"; 1 -> "1 Tag vorher"; 7 -> "1 Woche vorher"; else -> "$day Tage vorher" }
                    ListItem(headlineContent = { Text(text) }, trailingContent = { IconButton(onClick = { val newSet = notifDaysSet.toMutableSet(); newSet.remove(day.toString()); scope.launch { filterManager.saveNotificationDays(newSet) } }) { Icon(Icons.Default.Delete, "Löschen", tint = MaterialTheme.colorScheme.error) } })
                }
            }
        }
    }
    if (showAddDayDialog) {
        AlertDialog(onDismissRequest = { showAddDayDialog = false }, title = { Text("Vorlaufzeit hinzufügen") }, text = { OutlinedTextField(value = newDayInput, onValueChange = { if (it.all { char -> char.isDigit() }) newDayInput = it }, label = { Text("Tage vorher (z.B. 3)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true) }, confirmButton = { TextButton(onClick = { newDayInput.toIntOrNull()?.let { val newSet = notifDaysSet.toMutableSet(); newSet.add(it.toString()); scope.launch { filterManager.saveNotificationDays(newSet) } }; newDayInput = ""; showAddDayDialog = false }) { Text("Speichern") } }, dismissButton = { TextButton(onClick = { newDayInput = ""; showAddDayDialog = false }) { Text("Abbrechen") } })
    }
}