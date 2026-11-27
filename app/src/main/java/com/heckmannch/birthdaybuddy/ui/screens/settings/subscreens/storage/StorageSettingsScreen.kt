package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.storage

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.settings.InfoStatBox
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsGroup
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsItem
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsItemCard
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSection
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSubpageScaffold
import com.heckmannch.birthdaybuddy.ui.components.settings.ThemedIcon
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThick
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeMedium

private const val TAG = "StorageSettingsScreen"

/**
 * Stateful entry point for the Storage Management settings subpage.
 *
 * Connects [StorageSettingsViewModel] to the stateless [StorageSettingsContent], handles
 * one-off snackbar events, refreshes storage usage on resume, and launches Android's
 * application details settings safely.
 */
@Composable
fun StorageSettingsScreen(
    viewModel: StorageSettingsViewModel = hiltViewModel(),
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val cacheClearedMessage = stringResource(R.string.settings_storage_cache_cleared)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshStorageUsage()
    }

    LaunchedEffect(viewModel, cacheClearedMessage) {
        viewModel.events.collect { event ->
            when (event) {
                StorageSettingsEvent.CacheCleared -> {
                    snackbarHostState.showSnackbar(cacheClearedMessage)
                }
            }
        }
    }

    StorageSettingsContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        showBackButton = showBackButton,
        onClearCacheClick = viewModel::clearCache,
        onOpenAppInfoClick = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "Unable to open application details settings.", e)
            }
        },
        onNavigateBack = onNavigateBack,
    )
}

/**
 * Stateless UI content for the Storage Management settings subpage displaying total usage,
 * database and cache breakdown, and cache cleanup actions.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun StorageSettingsContent(
    uiState: StorageUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    showBackButton: Boolean = true,
    onClearCacheClick: () -> Unit = {},
    onOpenAppInfoClick: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
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
                    value = uiState.formattedTotalSize,
                    icon = Icons.Rounded.Storage,
                )
            }
        }

        SettingsSection(title = stringResource(R.string.storage_overview_title)) {
            SettingsGroup {
                SettingsItem(
                    onClick = null,
                    title = stringResource(R.string.storage_database_size),
                    subtitle = uiState.formattedDatabaseSize,
                    icon = Icons.Rounded.Storage,
                )
                SettingsItem(
                    onClick = null,
                    title = stringResource(R.string.storage_cache_size),
                    subtitle = uiState.formattedCacheSize,
                    icon = Icons.Rounded.Cached,
                )
            }
        }

        SettingsSection(title = stringResource(R.string.storage_actions_title)) {
            SettingsGroup {
                SettingsItem(
                    onClick = onClearCacheClick,
                    title = stringResource(R.string.storage_clear_cache),
                    subtitle = stringResource(R.string.storage_clear_cache_desc),
                    icon = Icons.Rounded.DeleteOutline,
                    enabled = !uiState.isClearingCache,
                    statusContent = if (uiState.isClearingCache) {
                        {
                            CircularProgressIndicator(
                                modifier = Modifier.size(IconSizeMedium),
                                strokeWidth = BorderWidthThick,
                            )
                        }
                    } else {
                        null
                    },
                )
                SettingsItem(
                    onClick = onOpenAppInfoClick,
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

@Preview(showBackground = true)
@Composable
private fun StorageSettingsPreview() {
    BirthdayBuddyTheme {
        StorageSettingsContent(
            uiState = StorageUiState(
                databaseBytes = 245_760L,
                cacheBytes = 1_572_864L,
            ),
            showBackButton = true,
        )
    }
}
