package com.heckmannch.birthdaybuddy.ui.screens.settings.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onIntent(BackupIntent.ExportGiftIdeas(uri.toString()))
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onIntent(BackupIntent.ImportGiftIdeas(uri.toString()))
        }
    }

    BackupContent(
        isLoading = uiState.isLoading,
        message = uiState.message,
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
        onNavigateBack = onNavigateBack,
        onDismissMessage = {
            viewModel.onIntent(BackupIntent.ClearMessage)
        }
    )
}
