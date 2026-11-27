package com.heckmannch.birthdaybuddy.ui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppSwitch
import com.heckmannch.birthdaybuddy.ui.components.cards.GlassCardExpandButton
import com.heckmannch.birthdaybuddy.ui.components.cards.GlassCardLayout
import com.heckmannch.birthdaybuddy.ui.components.cards.GlassCardScope
import com.heckmannch.birthdaybuddy.ui.components.cards.GlassCardStyle
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingTiny
import com.heckmannch.birthdaybuddy.ui.theme.defaultCardGradient

/**
 * Reusable icon component with standard settings styling.
 */
@Composable
fun ThemedIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = IconSizeNormal,
    tint: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size)
    )
}

/**
 * Row with icon, title, description, and trailing content.
 */
@Composable
fun IconTextRow(
    modifier: Modifier = Modifier,
    leadingContent: @Composable (() -> Unit)? = null,
    title: String,
    description: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    titleWeight: FontWeight = FontWeight.Medium,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    descriptionStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    descriptionColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailingContent: @Composable (() -> Unit)? = null,
    spacing: Dp = SpacingNormal
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingContent?.invoke()

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SpacingTiny)
        ) {
            Text(
                text = title,
                style = titleStyle,
                fontWeight = titleWeight,
                color = titleColor
            )
            description?.let {
                Text(
                    text = it,
                    style = descriptionStyle,
                    color = descriptionColor
                )
            }
        }

        trailingContent?.invoke()
    }
}

/**
 * Forward chevron icon pointing towards subpages or details.
 */
@Composable
fun ForwardChevronIcon(
    modifier: Modifier = Modifier,
    size: Dp = IconSizeNormal,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    ThemedIcon(
        icon = Icons.AutoMirrored.Filled.NavigateNext,
        modifier = modifier,
        size = size,
        tint = tint
    )
}

@Composable
private fun GlassCardScope.SettingsTitleContent(
    title: String,
    subtitle: String?
) {
    Column(
        modifier = with(row) { Modifier.weight(1f) },
        verticalArrangement = Arrangement.spacedBy(style.textSpacing)
    ) {
        Text(
            text = title,
            style = style.titleStyle,
            color = style.titleColor
        )
        subtitle?.let {
            Text(
                text = it,
                style = style.subtitleStyle,
                color = style.subtitleColor
            )
        }
    }
}

/**
 * Standard settings item card built on the central [GlassCardLayout].
 * Supports click-to-navigate to a settings detail sub-screen or action.
 */
@Composable
fun SettingsItem(
    onClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
    gradientColors: List<Color> = defaultCardGradient(),
    statusContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = { ForwardChevronIcon() }
) {
    GlassCardLayout(
        modifier = modifier,
        gradientColors = gradientColors,
        onClick = onClick,
        enabled = enabled,
        trailing = statusContent?.let { content -> { content() } }
            ?: trailingContent?.let { content -> { content() } }
    ) {
        leadingContent?.invoke() ?: icon?.let { ThemedIcon(icon = it) }
        SettingsTitleContent(title = title, subtitle = subtitle)
    }
}

/**
 * Expandable settings card built on the central [GlassCardLayout].
 * Enables inline expansion directly inside Settings screens alongside clickable detail cards.
 */
@Composable
fun SettingsExpandableCard(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    gradientColors: List<Color> = defaultCardGradient(),
    expandedContent: @Composable ColumnScope.(GlassCardStyle) -> Unit
) {
    GlassCardLayout(
        modifier = modifier,
        gradientColors = gradientColors,
        onClick = { onExpandedChange(!expanded) },
        enabled = enabled,
        expanded = expanded,
        trailing = {
            GlassCardExpandButton(
                expanded = expanded,
                onToggle = { onExpandedChange(!expanded) },
                style = style,
                contentDescription = stringResource(
                    if (expanded) R.string.labels_collapse_card else R.string.labels_expand_card
                )
            )
        },
        expandedContent = expandedContent
    ) {
        leadingContent?.invoke() ?: icon?.let { ThemedIcon(icon = it) }
        SettingsTitleContent(title = title, subtitle = subtitle)
    }
}

/**
 * Clickable settings row card for detail screens built on [GlassCardLayout].
 */
@Composable
fun SettingsClickableRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    gradientColors: List<Color> = defaultCardGradient()
) {
    GlassCardLayout(
        modifier = modifier,
        gradientColors = gradientColors,
        onClick = onClick,
        enabled = enabled,
        trailing = trailingContent?.let { content -> { content() } }
    ) {
        leadingIcon?.invoke()
        SettingsTitleContent(title = title, subtitle = description)
    }
}

/**
 * Settings switch row card for detail screens built on [GlassCardLayout].
 */
@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    gradientColors: List<Color> = defaultCardGradient()
) {
    GlassCardLayout(
        modifier = modifier,
        gradientColors = gradientColors,
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
        trailing = {
            AppSwitch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled
            )
        }
    ) {
        leadingIcon?.invoke()
        SettingsTitleContent(title = title, subtitle = description)
    }
}

/**
 * Inline switch row intended to be placed inside an expanded [SettingsExpandableCard]
 * without nesting a second frosted glass card border.
 */
@Composable
fun SettingsInlineSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    cardStyle: GlassCardStyle,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true
) {
    IconTextRow(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = SpacingExtraSmall),
        title = title,
        description = description,
        titleStyle = cardStyle.titleStyle,
        titleWeight = FontWeight.Medium,
        titleColor = cardStyle.titleColor,
        descriptionStyle = cardStyle.subtitleStyle,
        descriptionColor = cardStyle.subtitleColor,
        trailingContent = {
            AppSwitch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled
            )
        }
    )
}
