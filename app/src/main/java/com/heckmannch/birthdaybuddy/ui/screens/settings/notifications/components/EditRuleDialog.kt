package com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.data.local.NotificationRule

private enum class RuleUnit {
    DAYS, WEEKS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRuleDialog(
    rule: NotificationRule? = null,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int) -> Unit,
) {
    val initialUnit = remember(rule) {
        if (rule != null && rule.daysBefore % 7 == 0 && rule.daysBefore > 0) {
            RuleUnit.WEEKS
        } else {
            RuleUnit.DAYS
        }
    }
    val initialNumber = remember(rule) {
        if (rule != null) {
            if (rule.daysBefore % 7 == 0 && rule.daysBefore > 0) {
                (rule.daysBefore / 7).toString()
            } else {
                rule.daysBefore.toString()
            }
        } else {
            "1"
        }
    }

    val numberStringState = remember { mutableStateOf(initialNumber) }
    val selectedUnitState = remember { mutableStateOf(initialUnit) }

    val hourState = remember { mutableIntStateOf(rule?.hour ?: 9) }
    val minuteState = remember { mutableIntStateOf(rule?.minute ?: 0) }

    val showTimePickerState = remember { mutableStateOf(false) }

    val isInputValid = numberStringState.value.isNotBlank() && numberStringState.value.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (rule == null) R.string.notifications_add_rule else R.string.notifications_edit_rule
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = numberStringState.value,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            numberStringState.value = newValue
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                val displayNum = numberStringState.value.toIntOrNull() ?: 1

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedUnitState.value = RuleUnit.DAYS }
                            .padding(vertical = 6.dp)
                    ) {
                        RadioButton(
                            selected = selectedUnitState.value == RuleUnit.DAYS,
                            onClick = { selectedUnitState.value = RuleUnit.DAYS }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pluralStringResource(
                                id = R.plurals.dialog_rule_unit_days,
                                count = displayNum,
                                displayNum
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedUnitState.value = RuleUnit.WEEKS }
                            .padding(vertical = 6.dp)
                    ) {
                        RadioButton(
                            selected = selectedUnitState.value == RuleUnit.WEEKS,
                            onClick = { selectedUnitState.value = RuleUnit.WEEKS }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pluralStringResource(
                                id = R.plurals.dialog_rule_unit_weeks,
                                count = displayNum,
                                displayNum
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePickerState.value = true }
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.rule_time_format,
                            hourState.intValue,
                            minuteState.intValue
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isInputValid,
                onClick = {
                    val num = numberStringState.value.toIntOrNull() ?: 0
                    val daysBefore = if (selectedUnitState.value == RuleUnit.WEEKS) num * 7 else num
                    onConfirm(daysBefore, hourState.intValue, minuteState.intValue)
                }
            ) {
                Text(stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )

    if (showTimePickerState.value) {
        val timePickerState = rememberTimePickerState(
            initialHour = hourState.intValue,
            initialMinute = minuteState.intValue,
            is24Hour = true,
        )

        AlertDialog(
            onDismissRequest = { showTimePickerState.value = false },
            title = { Text(stringResource(R.string.dialog_time_title)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        hourState.intValue = timePickerState.hour
                        minuteState.intValue = timePickerState.minute
                        showTimePickerState.value = false
                    }
                ) {
                    Text(stringResource(R.string.dialog_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerState.value = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

