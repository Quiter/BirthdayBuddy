package com.heckmannch.birthdaybuddy.ui.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsItem
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsMenuItemData
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsSectionType
import com.heckmannch.birthdaybuddy.ui.screens.settings.rememberSettingsMenuItems
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * Menu content displaying settings cards grouped by section (Theme, Sync, About).
 * Designed to be placed inside a [SettingsBody].
 *
 * @param menuItems List of menu items to render.
 * @param onItemClick Callback when a menu item is clicked.
 * @param modifier Optional [Modifier].
 */
@Composable
internal fun SettingsMenuList(
    menuItems: List<SettingsMenuItemData>,
    onItemClick: (SettingsMenuItemData) -> Unit,
    labelsEnabled: Boolean,
    onLabelsEnabledChanged: (Boolean) -> Unit,
    otherEventsEnabled: Boolean,
    onOtherEventsEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val generalItems = remember(menuItems) { menuItems.filter { it.section == SettingsSectionType.GENERAL } }
    SettingsMenuSectionCard(
        sectionTitle = stringResource(R.string.settings_theme_title),
        sectionIcon = Icons.Default.Palette,
        modifier = modifier,
    ) {
        generalItems.forEach { item ->
            SettingsItem(
                onClick = { onItemClick(item) },
                title = stringResource(item.titleRes),
                subtitle = stringResource(item.descRes),
                icon = item.icon
            )
        }
        LabelManagementExpandableCard(
            labelsEnabled = labelsEnabled,
            onLabelsEnabledChanged = onLabelsEnabledChanged
        )
        OtherEventsManagementExpandableCard(
            otherEventsEnabled = otherEventsEnabled,
            onOtherEventsEnabledChanged = onOtherEventsEnabledChanged
        )
    }

    val dataItems = remember(menuItems) { menuItems.filter { it.section == SettingsSectionType.DATA } }
    SettingsMenuSectionCard(
        sectionTitle = stringResource(R.string.settings_sync_title),
        sectionIcon = Icons.Default.Refresh,
        items = dataItems,
        onItemClick = onItemClick,
    )

    val aboutItems = remember(menuItems) { menuItems.filter { it.section == SettingsSectionType.ABOUT } }
    SettingsMenuSectionCard(
        sectionTitle = stringResource(R.string.settings_about_title),
        sectionIcon = Icons.Default.Info,
        items = aboutItems,
        onItemClick = onItemClick,
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsMenuListPreview() {
    BirthdayBuddyTheme {
        SettingsBody {
            SettingsMenuList(
                menuItems = rememberSettingsMenuItems(),
                onItemClick = {},
                labelsEnabled = true,
                onLabelsEnabledChanged = {},
                otherEventsEnabled = true,
                onOtherEventsEnabledChanged = {}
            )
        }
    }
}
