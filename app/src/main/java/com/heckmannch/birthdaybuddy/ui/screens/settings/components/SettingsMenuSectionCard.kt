package com.heckmannch.birthdaybuddy.ui.screens.settings.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.SettingsGroup
import com.heckmannch.birthdaybuddy.ui.components.SettingsItem
import com.heckmannch.birthdaybuddy.ui.components.SettingsSection
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsMenuItemData
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsSectionType
import com.heckmannch.birthdaybuddy.ui.screens.settings.rememberSettingsMenuItems
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * Renders a settings section card containing a header and a group of items.
 */
@Composable
internal fun SettingsMenuSectionCard(
    sectionTitle: String,
    sectionIcon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    SettingsSection(
        title = sectionTitle,
        icon = sectionIcon,
        modifier = modifier
    ) {
        SettingsGroup {
            content()
        }
    }
}

/**
 * Renders a settings section card containing a header and a group of menu items.
 */
@Composable
internal fun SettingsMenuSectionCard(
    sectionTitle: String,
    sectionIcon: ImageVector,
    items: List<SettingsMenuItemData>,
    onItemClick: (SettingsMenuItemData) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    SettingsMenuSectionCard(
        sectionTitle = sectionTitle,
        sectionIcon = sectionIcon,
        modifier = modifier
    ) {
        items.forEach { item ->
            SettingsItem(
                onClick = { onItemClick(item) },
                title = stringResource(item.titleRes),
                subtitle = stringResource(item.descRes),
                icon = item.icon
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsMenuSectionCardPreview() {
    BirthdayBuddyTheme {
        SettingsMenuSectionCard(
            sectionTitle = stringResource(R.string.settings_theme_title),
            sectionIcon = Icons.Default.Palette,
            items = rememberSettingsMenuItems().filter { it.section == SettingsSectionType.GENERAL },
            onItemClick = {}
        )
    }
}
