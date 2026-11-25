package com.heckmannch.birthdaybuddy.ui.screens.labels.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThin

/**
 * Selection indicator circle showing checkmark when selected.
 */
@Composable
internal fun SelectionCheckIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = if (selected) colors.primary else colors.surfaceContainerHigh,
        border = if (selected) null else BorderStroke(BorderWidthThin, colors.outline),
        modifier = modifier.size(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectionCheckIndicatorSelectedPreview() {
    BirthdayBuddyTheme {
        SelectionCheckIndicator(selected = true)
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectionCheckIndicatorUnselectedPreview() {
    BirthdayBuddyTheme {
        SelectionCheckIndicator(selected = false)
    }
}
