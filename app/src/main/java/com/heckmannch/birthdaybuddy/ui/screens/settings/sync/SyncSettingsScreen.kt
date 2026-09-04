package com.heckmannch.birthdaybuddy.ui.screens.settings.sync

import android.Manifest
import android.content.pm.PackageManager
import androidx.annotation.VisibleForTesting
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.InfoCard
import com.heckmannch.birthdaybuddy.ui.components.SettingsCard
import com.heckmannch.birthdaybuddy.ui.components.SettingsClickableRow
import com.heckmannch.birthdaybuddy.ui.components.SettingsDetailScaffold
import com.heckmannch.birthdaybuddy.ui.components.withSettingsInsets
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

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

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun SyncSettingsScreenContent(
    uiState: SyncUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onSyncClick: () -> Unit = {},
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit = {},
) {
    SettingsDetailScaffold(
        title = stringResource(R.string.settings_sync_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues.withSettingsInsets(),
            verticalArrangement = Arrangement.spacedBy(SpacingNormal)
        ) {
            item {
                SettingsCard {
                    if (uiState.isSyncing) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    SettingsClickableRow(
                        title = stringResource(R.string.sync_button_title),
                        description = stringResource(R.string.sync_button_desc),
                        onClick = onSyncClick,
                        enabled = !uiState.isSyncing,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null
                            )
                        }
                    )
                }
            }

            item {
                InfoCard(
                    title = stringResource(R.string.sync_explanation_title),
                    description = stringResource(R.string.sync_explanation_desc)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SyncSettingsPreview() {
    MaterialTheme {
        SyncSettingsScreenContent(
            uiState = SyncUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onSyncClick = {},
            showBackButton = true,
            onNavigateBack = {}
        )
    }
}



