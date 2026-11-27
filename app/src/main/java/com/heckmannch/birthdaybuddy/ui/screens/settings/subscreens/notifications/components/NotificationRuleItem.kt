package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.notifications.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsGroup
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsItem
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * Expressive segmented card row representing a single scheduled [NotificationRule].
 */
@Composable
fun NotificationRuleItem(
    rule: NotificationRule,
    onEditRule: (NotificationRule) -> Unit,
    onDeleteRule: (NotificationRule) -> Unit,
) {
    val daysStr = when (rule.daysBefore) {
        0 -> stringResource(R.string.rule_today)
        1 -> stringResource(R.string.rule_tomorrow)
        7 -> stringResource(R.string.rule_one_week)
        else -> pluralStringResource(R.plurals.rule_days_before, rule.daysBefore, rule.daysBefore)
    }

    SettingsItem(
        onClick = { onEditRule(rule) },
        title = daysStr,
        subtitle = stringResource(R.string.rule_time_format, rule.hour, rule.minute),
        icon = Icons.Rounded.Schedule,
        trailingContent = {
            IconButton(onClick = { onDeleteRule(rule) }) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.notifications_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun NotificationRuleItemPreview() {
    BirthdayBuddyTheme {
        SettingsGroup {
            NotificationRuleItem(
                rule = NotificationRule(daysBefore = 1, hour = 9, minute = 0),
                onEditRule = {},
                onDeleteRule = {},
            )
        }
    }
}
