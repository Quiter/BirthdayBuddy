package com.heckmannch.birthdaybuddy.ui.screens.labels.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * Dialog for creating a new contact label.
 */
@Composable
internal fun AddLabelDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var newLabelNameInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.labels_add_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            OutlinedTextField(
                value = newLabelNameInput,
                onValueChange = { newLabelNameInput = it },
                label = { Text(stringResource(R.string.labels_add_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = newLabelNameInput.trim()
                    if (trimmed.isNotEmpty()) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm(trimmed)
                    }
                },
                enabled = newLabelNameInput.isNotBlank(),
            ) {
                Text(stringResource(R.string.labels_add_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.labels_cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun AddLabelDialogPreview() {
    BirthdayBuddyTheme {
        AddLabelDialog(
            onDismiss = {},
            onConfirm = {}
        )
    }
}
