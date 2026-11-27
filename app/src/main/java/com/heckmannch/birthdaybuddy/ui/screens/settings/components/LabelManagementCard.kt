package com.heckmannch.birthdaybuddy.ui.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsExpandableCard
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsInlineSwitchRow

/**
 * Expandable settings card for label management built on the central glass card system.
 */
@Composable
internal fun LabelManagementExpandableCard(
    labelsEnabled: Boolean,
    onLabelsEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    SettingsExpandableCard(
        title = stringResource(R.string.settings_labels_title),
        subtitle = stringResource(R.string.settings_labels_desc),
        icon = Icons.AutoMirrored.Filled.Label,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) { cardStyle ->
        SettingsInlineSwitchRow(
            title = stringResource(R.string.labels_enable),
            description = stringResource(R.string.labels_enable_desc),
            checked = labelsEnabled,
            onCheckedChange = onLabelsEnabledChanged,
            cardStyle = cardStyle
        )
    }
}
