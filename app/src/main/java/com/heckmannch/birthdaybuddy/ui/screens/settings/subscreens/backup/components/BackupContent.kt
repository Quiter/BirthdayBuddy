package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.backup.components

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsFooter
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsGroup
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsItem
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsNotice
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSection
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSemanticTone
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSubpageScaffold
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.backup.BackupMessage
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThick
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeMedium

/**
 * Expressive UI content for the Backup & Restore settings subpage.
 */
@Composable
fun BackupContent(
    isLoading: Boolean = false,
    message: BackupMessage? = null,
    showBackButton: Boolean = true,
    onExportClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onDismissMessage: () -> Unit = {},
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
            is BackupMessage.ExportError -> snackbarHostState.showSnackbar(
                exportFailedMsg.format(msg.errorMessage ?: "")
            )
            is BackupMessage.ImportSuccess -> {
                val formatted = context.applicationContext.resources.getQuantityString(
                    R.plurals.backup_import_success,
                    msg.count,
                    msg.count,
                )
                snackbarHostState.showSnackbar(formatted)
            }
            is BackupMessage.ImportInvalid -> snackbarHostState.showSnackbar(importInvalidMsg)
            is BackupMessage.ImportError -> snackbarHostState.showSnackbar(
                importFailedMsg.format(msg.errorMessage ?: "")
            )
        }
        onDismissMessage()
    }

    val loadingStatus: (@Composable () -> Unit)? = if (isLoading) {
        {
            CircularProgressIndicator(
                modifier = Modifier.size(IconSizeMedium),
                strokeWidth = BorderWidthThick,
            )
        }
    } else {
        null
    }

    SettingsSubpageScaffold(
        title = stringResource(R.string.settings_backup_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) {
        SettingsSection(title = stringResource(R.string.settings_backup_title)) {
            SettingsGroup {
                SettingsItem(
                    onClick = onExportClick,
                    title = stringResource(R.string.backup_export_title),
                    subtitle = stringResource(R.string.backup_export_desc),
                    icon = Icons.Rounded.Upload,
                    enabled = !isLoading,
                    statusContent = loadingStatus,
                )
                SettingsItem(
                    onClick = onImportClick,
                    title = stringResource(R.string.backup_import_title),
                    subtitle = stringResource(R.string.backup_import_desc),
                    icon = Icons.Rounded.Download,
                    enabled = !isLoading,
                    statusContent = loadingStatus,
                )
            }
            SettingsFooter(text = stringResource(R.string.backup_desc))
        }

        SettingsNotice(
            title = stringResource(R.string.backup_info_title),
            message = stringResource(R.string.backup_info_content),
            icon = Icons.Rounded.Info,
            tone = SettingsSemanticTone.Neutral,
        )
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
    onDismissMessage: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    BackupContent(
        isLoading = isLoading,
        message = message,
        showBackButton = showBackButton,
        onExportClick = onExportClick,
        onImportClick = onImportClick,
        onNavigateBack = onNavigateBack,
        onDismissMessage = onDismissMessage,
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
            onNavigateBack = {},
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
            onNavigateBack = {},
        )
    }
}
