package com.heckmannch.birthdaybuddy.ui.screens.labels.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.bars.AppTopHeader
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * Top header component for the Labels screen, inheriting layout and design from [AppTopHeader].
 *
 * Specific Labels screen elements:
 * 1. Screen title ("Label-Verwaltung").
 * 2. Action button: Add label (+) with haptic feedback.
 * 3. Search action button disabled by default (`showSearchButton = false`).
 *
 * @param onAddLabel Callback when user taps the add label action button.
 * @param modifier Optional [Modifier].
 */
@Composable
fun LabelsHeader(
    onAddLabel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    AppTopHeader(
        title = stringResource(R.string.labels_screen_title),
        modifier = modifier.testTag("labels_top_header"),
        showSearchButton = false,
        actions = {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddLabel()
                },
                modifier = Modifier.testTag("add_label_button"),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.labels_add_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun LabelsHeaderPreview() {
    BirthdayBuddyTheme {
        LabelsHeader(
            onAddLabel = {},
        )
    }
}
