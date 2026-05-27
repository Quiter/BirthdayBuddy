package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Ein wiederverwendbarer und modularer DatePickerDialog für die Geburtstagseingabe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayDatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        onDateSelected(date)
                    }
                    onDismissRequest()
                },
            ) {
                Text(stringResource(R.string.gift_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.gift_dialog_cancel))
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}
