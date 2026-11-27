package com.heckmannch.birthdaybuddy.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.settings.settingsPageBackground
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSettingsEmptyIcon
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSettingsInfoStatContainer
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusLargeIncreased
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeLarge
import com.heckmannch.birthdaybuddy.ui.theme.SettingsAboutLogoSize
import com.heckmannch.birthdaybuddy.ui.theme.SpacingLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

/**
 * Renders the detail pane placeholder when no specific settings tab is selected
 * (e.g., in tablet multi-pane mode).
 */
@Composable
internal fun SettingsDetailPlaceholder(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(settingsPageBackground())
            .padding(SpacingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SpacingNormal, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(SettingsAboutLogoSize)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = AlphaSettingsInfoStatContainer),
                    shape = RoundedCornerShape(CornerRadiusLargeIncreased),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                modifier = Modifier.size(IconSizeLarge),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = AlphaSettingsEmptyIcon),
            )
        }
        Text(
            text = stringResource(R.string.settings_select_item_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsDetailPlaceholderPreview() {
    BirthdayBuddyTheme {
        SettingsDetailPlaceholder()
    }
}
