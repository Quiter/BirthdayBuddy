package com.heckmannch.birthdaybuddy.ui.screens.settings.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsGroup
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsItem
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSection
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsMenuItemData
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsSectionType
import com.heckmannch.birthdaybuddy.ui.screens.settings.rememberSettingsMenuItems
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * Flexible settings section card used throughout settings detail screens.
 *
 * Renders a labeled section header (with an optional icon, description, and trailing action slot)
 * above a grouped block of arbitrary content. This is the primary container for all
 * settings sub-screen sections.
 *
 * @param sectionTitle Required section heading text.
 * @param modifier Optional [Modifier].
 * @param sectionIcon Optional leading icon displayed next to the title.
 * @param sectionDescription Optional explanatory subtitle shown below the title.
 * @param actionContent Optional trailing composable in the header row (e.g., an add-button or badge).
 * @param content Arbitrary [ColumnScope] content rendered inside the card group.
 */
@Composable
internal fun SettingsMenuSectionCard(
    sectionTitle: String,
    modifier: Modifier = Modifier,
    sectionIcon: ImageVector? = null,
    sectionDescription: String? = null,
    actionContent: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    SettingsSection(
        title = sectionTitle,
        icon = sectionIcon,
        description = sectionDescription,
        actionContent = actionContent,
        modifier = modifier
    ) {
        SettingsGroup {
            content()
        }
    }
}

/**
 * Convenience overload for rendering a list of [SettingsMenuItemData] items inside a section card.
 *
 * @param sectionTitle Required section heading text.
 * @param sectionIcon Optional leading icon.
 * @param items List of menu items to render.
 * @param onItemClick Callback when an item is clicked.
 * @param modifier Optional [Modifier].
 */
@Composable
internal fun SettingsMenuSectionCard(
    modifier: Modifier = Modifier,
    sectionTitle: String,
    sectionIcon: ImageVector? = null,
    items: List<SettingsMenuItemData>,
    onItemClick: (SettingsMenuItemData) -> Unit,
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

@Preview(showBackground = true, name = "Section Card – Items list")
@Composable
private fun SettingsMenuSectionCardItemsPreview() {
    BirthdayBuddyTheme {
        SettingsMenuSectionCard(
            sectionTitle = stringResource(R.string.settings_theme_title),
            sectionIcon = Icons.Default.Palette,
            items = rememberSettingsMenuItems().filter { it.section == SettingsSectionType.GENERAL },
            onItemClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Section Card – Free content with actionContent")
@Composable
private fun SettingsMenuSectionCardFreePreview() {
    BirthdayBuddyTheme {
        SettingsMenuSectionCard(
            sectionTitle = stringResource(R.string.notifications_planned_header),
            sectionIcon = Icons.Default.Notifications,
            sectionDescription = stringResource(R.string.notifications_desc),
            actionContent = {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null
                    )
                }
            }
        ) {
            SettingsItem(
                onClick = {},
                title = "8:00 Uhr – Today",
                subtitle = "On the day itself"
            )
        }
    }
}
