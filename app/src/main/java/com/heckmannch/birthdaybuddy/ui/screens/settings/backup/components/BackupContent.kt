package com.heckmannch.birthdaybuddy.ui.screens.settings.backup.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * Die UI-Darstellung der Backup-Seite.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupContent(
    windowWidthSizeClass: WindowWidthSizeClass,
    isLoading: Boolean,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    AppResponsiveScaffold(
        windowWidthSizeClass = windowWidthSizeClass,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.notifications_back)
                        )
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.backup_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                onClick = onExportClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.backup_export_title)) },
                    supportingContent = { Text(stringResource(R.string.backup_export_desc)) },
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
                    headlineContent = { Text(stringResource(R.string.backup_import_title)) },
                    supportingContent = { Text(stringResource(R.string.backup_import_desc)) },
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
                stringResource(R.string.backup_info_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.backup_info_content),
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
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
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
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            isLoading = true,
            onExportClick = {},
            onImportClick = {},
            onNavigateBack = {}
        )
    }
}
