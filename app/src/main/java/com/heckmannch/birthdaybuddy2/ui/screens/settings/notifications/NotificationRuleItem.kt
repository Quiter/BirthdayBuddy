package com.heckmannch.birthdaybuddy2.ui.screens.settings.notifications

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
import androidx.compose.ui.platform.LocalConfiguration
import com.heckmannch.birthdaybuddy2.database.NotificationRule

@Composable
fun NotificationRuleItem(
    rule: NotificationRule,
    onEditRule: (NotificationRule) -> Unit,
    onDeleteRule: (NotificationRule) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val timeStr = String.format(locale, "%02d:%02d Uhr", rule.hour, rule.minute)
    
    val daysStr = when (rule.daysBefore) {
        0 -> "Am Tag selbst"
        1 -> "Einen Tag vorher"
        7 -> "Eine Woche vorher"
        else -> "${rule.daysBefore} Tage vorher"
    }

    ListItem(
        headlineContent = { Text(daysStr) },
        supportingContent = { Text("Um $timeStr") },
        leadingContent = {
            Icon(Icons.Default.Notifications, contentDescription = null)
        },
        trailingContent = {
            IconButton(onClick = { onDeleteRule(rule) }) {
                Icon(Icons.Default.Delete, contentDescription = "Löschen")
            }
        },
        modifier = Modifier.clickable { onEditRule(rule) },
    )
}
