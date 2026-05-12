package com.heckmannch.birthdaybuddy2.ui.screens.settings.backup

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy2.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy2.viewmodel.BirthdayViewModel
import kotlinx.coroutines.launch

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
        onExportClick = { exportLauncher.launch("birthday_buddy_backup.json") },
        onImportClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) },
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupContent(
    isLoading: Boolean,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daten sichern") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Exportiere deine Geschenkideen in eine Datei, um sie später wiederherzustellen oder auf ein anderes Gerät zu übertragen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                onClick = onExportClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                ListItem(
                    headlineContent = { Text("Geschenkideen exportieren") },
                    supportingContent = { Text("Speichert alle Ideen in einer JSON-Datei") },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            }

            Card(
                onClick = onImportClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                ListItem(
                    headlineContent = { Text("Geschenkideen importieren") },
                    supportingContent = { Text("Wiederherstellen aus einer Backup-Datei") },
                    leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            InfoSection()
        }
    }
}

@Composable
private fun InfoSection() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Wichtige Hinweise:",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "• Der Import ordnet die Ideen automatisch deinen Kontakten zu.\n" +
                "• Kontakte werden zuerst über den stabilen System-Key (Lookup) und dann über den Namen gesucht.\n" +
                "• Existierende Geschenkideen für einen Kontakt werden beim Import überschrieben.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BackupPreview() {
    BirthdayBuddyTheme {
        BackupContent(
            isLoading = false,
            onExportClick = {},
            onImportClick = {},
            onNavigateBack = {}
        )
    }
}
