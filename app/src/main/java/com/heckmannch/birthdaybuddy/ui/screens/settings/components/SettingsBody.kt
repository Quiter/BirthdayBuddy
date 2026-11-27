package com.heckmannch.birthdaybuddy.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsColumn
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * Standardized body container for settings menu and detail screens.
 * Handles background color, scrolling, and content padding (including space for floating bottom bars).
 *
 * @param modifier Optional [Modifier] applied to the body container.
 * @param paddingValues Scaffold or external [PaddingValues] to incorporate.
 * @param bottomBarHeight Reserved height at the bottom for floating elements like GlassPillBar.
 * @param scrollable Whether the content should be vertically scrollable inside this body.
 * @param footer Optional footer composable rendered at the end of the scroll content.
 * @param content The main body content.
 */
@Composable
internal fun SettingsBody(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(),
    bottomBarHeight: Dp = 0.dp,
    scrollable: Boolean = true,
    footer: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val combinedPadding = PaddingValues(
        start = paddingValues.calculateStartPadding(layoutDirection),
        top = paddingValues.calculateTopPadding(),
        end = paddingValues.calculateEndPadding(layoutDirection),
        bottom = paddingValues.calculateBottomPadding() + bottomBarHeight
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        if (scrollable) {
            val scrollState = rememberScrollState()
            SettingsColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(combinedPadding)
            ) {
                content()
                footer?.invoke()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(combinedPadding)
            ) {
                content()
                footer?.invoke()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsBodyPreview() {
    BirthdayBuddyTheme {
        SettingsBody {
            // Preview placeholder
        }
    }
}
