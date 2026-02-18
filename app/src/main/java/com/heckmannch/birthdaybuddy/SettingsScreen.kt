package com.heckmannch.birthdaybuddy

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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

    val widgetCount by filterManager.widgetItemCountFlow.collectAsState(initial = 1)
    var showCountDialog by remember { mutableStateOf(false) }

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.6"
        } catch (e: Exception) { "0.6" }
    }

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
            SectionHeader("Benachrichtigungen")
            SettingsBlock("Alarme", "Erinnerungszeiten konfigurieren", Icons.Default.Notifications, Color(0xFF4CAF50)) { onNavigate("settings_alarms") }
            
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader("Filter & Sichtbarkeit")
            SettingsCard {
                val menuOrange = Color(0xFFFF9800)
                SettingsBlockRow("Anzeigen", "Labels im Drawer verstecken", Icons.Default.Visibility, menuOrange) { onNavigate("settings_hide") }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingsBlockRow("Blockieren", "Labels komplett ignorieren", Icons.Default.VisibilityOff, menuOrange) { onNavigate("settings_block") }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader("Widget")
            SettingsCard {
                val widgetBlue = Color(0xFF2196F3)
                SettingsBlockRow("Anzahl", "Bis zu $widgetCount Personen", Icons.Default.List, widgetBlue) { showCountDialog = true }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingsBlockRow("Anzeigen", "Nur diese Labels nutzen", Icons.Default.Visibility, widgetBlue) { onNavigate("settings_widget_include") }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingsBlockRow("Blockieren", "Diese Labels ausschließen", Icons.Default.VisibilityOff, widgetBlue) { onNavigate("settings_widget_exclude") }
            }

            Spacer(modifier = Modifier.weight(1f))
            Footer(versionName) {
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

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        content = content
    )
}

@Composable
fun SettingsBlock(title: String, subtitle: String, icon: ImageVector, containerColor: Color, onClick: () -> Unit) {
    SettingsCard {
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
                Icon(icon, null, modifier = Modifier.size(20.dp), tint = Color.White)
            }
        },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun Footer(versionName: String, onGithubClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Version $versionName", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onGithubClick) {
            Icon(painter = painterResource(id = R.drawable.ic_github), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Projekt auf GitHub")
        }
    }
}

@Composable
private fun WidgetCountDialog(currentCount: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Widget Kapazität") },
        text = {
            Column {
                listOf(1, 2, 3).forEach { count ->
                    ListItem(
                        headlineContent = { Text("$count ${if (count == 1) "Person" else "Personen"}") },
                        leadingContent = { RadioButton(selected = currentCount == count, onClick = null) },
                        modifier = Modifier.clickable { onSelect(count) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fertig") } }
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
            ListItem(
                headlineContent = { Text("Standard-Uhrzeit") },
                supportingContent = { Text(String.format(Locale.getDefault(), "%02d:%02d Uhr", notifHour, notifMinute)) },
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
    val labels by f.hiddenDrawerLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    LabelSelectionScreen("Anzeigen", "Diese Labels im Seitenmenü verstecken.", a, labels, l, { label, checked ->
        val newSet = labels.toMutableSet()
        if (checked) newSet.add(label) else newSet.remove(label)
        scope.launch { f.saveHiddenDrawerLabels(newSet) }
    }, b)
}

@Composable fun WidgetIncludeLabelsScreen(f: FilterManager, a: Set<String>, l: Boolean, b: () -> Unit) {
    val context = LocalContext.current
    val labels by f.widgetIncludedLabelsFlow.collectAsState(initial = setOf("ALL_DEFAULT"))
    val scope = rememberCoroutineScope()
    val activeLabels = if (labels.contains("ALL_DEFAULT")) a else labels
    LabelSelectionScreen("Widget: Anzeigen", "Nur diese Labels im Widget zeigen.", a, activeLabels, l, { label, checked ->
        val newSet = activeLabels.toMutableSet()
        if (checked) newSet.add(label) else newSet.remove(label)
        scope.launch { f.saveWidgetIncludedLabels(newSet) }
        updateWidget(context)
    }, b)
}

@Composable fun WidgetExcludeLabelsScreen(f: FilterManager, a: Set<String>, l: Boolean, b: () -> Unit) {
    val context = LocalContext.current
    val labels by f.widgetExcludedLabelsFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    LabelSelectionScreen("Widget: Blockieren", "Diese Labels im Widget immer ignorieren.", a, labels, l, { label, checked ->
        val newSet = labels.toMutableSet()
        if (checked) newSet.add(label) else newSet.remove(label)
        scope.launch { f.saveWidgetExcludedLabels(newSet) }
        updateWidget(context)
    }, b)
}
