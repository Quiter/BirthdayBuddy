package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.storage

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.settings.InfoStatBox
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsGroup
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsItem
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsItemCard
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSection
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSubpageScaffold
import com.heckmannch.birthdaybuddy.ui.components.settings.ThemedIcon
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeMedium
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * Expressive Storage Management settings subpage displaying total usage, breakdown,
 * and live-refreshing cache cleanup.
 */
@Composable
fun StorageSettingsScreen(
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val cacheClearedMessage = stringResource(R.string.settings_storage_cache_cleared)
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val cacheBytes = remember(refreshTrigger) {
        runCatching { getFolderSize(context.cacheDir) }.getOrDefault(0L)
    }
    val dbBytes = remember(refreshTrigger) {
        runCatching {
            val dbFile = context.getDatabasePath("settings_database")
            if (dbFile.exists()) dbFile.length() else 0L
        }.getOrDefault(0L)
    }
    val totalSize = remember(cacheBytes, dbBytes) { formatBytes(cacheBytes + dbBytes) }
    val cacheSize = remember(cacheBytes) { formatBytes(cacheBytes) }
    val dbSize = remember(dbBytes) { formatBytes(dbBytes) }

    SettingsSubpageScaffold(
        title = stringResource(R.string.storage_management_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) {
        SettingsGroup {
            SettingsItemCard(onClick = null) {
                InfoStatBox(
                    label = stringResource(R.string.settings_storage_total),
                    value = totalSize,
                    icon = Icons.Rounded.Storage,
                )
            }
        }

        SettingsSection(title = stringResource(R.string.storage_overview_title)) {
            SettingsGroup {
                SettingsItem(
                    onClick = null,
                    title = stringResource(R.string.storage_database_size),
                    subtitle = dbSize,
                    icon = Icons.Rounded.Storage,
                )
                SettingsItem(
                    onClick = null,
                    title = stringResource(R.string.storage_cache_size),
                    subtitle = cacheSize,
                    icon = Icons.Rounded.Cached,
                )
            }
        }

        SettingsSection(title = stringResource(R.string.storage_actions_title)) {
            SettingsGroup {
                SettingsItem(
                    onClick = {
                        runCatching {
                            context.cacheDir.listFiles()?.forEach { child ->
                                child.deleteRecursively()
                            }
                        }
                        refreshTrigger++
                        scope.launch {
                            snackbarHostState.showSnackbar(cacheClearedMessage)
                        }
                    },
                    title = stringResource(R.string.storage_clear_cache),
                    subtitle = stringResource(R.string.storage_clear_cache_desc),
                    icon = Icons.Rounded.DeleteOutline,
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
                    icon = Icons.Rounded.Info,
                    trailingContent = {
                        ThemedIcon(
                            icon = Icons.AutoMirrored.Rounded.OpenInNew,
                            size = IconSizeMedium,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
        }
    }
}

private fun getFolderSize(file: File): Long {
    var size = 0L
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

@Preview(showBackground = true)
@Composable
private fun StorageSettingsPreview() {
    BirthdayBuddyTheme {
        StorageSettingsScreen(
            showBackButton = true,
            onNavigateBack = {},
        )
    }
}
