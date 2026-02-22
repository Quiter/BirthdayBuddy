package com.heckmannch.birthdaybuddy.ui.screens

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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.SectionHeader
import com.heckmannch.birthdaybuddy.ui.components.WheelPicker
import com.heckmannch.birthdaybuddy.data.FilterManager
import com.heckmannch.birthdaybuddy.utils.scheduleDailyBirthdayWork
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAlarmsScreen(
    onBack: () -> Unit,
    filterManager: FilterManager = hiltViewModel<AlarmsViewModel>().filterManager
) {
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
                title = { Text(stringResource(R.string.settings_alarms_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.label_selection_back)) } },
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.alarms_add_button)) },
                icon = { Icon(Icons.Default.Add, null) },
                onClick = { showAddDayDialog = true }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SectionHeader(stringResource(R.string.alarms_time_section))
            ListItem(
                headlineContent = { Text(stringResource(R.string.alarms_default_time_label)) },
                supportingContent = { 
                    Text(stringResource(R.string.alarms_time_display, notifHour, notifMinute)) 
                },
                leadingContent = { Icon(Icons.Default.Notifications, null) },
                modifier = Modifier.clickable { showTimePicker = true }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionHeader(stringResource(R.string.alarms_lead_times_section))
            val sortedDays = notifDaysSet.mapNotNull { it.toIntOrNull() }.sorted()
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sortedDays) { day ->
                    val text = when {
                        day == 0 -> stringResource(R.string.alarms_day_0)
                        day == 1 -> stringResource(R.string.alarms_day_1)
                        day % 7 == 0 -> {
                            val weeks = day / 7
                            pluralStringResource(R.plurals.alarms_weeks_before, weeks, weeks)
                        }
                        else -> pluralStringResource(R.plurals.alarms_days_before, day, day)
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
                            }) { Icon(Icons.Default.Delete, stringResource(R.string.alarms_delete_description), tint = MaterialTheme.colorScheme.error) }
                        }
                    )
                }
            }
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.alarms_dialog_time_title)) },
            text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timePickerState) } },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        filterManager.saveNotificationTime(timePickerState.hour, timePickerState.minute)
                        scheduleDailyBirthdayWork(context, timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                }) { Text(stringResource(R.string.dialog_save)) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.dialog_cancel)) } }
        )
    }

    if (showAddDayDialog) {
        AlertDialog(
            onDismissRequest = { showAddDayDialog = false },
            title = { Text(stringResource(R.string.alarms_dialog_lead_time_title)) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    WheelPicker(
                        range = (0..30).toList(),
                        initialValue = selectedDayByWheel,
                        onValueChange = { selectedDayByWheel = it }
                    )
                }
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
                }) { Text(stringResource(R.string.alarms_add_button)) }
            },
            dismissButton = { TextButton(onClick = { showAddDayDialog = false }) { Text(stringResource(R.string.dialog_cancel)) } }
        )
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class AlarmsViewModel @javax.inject.Inject constructor(
    val filterManager: FilterManager
) : androidx.lifecycle.ViewModel()
