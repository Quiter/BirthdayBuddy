package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.sync

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsGroup
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsItem
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsNotice
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSection
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSemanticTone
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSubpageScaffold
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThick
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeMedium

/**
 * Entry point for the Contact Synchronization settings subpage.
 */
@Composable
fun SyncSettingsScreen(
    viewModel: SyncViewModel = hiltViewModel(),
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val syncSuccessMessage = stringResource(R.string.sync_success)
    val syncErrorMessage = stringResource(R.string.sync_error)

    LaunchedEffect(viewModel, syncSuccessMessage, syncErrorMessage) {
        viewModel.events.collect { event ->
            when (event) {
                is SyncEvent.Success -> {
                    snackbarHostState.showSnackbar(syncSuccessMessage)
                }

                is SyncEvent.Error -> {
                    val message = event.message?.let { "$syncErrorMessage: $it" } ?: syncErrorMessage
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            viewModel.syncContacts()
        }
    }

    val onSyncClick = {
        if (!uiState.isSyncing) {
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) -> {
                    viewModel.syncContacts()
                }

                else -> permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    SyncSettingsScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onSyncClick = onSyncClick,
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack,
    )
}

/**
 * Stateless UI content for Contact Synchronization settings in Morphe expressive style.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun SyncSettingsScreenContent(
    uiState: SyncUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onSyncClick: () -> Unit = {},
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit = {},
) {
    SettingsSubpageScaffold(
        title = stringResource(R.string.settings_sync_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) {
        SettingsSection(title = stringResource(R.string.settings_sync_title)) {
            SettingsGroup {
                SettingsItem(
                    onClick = onSyncClick,
                    title = stringResource(R.string.sync_button_title),
                    subtitle = stringResource(R.string.sync_button_desc),
                    icon = Icons.Rounded.Sync,
                    enabled = !uiState.isSyncing,
                    statusContent = if (uiState.isSyncing) {
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
            }
        }

        SettingsNotice(
            title = stringResource(R.string.sync_explanation_title),
            message = stringResource(R.string.sync_explanation_desc),
            icon = Icons.Rounded.Info,
            tone = SettingsSemanticTone.Neutral,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SyncSettingsPreview() {
    BirthdayBuddyTheme {
        SyncSettingsScreenContent(
            uiState = SyncUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onSyncClick = {},
            showBackButton = true,
            onNavigateBack = {},
        )
    }
}
