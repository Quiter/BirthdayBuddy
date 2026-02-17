package com.heckmannch.birthdaybuddy

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.utils.FilterManager
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    filterManager: FilterManager,
    availableLabels: Set<String>,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current // Wichtig für den TimePicker

    // Listen aus dem Speicher laden
    val excludedLabels by filterManager.excludedLabelsFlow.collectAsState(initial = emptySet())
    val hiddenDrawerLabels by filterManager.hiddenDrawerLabelsFlow.collectAsState(initial = emptySet())

    // NEU: Benachrichtigungs-Einstellungen laden
    val notifHour by filterManager.notificationHourFlow.collectAsState(initial = 9)
    val notifMinute by filterManager.notificationMinuteFlow.collectAsState(initial = 0)
    val notifDaysSet by filterManager.notificationDaysFlow.collectAsState(initial = setOf("0", "7"))

    // Zustand für die Reiter (Tabs) - jetzt 3 Tabs!
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Blockieren", "Menü", "Alarme") // Kurznamen, damit es auf schmale Bildschirme passt

    // State für den Dialog zum Hinzufügen von Tagen
    var showAddDayDialog by remember { mutableStateOf(false) }
    var newDayInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        },
        // Ein Floating Action Button unten rechts für den Tab "Alarme"
        floatingActionButton = {
            if (selectedTabIndex == 2) {
                FloatingActionButton(onClick = { showAddDayDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Tag hinzufügen")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding()
                )
        ) {

            // Die Reiter (Tabs)
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            if (selectedTabIndex == 0 || selectedTabIndex == 1) {
                // TAB 1 & 2: Die bisherigen Filter
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        val title = if (selectedTabIndex == 0) "Labels komplett ausschließen" else "Labels im Drawer verstecken"
                        val desc = if (selectedTabIndex == 0)
                            "Kontakte mit diesen Labels werden ignoriert und nirgendwo angezeigt."
                        else
                            "Diese Labels tauchen im linken Filter-Menü nicht mehr auf, die Kontakte bleiben aber erhalten."

                        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                        Text(desc, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
                    }

                    items(availableLabels.toList()) { label ->
                        val isChecked = if (selectedTabIndex == 0) excludedLabels.contains(label) else hiddenDrawerLabels.contains(label)

                        ListItem(
                            headlineContent = { Text(label) },
                            trailingContent = {
                                Switch(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (selectedTabIndex == 0) {
                                            val newSet = excludedLabels.toMutableSet()
                                            if (checked) newSet.add(label) else newSet.remove(label)
                                            scope.launch { filterManager.saveExcludedLabels(newSet) }
                                        } else {
                                            val newSet = hiddenDrawerLabels.toMutableSet()
                                            if (checked) newSet.add(label) else newSet.remove(label)
                                            scope.launch { filterManager.saveHiddenDrawerLabels(newSet) }
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            } else {
                // TAB 3: NEU - BENACHRICHTIGUNGEN
                Column(modifier = Modifier.fillMaxSize()) {
                    Text("Uhrzeit für alle Alarme", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))

                    // Klickbare Reihe für den TimePicker
                    ListItem(
                        headlineContent = { Text("Standard-Uhrzeit") },
                        supportingContent = {
                            // Formatiert es schön als "09:00 Uhr"
                            val timeString = String.format(Locale.getDefault(), "%02d:%02d Uhr", notifHour, notifMinute)
                            Text(timeString)
                        },
                        modifier = Modifier.clickable {
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    scope.launch { filterManager.saveNotificationTime(hourOfDay, minute) }
                                },
                                notifHour,
                                notifMinute,
                                true // 24-Stunden Format
                            ).show()
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("Vorlaufzeiten", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                    Text("An diesen Tagen vor dem Geburtstag wirst du erinnert:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
                    // Liste holen, in Zahlen umwandeln und AUFSTEIGEND sortieren
                    val sortedDays = notifDaysSet.mapNotNull { it.toIntOrNull() }.sorted()

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(sortedDays) { day ->
                            // Schön lesbare Texte
                            val text = when (day) {
                                0 -> "Am Tag des Geburtstags"
                                1 -> "1 Tag vorher"
                                7 -> "1 Woche vorher (7 Tage)"
                                else -> "$day Tage vorher"
                            }

                            ListItem(
                                headlineContent = { Text(text) },
                                trailingContent = {
                                    IconButton(onClick = {
                                        val newSet = notifDaysSet.toMutableSet()
                                        newSet.remove(day.toString())
                                        scope.launch { filterManager.saveNotificationDays(newSet) }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Das PopUp, wenn man auf den "+" Button drückt
    if (showAddDayDialog) {
        AlertDialog(
            onDismissRequest = { showAddDayDialog = false },
            title = { Text("Vorlaufzeit hinzufügen") },
            text = {
                OutlinedTextField(
                    value = newDayInput,
                    onValueChange = { newValue ->
                        // Wir erlauben nur Zahlen!
                        if (newValue.all { it.isDigit() }) {
                            newDayInput = newValue
                        }
                    },
                    label = { Text("Tage vorher (z.B. 3)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newDay = newDayInput.toIntOrNull()
                    if (newDay != null) {
                        val newSet = notifDaysSet.toMutableSet()
                        newSet.add(newDay.toString())
                        scope.launch { filterManager.saveNotificationDays(newSet) }
                    }
                    newDayInput = ""
                    showAddDayDialog = false
                }) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    newDayInput = ""
                    showAddDayDialog = false
                }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}