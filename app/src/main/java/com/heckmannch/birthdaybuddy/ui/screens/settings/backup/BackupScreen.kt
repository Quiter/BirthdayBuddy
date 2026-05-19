package com.heckmannch.birthdaybuddy.ui.screens.settings.backup

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.screens.settings.backup.components.BackupContent
import com.heckmannch.birthdaybuddy.viewmodel.BackupViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Der Backup-Screen der App.
 * Orchestriert die Logik für den Export und Import von Geschenkideen.
 */
@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    val exportSuccessMsg = stringResource(R.string.backup_export_success)
    val exportFailedMsg = stringResource(R.string.backup_export_failed)
    val importInvalidMsg = stringResource(R.string.backup_import_invalid)
    val importFailedMsg = stringResource(R.string.backup_import_failed)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                try {
                    val json = viewModel.exportGiftIdeas()
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray())
                    }
                    Toast.makeText(context, exportSuccessMsg, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, exportFailedMsg.format(e.message), Toast.LENGTH_LONG).show()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                try {
                    val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().readText()
                    }
                    if (json != null) {
                        val count = viewModel.importGiftIdeas(json)
                        if (count >= 0) {
                            // Wir nutzen hier den count für den Singular/Plural-Check
                            // Da Toast außerhalb der UI-Komposition aufgerufen wird, 
                            // holen wir uns den String über den Context-Helper
                            val message = context.resources.getQuantityString(R.plurals.backup_import_success, count, count)
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, importInvalidMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, importFailedMsg.format(e.message), Toast.LENGTH_LONG).show()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    BackupContent(
        isLoading = isLoading,
        onExportClick = {
            val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            exportLauncher.launch("birthday_buddy_backup_$date.json")
        },
        onImportClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) },
        onNavigateBack = onNavigateBack
    )
}
