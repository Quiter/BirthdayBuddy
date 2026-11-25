package com.heckmannch.birthdaybuddy.ui.screens.settings.storage

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.SettingsDetailScaffold
import com.heckmannch.birthdaybuddy.ui.components.SettingsGroup
import com.heckmannch.birthdaybuddy.ui.components.SettingsItem
import com.heckmannch.birthdaybuddy.ui.components.SettingsSection
import com.heckmannch.birthdaybuddy.ui.components.withSettingsInsets
import java.io.File
import java.util.Locale

@Composable
fun StorageSettingsScreen(
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val cacheSize = remember {
        try {
            val cacheDir = context.cacheDir
            val size = getFolderSize(cacheDir)
            formatBytes(size)
        } catch (e: Exception) {
            "0 B"
        }
    }

    val dbSize = remember {
        try {
            val dbFile = context.getDatabasePath("settings_database")
            if (dbFile.exists()) formatBytes(dbFile.length()) else "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    SettingsDetailScaffold(
        title = stringResource(R.string.storage_management_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues.withSettingsInsets())
        ) {
            SettingsSection(
                title = stringResource(R.string.storage_management_title),
                icon = Icons.Default.Storage
            ) {
                SettingsGroup {
                    SettingsItem(
                        onClick = {},
                        title = stringResource(R.string.storage_database_size),
                        subtitle = dbSize,
                        icon = Icons.Default.Info
                    )
                    SettingsItem(
                        onClick = {},
                        title = stringResource(R.string.storage_cache_size),
                        subtitle = cacheSize,
                        icon = Icons.Default.Storage
                    )
                    SettingsItem(
                        onClick = {
                            try {
                                context.cacheDir.deleteRecursively()
                            } catch (_: Exception) {}
                        },
                        title = stringResource(R.string.storage_clear_cache),
                        subtitle = stringResource(R.string.storage_clear_cache_desc),
                        icon = Icons.Default.Delete
                    )
                    SettingsItem(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        title = stringResource(R.string.storage_app_info),
                        subtitle = stringResource(R.string.storage_app_info_desc),
                        icon = Icons.Default.Info
                    )
                }
            }
        }
    }
}

private fun getFolderSize(file: File): Long {
    var size: Long = 0
    if (file.isDirectory) {
        file.listFiles()?.forEach { child ->
            size += getFolderSize(child)
        }
    } else {
        size = file.length()
    }
    return size
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.ROOT, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.ROOT, "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.ROOT, "%.1f GB", gb)
}
