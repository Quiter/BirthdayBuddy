package com.heckmannch.birthdaybuddy.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.components.SectionHeader
import com.heckmannch.birthdaybuddy.components.WheelPicker
import com.heckmannch.birthdaybuddy.utils.FilterManager
import com.heckmannch.birthdaybuddy.utils.scheduleDailyBirthdayWork
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(filterManager: FilterManager, onBack: () -> Unit) {
    val context = LocalContext.current
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
                                scope.launch { 
                                    filterManager.saveNotificationDays(newSet)
                                    scheduleDailyBirthdayWork(context, notifHour, notifMinute)
                                }
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
                        scheduleDailyBirthdayWork(context, timePickerState.hour, timePickerState.minute)
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
                    scope.launch { 
                        filterManager.saveNotificationDays(newSet)
                        scheduleDailyBirthdayWork(context, notifHour, notifMinute)
                    }
                    showAddDayDialog = false
                }) { Text("Hinzufügen") }
            },
            dismissButton = { TextButton(onClick = { showAddDayDialog = false }) { Text("Abbrechen") } }
        )
    }
}
