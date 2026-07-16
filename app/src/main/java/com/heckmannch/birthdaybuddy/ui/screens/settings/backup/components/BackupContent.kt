package com.heckmannch.birthdaybuddy.ui.screens.settings.backup.components

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.InfoCard
import com.heckmannch.birthdaybuddy.ui.components.SettingsCard
import com.heckmannch.birthdaybuddy.ui.components.SettingsClickableRow
import com.heckmannch.birthdaybuddy.ui.components.SettingsDetailScaffold
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisLow
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

/**
 * Die UI-Darstellung der Backup-Seite.
 */
@Composable
fun BackupContent(
    isLoading: Boolean,
    showBackButton: Boolean = true,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    SettingsDetailScaffold(
        title = stringResource(R.string.settings_backup_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack
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
