package com.heckmannch.birthdaybuddy.ui.screens.settings

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
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.viewmodel.HomeViewModel

@Composable
fun SettingsScreen(
    viewModel: HomeViewModel,
    onNavigateToLabels: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToAbout: () -> Unit,
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
                    viewModel.syncContacts(showLoading = true)
                }
                else -> permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        },
        onNavigateToLabels = onNavigateToLabels,
        onNavigateToNotifications = onNavigateToNotifications,
        onNavigateToBackup = onNavigateToBackup,
        onNavigateToAbout = onNavigateToAbout,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    onSyncClick: () -> Unit,
    onNavigateToLabels: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.notifications_back),
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
                        headlineContent = { Text(stringResource(R.string.settings_notifications_title)) },
                        supportingContent = { Text(stringResource(R.string.settings_notifications_desc)) },
                        leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToNotifications() }
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_labels_title)) },
                        supportingContent = { Text(stringResource(R.string.settings_labels_desc)) },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToLabels() }
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_backup_title)) },
                        supportingContent = { Text(stringResource(R.string.settings_backup_desc)) },
                        leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToBackup() }
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_sync_title)) },
                        supportingContent = { Text(stringResource(R.string.settings_sync_desc)) },
                        leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        modifier = Modifier.clickable { onSyncClick() }
                    )
                }

                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_about_title)) },
                        supportingContent = { Text(stringResource(R.string.settings_about_desc)) },
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToAbout() }
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
            packageInfo.versionName ?: "1.4.4"
        } catch (_: Exception) {
            "1.4.4"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.settings_version, versionName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_made_with),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    BirthdayBuddyTheme {
        SettingsContent(
            onSyncClick = {},
            onNavigateToLabels = {},
            onNavigateToNotifications = {},
            onNavigateToBackup = {},
            onNavigateToAbout = {},
            onNavigateBack = {}
        )
    }
}
