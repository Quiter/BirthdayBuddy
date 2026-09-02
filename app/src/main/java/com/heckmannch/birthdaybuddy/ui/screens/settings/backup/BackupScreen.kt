package com.heckmannch.birthdaybuddy.ui.screens.settings.backup

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.model.BackupMessage
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val exportSuccessMsg = stringResource(R.string.backup_export_success)
    val exportFailedMsg = stringResource(R.string.backup_export_failed)
    val importInvalidMsg = stringResource(R.string.backup_import_invalid)
    val importFailedMsg = stringResource(R.string.backup_import_failed)

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        when (message) {
            is BackupMessage.ExportSuccess -> {
                Toast.makeText(context, exportSuccessMsg, Toast.LENGTH_SHORT).show()
            }

            is BackupMessage.ExportError -> {
                Toast.makeText(
                    context,
                    exportFailedMsg.format(message.errorMessage ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            }

            is BackupMessage.ImportSuccess -> {
                val formattedMessage = context.applicationContext.resources.getQuantityString(
                    R.plurals.backup_import_success,
                    message.count,
                    message.count
                )
                Toast.makeText(context, formattedMessage, Toast.LENGTH_SHORT).show()
            }

            is BackupMessage.ImportInvalid -> {
                Toast.makeText(context, importInvalidMsg, Toast.LENGTH_SHORT).show()
            }

            is BackupMessage.ImportError -> {
                Toast.makeText(
                    context,
                    importFailedMsg.format(message.errorMessage ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        viewModel.onIntent(BackupIntent.ClearMessage)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onIntent(BackupIntent.ExportBackup(uri))
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onIntent(BackupIntent.ImportBackup(uri))
        }
    }

    BackupContent(
        isLoading = uiState.isLoading,
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
