package com.heckmannch.birthdaybuddy.ui.screens.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.ui.components.SettingsGroup
import com.heckmannch.birthdaybuddy.ui.components.SettingsItem
import com.heckmannch.birthdaybuddy.ui.components.SettingsSection
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsMenuItemData

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

    SettingsSection(
        title = sectionTitle,
        icon = sectionIcon,
        modifier = modifier
    ) {
        SettingsGroup {
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
}
