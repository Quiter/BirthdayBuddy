package com.heckmannch.birthdaybuddy.ui.screens.settings.backup

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.heckmannch.birthdaybuddy.ui.screens.settings.backup.components.BackupContent
import com.heckmannch.birthdaybuddy.viewmodel.BirthdayViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Der Backup-Screen der App.
 * Orchestriert die Logik für den Export und Import von Geschenkideen.
 */
@Composable
fun BackupScreen(
    viewModel: BirthdayViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

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
                    Toast.makeText(context, "Export erfolgreich!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export fehlgeschlagen: ${e.message}", Toast.LENGTH_LONG).show()
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
                            Toast.makeText(context, "$count Geschenkideen importiert!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Ungültiges Dateiformat", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Import fehlgeschlagen: ${e.message}", Toast.LENGTH_LONG).show()
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
