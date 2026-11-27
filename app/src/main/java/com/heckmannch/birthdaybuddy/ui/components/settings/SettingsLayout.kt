package com.heckmannch.birthdaybuddy.ui.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.heckmannch.birthdaybuddy.ui.components.layout.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.screens.settings.components.SettingsHeader
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingTiny

/**
 * Group container for settings items.
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SpacingExtraSmall),
        verticalArrangement = Arrangement.spacedBy(SpacingSmall),
        content = content
    )
}

/**
 * Titled section container for settings.
 */
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    description: String? = null,
    actionContent: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = SpacingNormal)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = SpacingNormal, end = SpacingNormal, bottom = SpacingSmall)
                .semantics { heading() },
            horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                ThemedIcon(icon = icon, size = IconSizeMedium, tint = MaterialTheme.colorScheme.primary)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SpacingTiny)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            actionContent?.invoke()
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingNormal),
            verticalArrangement = Arrangement.spacedBy(SpacingSmall),
            content = content
        )
    }
}

/**
 * Vertical column container for settings sections.
 */
@Composable
fun SettingsColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SpacingNormal),
        verticalArrangement = Arrangement.spacedBy(SpacingNormal),
        content = content
    )
}

/**
 * Standardized scaffold layout for settings detail screens.
 */
@Composable
fun SettingsDetailScaffold(
    modifier: Modifier = Modifier,
    title: String,
    onNavigateBack: () -> Unit,
    showBackButton: Boolean = true,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    AppResponsiveScaffold(
        modifier = modifier,
        snackbarHost = snackbarHost,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            SettingsHeader(
                title = title,
                showBackButton = showBackButton,
                onNavigateBack = onNavigateBack,
                showSearchButton = false
            )
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}
