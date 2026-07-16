package com.heckmannch.birthdaybuddy.ui.screens.settings.sync

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.InfoCard
import com.heckmannch.birthdaybuddy.ui.components.SettingsCard
import com.heckmannch.birthdaybuddy.ui.components.SettingsClickableRow
import com.heckmannch.birthdaybuddy.ui.components.SettingsDetailScaffold
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeIntent
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeViewModel
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

@Composable
fun SyncSettingsScreen(
    viewModel: HomeViewModel,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val syncSuccessMessage = stringResource(R.string.sync_success)

    LaunchedEffect(Unit) {
        viewModel.syncCompletedEvent.collect {
            snackbarHostState.showSnackbar(syncSuccessMessage)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            viewModel.onIntent(HomeIntent.SyncContacts(showLoading = true))
        }
    }

    val onSyncClick: () -> Unit = {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) -> {
                viewModel.onIntent(HomeIntent.SyncContacts(showLoading = true))
            }

            else -> permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    SyncSettingsContent(
        snackbarHostState = snackbarHostState,
        onSyncClick = onSyncClick,
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
private fun SyncSettingsContent(
    snackbarHostState: SnackbarHostState,
    onSyncClick: () -> Unit,
    showBackButton: Boolean,
    onNavigateBack: () -> Unit,
) {
    SettingsDetailScaffold(
        title = stringResource(R.string.settings_sync_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = SpacingNormal,
                top = paddingValues.calculateTopPadding() + SpacingSmall,
                end = SpacingNormal,
                bottom = paddingValues.calculateBottomPadding() + SpacingSmall
            ),
            verticalArrangement = Arrangement.spacedBy(SpacingNormal)
        ) {
            item {
                SettingsCard {
                    SettingsClickableRow(
                        title = stringResource(R.string.sync_button_title),
                        description = stringResource(R.string.sync_button_desc),
                        onClick = onSyncClick,
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
        SyncSettingsContent(
            snackbarHostState = remember { SnackbarHostState() },
            onSyncClick = {},
            showBackButton = true,
            onNavigateBack = {}
        )
    }
}


