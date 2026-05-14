package com.heckmannch.birthdaybuddy.ui.screens.settings.backup.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * Die UI-Darstellung der Backup-Seite.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupContent(
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
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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

@Preview(showBackground = true)
@Composable
private fun BackupLoadingPreview() {
    BirthdayBuddyTheme {
        BackupContent(
            isLoading = true,
            onExportClick = {},
            onImportClick = {},
            onNavigateBack = {}
        )
    }
}
