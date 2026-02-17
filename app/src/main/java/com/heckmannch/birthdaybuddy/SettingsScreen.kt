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
import androidx.compose.material.icons.filled.Info
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
import kotlinx.coroutines.launch
import java.util.Locale

// --- 1. DAS HAUPTMENÜ ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMenuScreen(onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current

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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Alarme (Ein einzelner, großer Block - Icon: Grün)
            SettingsBlock(
                title = "Alarme",
                subtitle = "Benachrichtigungen einrichten",
                icon = Icons.Default.DateRange, // Kalenderblatt
                iconBgColor = Color(0xFF4CAF50), // Schickes Material-Grün
                onClick = { onNavigate("settings_alarms") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Filter & Sichtbarkeit (Zusammengefasst in EINER Karte - Icons: Rot)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column {
                    SettingsBlockRow(
                        title = "Blockieren",
                        subtitle = "Labels komplett ausschließen",
                        icon = Icons.Default.Clear, // Großes X
                        iconBgColor = Color(0xFFE53935), // Schickes Material-Rot
                        onClick = { onNavigate("settings_block") }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )

                    SettingsBlockRow(
                        title = "Menü",
                        subtitle = "Labels im Drawer verstecken",
                        icon = Icons.Default.Lock, // Schloss
                        iconBgColor = Color(0xFFE53935), // Schickes Material-Rot
                        onClick = { onNavigate("settings_hide") }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- DER FOOTER ---
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("Entwickelt von heckmannch", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/heckmannch/BirthdayBuddy"))
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.Info, contentDescription = "GitHub")
                    Spacer(Modifier.width(8.dp))
                    Text("Projekt auf GitHub ansehen")
                }
            }
        }
    }
}

// --- HELFER FÜR DAS ANDROID-12+ DESIGN ---
@Composable
fun SettingsBlock(
    title: String, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color, onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        onClick = onClick
    ) {
        SettingsBlockRow(title, subtitle, icon, iconBgColor, onClick = onClick)
    }
}

@Composable
fun SettingsBlockRow(
    title: String, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color, onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(iconBgColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}

// --- 2. UNTERSEITE: BLOCKIEREN ---
@Composable
fun BlockLabelsScreen(filterManager: FilterManager, availableLabels: Set<String>, isLoading: Boolean, onBack: () -> Unit) {
    val excludedLabels by filterManager.excludedLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    LabelSelectionScreen(
        title = "Blockieren",
        description = "Kontakte mit diesen Labels werden ignoriert und nirgendwo angezeigt.",
        availableLabels = availableLabels,
        activeLabels = excludedLabels,
        isLoading = isLoading, // Reichen wir weiter
        onToggle = { label, isChecked ->
            val newSet = excludedLabels.toMutableSet()
            if (isChecked) newSet.add(label) else newSet.remove(label)
            scope.launch { filterManager.saveExcludedLabels(newSet) }
        },
        onBack = onBack
    )
}

// --- 3. UNTERSEITE: VERSTECKEN ---
@Composable
fun HideLabelsScreen(filterManager: FilterManager, availableLabels: Set<String>, isLoading: Boolean, onBack: () -> Unit) {
    val hiddenLabels by filterManager.hiddenDrawerLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    LabelSelectionScreen(
        title = "Menü-Sichtbarkeit",
        description = "Diese Labels tauchen im linken Filter-Menü nicht auf, die Kontakte bleiben aber erhalten.",
        availableLabels = availableLabels,
        activeLabels = hiddenLabels,
        isLoading = isLoading, // Reichen wir weiter
        onToggle = { label, isChecked ->
            val newSet = hiddenLabels.toMutableSet()
            if (isChecked) newSet.add(label) else newSet.remove(label)
            scope.launch { filterManager.saveHiddenDrawerLabels(newSet) }
        },
        onBack = onBack
    )
}

// --- GENERISCHE VORLAGE FÜR LABEL-LISTEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelSelectionScreen(
    title: String, description: String, availableLabels: Set<String>, activeLabels: Set<String>,
    isLoading: Boolean, // NEU: Wir prüfen hier, ob wir noch laden
    onToggle: (String, Boolean) -> Unit, onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))

            // Magie: Wenn wir laden, zeige den Kreis. Wenn nicht, zeige die Liste!
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator() // Der wunderschöne Android-Ladekreis
                }
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

// --- 4. UNTERSEITE: ALARME ---
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
        topBar = {
            TopAppBar(
                title = { Text("Alarme") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDayDialog = true }) { Icon(Icons.Default.Add, "Hinzufügen") }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text("Uhrzeit für alle Alarme", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))

            ListItem(
                headlineContent = { Text("Standard-Uhrzeit") },
                supportingContent = { Text(String.format(Locale.getDefault(), "%02d:%02d Uhr", notifHour, notifMinute)) },
                modifier = Modifier.clickable {
                    TimePickerDialog(context, { _, h, m -> scope.launch { filterManager.saveNotificationTime(h, m) } }, notifHour, notifMinute, true).show()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Vorlaufzeiten", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))

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

    if (showAddDayDialog) {
        AlertDialog(
            onDismissRequest = { showAddDayDialog = false },
            title = { Text("Vorlaufzeit hinzufügen") },
            text = {
                OutlinedTextField(
                    value = newDayInput,
                    onValueChange = { if (it.all { char -> char.isDigit() }) newDayInput = it },
                    label = { Text("Tage vorher (z.B. 3)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    newDayInput.toIntOrNull()?.let {
                        val newSet = notifDaysSet.toMutableSet()
                        newSet.add(it.toString())
                        scope.launch { filterManager.saveNotificationDays(newSet) }
                    }
                    newDayInput = ""; showAddDayDialog = false
                }) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(onClick = { newDayInput = ""; showAddDayDialog = false }) { Text("Abbrechen") }
            }
        )
    }
}