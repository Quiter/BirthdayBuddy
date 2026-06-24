package com.heckmannch.birthdaybuddy.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * A standardized Switch component for BirthdayBuddy.
 * It automatically applies the standard Material 3 style and displays a checkmark icon on the thumb when checked.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        thumbContent = if (checked) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        } else null
    )
}

@Preview(showBackground = true)
@Composable
private fun AppSwitchPreview() {
    BirthdayBuddyTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppSwitch(
                checked = true,
                onCheckedChange = {}
            )
            Spacer(modifier = Modifier.height(16.dp))
            AppSwitch(
                checked = false,
                onCheckedChange = {}
            )
            Spacer(modifier = Modifier.height(16.dp))
            AppSwitch(
                checked = true,
                enabled = false,
                onCheckedChange = {}
            )
            Spacer(modifier = Modifier.height(16.dp))
            AppSwitch(
                checked = false,
                enabled = false,
                onCheckedChange = {}
            )
        }
    }
}
