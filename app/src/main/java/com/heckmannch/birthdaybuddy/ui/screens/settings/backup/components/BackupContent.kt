package com.heckmannch.birthdaybuddy.ui.screens.settings.backup.components

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.InfoCard
import com.heckmannch.birthdaybuddy.ui.components.SettingsCard
import com.heckmannch.birthdaybuddy.ui.components.SettingsClickableRow
import com.heckmannch.birthdaybuddy.ui.components.SettingsDetailScaffold
import com.heckmannch.birthdaybuddy.ui.model.BackupMessage
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisLow
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

/**
 * Die UI-Darstellung der Backup-Seite.
 */
@Composable
fun BackupContent(
    isLoading: Boolean = false,
    message: BackupMessage? = null,
    showBackButton: Boolean = true,
    onExportClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val exportSuccessMsg = stringResource(R.string.backup_export_success)
    val exportFailedMsg = stringResource(R.string.backup_export_failed)
    val importInvalidMsg = stringResource(R.string.backup_import_invalid)
    val importFailedMsg = stringResource(R.string.backup_import_failed)
    val context = LocalContext.current

    LaunchedEffect(message) {
        val msg = message ?: return@LaunchedEffect
        when (msg) {
            is BackupMessage.ExportSuccess -> snackbarHostState.showSnackbar(exportSuccessMsg)
            is BackupMessage.ExportError -> snackbarHostState.showSnackbar(exportFailedMsg.format(msg.errorMessage ?: ""))
            is BackupMessage.ImportSuccess -> {
                val formatted = context.applicationContext.resources.getQuantityString(
                    R.plurals.backup_import_success,
                    msg.count,
                    msg.count
                )
                snackbarHostState.showSnackbar(formatted)
            }
            is BackupMessage.ImportInvalid -> snackbarHostState.showSnackbar(importInvalidMsg)
            is BackupMessage.ImportError -> snackbarHostState.showSnackbar(importFailedMsg.format(msg.errorMessage ?: ""))
        }
    }

    SettingsDetailScaffold(
        title = stringResource(R.string.settings_backup_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = SpacingNormal,
                    top = paddingValues.calculateTopPadding() + SpacingNormal,
                    end = SpacingNormal,
                    bottom = paddingValues.calculateBottomPadding() + SpacingNormal
                ),
            verticalArrangement = Arrangement.spacedBy(SpacingNormal)
        ) {
            Text(
                stringResource(R.string.backup_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SettingsCard {
                SettingsClickableRow(
                    title = stringResource(R.string.backup_export_title),
                    description = stringResource(R.string.backup_export_desc),
                    onClick = onExportClick,
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                    enabled = !isLoading
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AlphaEmphasisLow),
                    modifier = Modifier.padding(horizontal = SpacingNormal)
                )
                SettingsClickableRow(
                    title = stringResource(R.string.backup_import_title),
                    description = stringResource(R.string.backup_import_desc),
                    onClick = onImportClick,
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                    enabled = !isLoading
                )
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            InfoCard(
                title = stringResource(R.string.backup_info_title),
                description = stringResource(R.string.backup_info_content)
            )
        }
    }
}

/**
 * Test-accessible entry point for Backup screen content.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
fun BackupScreenContent(
    isLoading: Boolean = false,
    message: BackupMessage? = null,
    showBackButton: Boolean = true,
    onExportClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    BackupContent(
        isLoading = isLoading,
        message = message,
        showBackButton = showBackButton,
        onExportClick = onExportClick,
        onImportClick = onImportClick,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    )
}

@Preview(showBackground = true)
@Composable
private fun BackupPreview() {
    BirthdayBuddyTheme {
        BackupContent(
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
            isLoading = true,
            onExportClick = {},
            onImportClick = {},
            onNavigateBack = {}
        )
    }
}
