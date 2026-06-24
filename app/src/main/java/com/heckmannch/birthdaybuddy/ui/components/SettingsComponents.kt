package com.heckmannch.birthdaybuddy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * A standardized section header for settings screens.
 *
 * @param title The section title text.
 * @param modifier The modifier to be applied to the text.
 */
@Composable
fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            start = SpacingNormal,
            end = SpacingNormal,
            bottom = SpacingSmall
        )
    )
}

/**
 * A standardized card container for settings screens that groups options together.
 *
 * @param modifier The modifier to be applied to the card.
 * @param content The content of the card (usually a list of rows).
 */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = modifier.fillMaxWidth(),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge),
                content = content
            )
        }
    )
}

/**
 * A standardized settings list row with a title, optional description, leading icon, and trailing switch.
 *
 * @param title The title of the setting.
 * @param checked The checked state of the switch.
 * @param onCheckedChange Action to trigger when the switch state changes.
 * @param modifier The modifier to be applied to the row.
 * @param description Optional description text below the title.
 * @param leadingIcon Optional leading icon.
 * @param enabled Whether this row is enabled and interactive.
 */
@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = description?.let { { Text(it) } },
        leadingContent = leadingIcon,
        trailingContent = {
            AppSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.clickable(enabled = enabled) { onCheckedChange(!checked) }
    )
}

/**
 * A standardized settings list row for clickable actions (like Sync, Backup Import/Export, or custom selectors).
 *
 * @param title The title of the action.
 * @param onClick Action to trigger when clicked.
 * @param modifier The modifier to be applied to the row.
 * @param description Optional description text below the title.
 * @param leadingIcon Optional leading icon.
 * @param trailingContent Optional trailing component.
 * @param enabled Whether this row is enabled and interactive.
 */
@Composable
fun SettingsClickableRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = description?.let { { Text(it) } },
        leadingContent = leadingIcon,
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.clickable(enabled = enabled, onClick = onClick)
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsComponentsPreview() {
    BirthdayBuddyTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingNormal)
        ) {
            SettingsSectionHeader(title = "Benachrichtigungen")
            SettingsCard {
                SettingsSwitchRow(
                    title = "Geburtstags-Erinnerungen",
                    description = "Erhalte Benachrichtigungen an Geburtstagen",
                    checked = true,
                    onCheckedChange = {}
                )
                SettingsClickableRow(
                    title = "Erinnerungszeit",
                    description = "09:00 Uhr",
                    onClick = {}
                )
            }
        }
    }
}
