package com.heckmannch.birthdaybuddy.ui.screens.home.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R

/**
 * Dialog für den Erststart der App.
 * Frägt den Nutzer, ob Benachrichtigungen aktiviert werden sollen.
 */
@Composable
fun OnboardingDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.onboarding_notif_title)) },
        text = { Text(stringResource(R.string.onboarding_notif_desc)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.onboarding_notif_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.onboarding_notif_no))
            }
        }
    )
}
