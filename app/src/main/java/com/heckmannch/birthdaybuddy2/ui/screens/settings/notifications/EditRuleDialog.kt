package com.heckmannch.birthdaybuddy2.ui.screens.settings.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy2.database.NotificationRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRuleDialog(
    rule: NotificationRule? = null,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int) -> Unit,
) {
    val daysBeforeState = remember { mutableIntStateOf(rule?.daysBefore ?: 0) }
    val showTimePickerState = remember { mutableStateOf(value = false) }

    if (!showTimePickerState.value) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (rule == null) "Regel hinzufügen" else "Regel bearbeiten") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Wann möchtest du erinnert werden?",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Start),
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(0, 1, 7).forEach { preset ->
                            FilterChip(
                                selected = daysBeforeState.intValue == preset,
                                onClick = { daysBeforeState.intValue = preset },
                                label = {
                                    Text(
                                        when (preset) {
                                            0 -> "Heute"
                                            1 -> "Morgen"
                                            7 -> "1 Woche"
                                            else -> ""
                                        },
                                    )
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = when (daysBeforeState.intValue) {
                            0 -> "Am Tag selbst"
                            1 -> "1 Tag vorher"
                            7 -> "1 Woche vorher"
                            else -> "${daysBeforeState.intValue} Tage vorher"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Slider(
                        value = daysBeforeState.intValue.toFloat(),
                        onValueChange = { daysBeforeState.intValue = it.toInt() },
                        valueRange = 0f..30f,
                        steps = 29,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Heute", style = MaterialTheme.typography.labelSmall)
                        Text("30 Tage", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimePickerState.value = true }) {
                    Text("Weiter")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
            },
        )
    } else {
        val timePickerState = rememberTimePickerState(
            initialHour = rule?.hour ?: 9,
            initialMinute = rule?.minute ?: 0,
            is24Hour = true,
        )

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Uhrzeit wählen") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(daysBeforeState.intValue, timePickerState.hour, timePickerState.minute)
                    },
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerState.value = false }) {
                    Text("Zurück")
                }
            },
        )
    }
}
