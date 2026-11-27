package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.BuildConfig
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.settings.SectionCard
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsGroup
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsItem
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSection
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSubpageScaffold
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSettingsSubtitleMuted
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusLarge
import com.heckmannch.birthdaybuddy.ui.theme.SettingsAboutLogoSize
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * Expressive About subpage displaying app identity, version, legal links, and credits.
 */
@Composable
fun AboutScreen(
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToLicenses: () -> Unit,
) {
    val versionName = BuildConfig.VERSION_NAME

    SettingsSubpageScaffold(
        title = stringResource(R.string.settings_about_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack,
    ) {
        SectionCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SpacingSmall),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SpacingMedium),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_app_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(SettingsAboutLogoSize)
                        .clip(RoundedCornerShape(CornerRadiusLarge)),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SpacingExtraSmall),
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.settings_version, versionName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Text(
                    text = stringResource(R.string.about_description),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SettingsSection(title = stringResource(R.string.about_section_info)) {
            SettingsGroup {
                SettingsItem(
                    onClick = null,
                    title = stringResource(R.string.app_name),
                    subtitle = stringResource(R.string.settings_version, versionName),
                    icon = Icons.Rounded.Info,
                )
            }
        }

        SettingsSection(title = stringResource(R.string.about_section_legal)) {
            SettingsGroup {
                SettingsItem(
                    onClick = onNavigateToPrivacyPolicy,
                    title = stringResource(R.string.about_privacy_policy),
                    icon = Icons.Rounded.Policy,
                )
                SettingsItem(
                    onClick = onNavigateToLicenses,
                    title = stringResource(R.string.opensource_licenses),
                    icon = Icons.Rounded.Description,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = SpacingSmall, bottom = SpacingNormal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SpacingExtraSmall),
        ) {
            Text(
                text = stringResource(R.string.settings_made_with),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaSettingsSubtitleMuted),
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.about_copyright),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaSettingsSubtitleMuted),
                textAlign = TextAlign.Center,
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
            onNavigateToLicenses = {},
        )
    }
}
