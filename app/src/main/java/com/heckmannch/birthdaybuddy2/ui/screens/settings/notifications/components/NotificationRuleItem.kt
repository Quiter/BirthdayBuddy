package com.heckmannch.birthdaybuddy2.ui.screens.settings.notifications.components

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy2.R
import com.heckmannch.birthdaybuddy2.database.NotificationRule

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

    ListItem(
        headlineContent = { Text(daysStr) },
        supportingContent = { 
            Text(stringResource(R.string.rule_time_format, rule.hour, rule.minute)) 
        },
        leadingContent = {
            Icon(Icons.Default.Notifications, contentDescription = null)
        },
        trailingContent = {
            IconButton(onClick = { onDeleteRule(rule) }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.notifications_delete))
            }
        },
        modifier = Modifier.clickable { onEditRule(rule) },
    )
}
