package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.about

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.components.SettingsItem
import com.heckmannch.birthdaybuddy.ui.screens.settings.components.SettingsBody
import com.heckmannch.birthdaybuddy.ui.screens.settings.components.SettingsFooter
import com.heckmannch.birthdaybuddy.ui.screens.settings.components.SettingsHeader
import com.heckmannch.birthdaybuddy.ui.screens.settings.components.SettingsMenuSectionCard
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

private const val TAG = "AboutScreen"

@Composable
fun AboutScreen(
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToLicenses: () -> Unit
) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            val packageInfo: PackageInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
            packageInfo.versionName ?: "2.1.0"
        } catch (e: Exception) {
            Log.w(TAG, "Fehler beim Auslesen der Versionsbezeichnung via PackageManager", e)
            "2.1.0"
        }
    }

    AppResponsiveScaffold(
        topBar = {
            SettingsHeader(
                title = stringResource(R.string.settings_about_title),
                showBackButton = showBackButton,
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        SettingsBody(
            paddingValues = paddingValues,
            footer = {
                SettingsFooter(showInSubscreen = true) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(SpacingNormal)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_made_with),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.about_copyright),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        ) {
            // App header: logo, name, version
            AboutAppHeader(versionName = versionName)

            // Description
            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingNormal)
            )

            // App info section: version + credits
            SettingsMenuSectionCard(
                sectionTitle = stringResource(R.string.about_section_info),
                sectionIcon = Icons.Default.Info
            ) {
                SettingsItem(
                    onClick = {},
                    title = stringResource(R.string.app_name),
                    subtitle = stringResource(R.string.settings_version, versionName),
                    icon = Icons.Default.Info,
                    trailingContent = null
                )
            }

            // Legal section: privacy policy + open-source licenses
            SettingsMenuSectionCard(
                sectionTitle = stringResource(R.string.about_section_legal),
                sectionIcon = Icons.Default.Policy
            ) {
                SettingsItem(
                    onClick = onNavigateToPrivacyPolicy,
                    title = stringResource(R.string.about_privacy_policy),
                    icon = Icons.Default.Policy
                )
                SettingsItem(
                    onClick = onNavigateToLicenses,
                    title = stringResource(R.string.opensource_licenses),
                    icon = Icons.Default.Info
                )
            }
        }
    }
}

@Composable
private fun AboutAppHeader(versionName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SpacingNormal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SpacingNormal)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_app_logo),
            contentDescription = null,
            modifier = Modifier.size(72.dp)
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.settings_version, versionName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutPreview() {
    BirthdayBuddyTheme {
        AboutScreen(
            onNavigateBack = {},
            onNavigateToPrivacyPolicy = {},
            onNavigateToLicenses = {}
        )
    }
}
