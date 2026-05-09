package com.heckmannch.birthdaybuddy2.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy2.viewmodel.BirthdayViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Kontakte synchronisieren") },
                    supportingContent = { Text("Geburtstage aus deinen System-Kontakten laden") },
                    leadingContent = {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_CONTACTS,
                            ) -> {
                                viewModel.syncContacts()
                            }
                            else ->
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Labels verwalten") },
                    supportingContent = { Text("Filter anpassen und Kontakte nach Gruppen ausblenden") },
                    leadingContent = {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onNavigateToLabels() }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Benachrichtigungen") },
                    supportingContent = { Text("Erinnerungen und Uhrzeit verwalten") },
                    leadingContent = {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onNavigateToNotifications() }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Daten sichern") },
                    supportingContent = { Text("Geschenkideen exportieren oder importieren") },
                    leadingContent = {
                        Icon(Icons.Default.Share, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onNavigateToBackup() }
                )
            }

            item {
                val versionName = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (_: Exception) {
                    "1.1.3"
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
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
        }
    }
}
