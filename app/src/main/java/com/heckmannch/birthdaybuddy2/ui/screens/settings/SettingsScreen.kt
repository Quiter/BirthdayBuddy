package com.heckmannch.birthdaybuddy2.ui.screens.settings

import android.Manifest
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy2.ui.theme.BirthdayBuddy2Theme
import com.heckmannch.birthdaybuddy2.viewmodel.BirthdayViewModel

@Composable
fun SettingsScreen(
    viewModel: BirthdayViewModel,
    onNavigateToLabels: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            viewModel.syncContacts()
        }
    }

    SettingsContent(
        onSyncClick = {
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) -> {
                    viewModel.syncContacts()
                }
                else -> permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        },
        onNavigateToLabels = onNavigateToLabels,
        onNavigateToNotifications = onNavigateToNotifications,
        onNavigateToBackup = onNavigateToBackup,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    onSyncClick: () -> Unit,
    onNavigateToLabels: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                item {
                    ListItem(
                        headlineContent = { Text("Kontakte synchronisieren") },
                        supportingContent = { Text("Geburtstage aus deinen System-Kontakten laden") },
                        leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        modifier = Modifier.clickable { onSyncClick() }
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text("Labels verwalten") },
                        supportingContent = { Text("Filter anpassen und Kontakte nach Gruppen ausblenden") },
                        leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToLabels() }
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text("Benachrichtigungen") },
                        supportingContent = { Text("Erinnerungen und Uhrzeit verwalten") },
                        leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToNotifications() }
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text("Daten sichern") },
                        supportingContent = { Text("Geschenkideen exportieren oder importieren") },
                        leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToBackup() }
                    )
                }
            }

            SettingsFooter()
        }
    }
}

@Composable
private fun SettingsFooter() {
    val context = LocalContext.current
    val versionName = remember {
        try {
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.1.3"
        } catch (_: Exception) {
            "1.1.3"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "BirthdayBuddy",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Version $versionName",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Made with ❤️ in Dortmund",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    BirthdayBuddy2Theme {
        SettingsContent(
            onSyncClick = {},
            onNavigateToLabels = {},
            onNavigateToNotifications = {},
            onNavigateToBackup = {},
            onNavigateBack = {}
        )
    }
}
