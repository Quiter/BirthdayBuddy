package com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.data.local.NotificationRule

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
            title = {
                Text(
                    stringResource(
                        if (rule == null) R.string.notifications_add_rule else R.string.notifications_edit_rule
                    )
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.dialog_rule_question),
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
                                            0 -> stringResource(R.string.dialog_preset_today)
                                            1 -> stringResource(R.string.dialog_preset_tomorrow)
                                            7 -> stringResource(R.string.dialog_preset_week)
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
                            0 -> stringResource(R.string.rule_today)
                            1 -> stringResource(R.string.rule_tomorrow)
                            7 -> stringResource(R.string.rule_one_week)
                            else -> pluralStringResource(
                                R.plurals.rule_days_before,
                                daysBeforeState.intValue,
                                daysBeforeState.intValue
                            )
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
                        Text(
                            stringResource(R.string.dialog_slider_today),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            stringResource(R.string.dialog_slider_max),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimePickerState.value = true }) {
                    Text(stringResource(R.string.dialog_next))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel))
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
            title = { Text(stringResource(R.string.dialog_time_title)) },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(
                            daysBeforeState.intValue,
                            timePickerState.hour,
                            timePickerState.minute
                        )
                    },
                ) {
                    Text(stringResource(R.string.dialog_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerState.value = false }) {
                    Text(stringResource(R.string.dialog_back))
                }
            },
        )
    }
}
