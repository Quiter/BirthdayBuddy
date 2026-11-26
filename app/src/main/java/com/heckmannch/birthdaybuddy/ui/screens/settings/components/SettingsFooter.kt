package com.heckmannch.birthdaybuddy.ui.screens.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisMedium
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

/**
 * Standardized footer component for settings screens.
 * Displays optional footer text (e.g. app version or disclaimer), custom content,
 * and handles bottom spacing for the floating GlassPillBar.
 *
 * @param modifier Optional [Modifier].
 * @param text Optional text displayed in the footer (e.g. "BirthdayBuddy v1.0.0").
 * @param bottomBarHeight Reserved bottom padding for floating navigation bar.
 * @param content Optional custom slot for extra footer elements (e.g. links, buttons).
 */
@Composable
internal fun SettingsFooter(
    modifier: Modifier = Modifier,
    text: String? = null,
    bottomBarHeight: Dp = 0.dp,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = SpacingLarge,
                bottom = SpacingNormal + bottomBarHeight
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!text.isNullOrBlank()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaEmphasisMedium)
            )
        }
        content?.invoke(this)
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsFooterPreview() {
    BirthdayBuddyTheme {
        SettingsFooter(
            text = "BirthdayBuddy v1.0.0",
            bottomBarHeight = 64.dp
        )
    }
}
