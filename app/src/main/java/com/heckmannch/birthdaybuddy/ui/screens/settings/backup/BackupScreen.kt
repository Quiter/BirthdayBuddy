package com.heckmannch.birthdaybuddy.ui.screens.settings.backup

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.screens.settings.backup.components.BackupContent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Der Backup-Screen der App.
 * Orchestriert die Logik für den Export und Import von Geschenkideen.
 */
@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    val exportSuccessMsg = stringResource(R.string.backup_export_success)
    val exportFailedMsg = stringResource(R.string.backup_export_failed)
    val importInvalidMsg = stringResource(R.string.backup_import_invalid)
    val importFailedMsg = stringResource(R.string.backup_import_failed)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            isLoading = true
            viewModel.onIntent(
                BackupIntent.ExportBackup(
                    uri = uri,
                    onSuccess = {
                        isLoading = false
                        Toast.makeText(context, exportSuccessMsg, Toast.LENGTH_SHORT).show()
                    },
                    onError = { e ->
                        isLoading = false
                        Toast.makeText(context, exportFailedMsg.format(e.message), Toast.LENGTH_LONG)
                            .show()
                    }
                )
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isLoading = true
            viewModel.onIntent(
                BackupIntent.ImportBackup(
                    uri = uri,
                    onSuccess = { count ->
                        isLoading = false
                        val message = context.applicationContext.resources.getQuantityString(
                            R.plurals.backup_import_success,
                            count,
                            count
                        )
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    },
                    onInvalid = {
                        isLoading = false
                        Toast.makeText(context, importInvalidMsg, Toast.LENGTH_SHORT).show()
                    },
                    onError = { e ->
                        isLoading = false
                        Toast.makeText(context, importFailedMsg.format(e.message), Toast.LENGTH_LONG)
                            .show()
                    }
                )
            )
        }
    }

    BackupContent(
        isLoading = isLoading,
        showBackButton = showBackButton,
        onExportClick = {
            val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            exportLauncher.launch("birthday_buddy_backup_$date.json")
        },
        onImportClick = {
            importLauncher.launch(
                arrayOf(
                    "application/json",
                    "application/octet-stream",
                    "*/*"
                )
            )
        },
        onNavigateBack = onNavigateBack
    )
}
