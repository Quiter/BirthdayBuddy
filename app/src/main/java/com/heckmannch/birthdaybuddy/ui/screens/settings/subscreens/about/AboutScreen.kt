package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.about

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.SettingsDetailScaffold
import com.heckmannch.birthdaybuddy.ui.components.withSettingsInsets
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.about.components.AboutAppHeader
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraLarge
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

    SettingsDetailScaffold(
        title = stringResource(R.string.settings_about_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues.withSettingsInsets()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AboutAppHeader(versionName = versionName)

            Spacer(modifier = Modifier.height(SpacingExtraLarge))

            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = SpacingNormal)
            )

            Spacer(modifier = Modifier.height(SpacingExtraLarge))

            TextButton(onClick = onNavigateToPrivacyPolicy) {
                Text(
                    text = stringResource(R.string.about_privacy_policy),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(SpacingNormal))

            TextButton(onClick = onNavigateToLicenses) {
                Text(
                    text = stringResource(R.string.opensource_licenses),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(SpacingExtraLarge))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.settings_version, versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(SpacingNormal))
            Text(
                text = stringResource(R.string.settings_made_with),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(SpacingNormal))
            Text(
                text = stringResource(R.string.about_copyright),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

