package com.heckmannch.birthdaybuddy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.screens.settings.components.SettingsHeader
import com.heckmannch.birthdaybuddy.ui.screens.home.components.cards.CardBorder
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisLow
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * Surface fill color for settings cards adhering to Morphe architecture and Material 3 guidelines.
 */
@Composable
fun cardFill(): Color = MaterialTheme.colorScheme.surfaceContainer

/**
 * Elevated card container with subtle glassmorphism outline border (`CardBorder`).
 * Modeled after Morphe Manager's `SurfaceCard` and `CardBorder`.
 */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    elevation: Dp = 1.dp,
    cornerRadius: Dp = 20.dp,
    showBorder: Boolean = true,
    color: Color = cardFill(),
    shape: CornerBasedShape = RoundedCornerShape(cornerRadius),
    content: @Composable () -> Unit
) {
    val border = if (showBorder) CardBorder.neutral else null

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled, onClick = onClick)
                } else Modifier
            ),
        shape = shape,
        color = color,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = elevation,
        shadowElevation = 0.dp,
        border = border
    ) {
        content()
    }
}

/**
 * Settings card container for grouping options together.
 */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    SurfaceCard(
        modifier = modifier,
        showBorder = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            content = content
        )
    }
}

/**
 * Settings item card wrapper.
 */
@Composable
fun SettingsItemCard(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showBorder: Boolean = true,
    color: Color = cardFill(),
    shape: CornerBasedShape = RoundedCornerShape(20.dp),
    content: @Composable () -> Unit
) {
    SurfaceCard(
        onClick = onClick,
        enabled = enabled,
        elevation = 1.dp,
        cornerRadius = 20.dp,
        showBorder = showBorder,
        color = color,
        shape = shape,
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Horizontal divider for settings sections.
 */
@Composable
fun SettingsDivider(
    modifier: Modifier = Modifier,
    fullWidth: Boolean = false
) {
    HorizontalDivider(
        modifier = if (fullWidth) modifier else modifier.padding(horizontal = SpacingNormal),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AlphaEmphasisLow)
    )
}

/**
 * Reusable icon component with standard styling.
 */
@Composable
fun ThemedIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
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
            verticalArrangement = Arrangement.spacedBy(2.dp)
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
    size: Dp = 24.dp,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    ThemedIcon(
        icon = Icons.AutoMirrored.Filled.NavigateNext,
        modifier = modifier,
        size = size,
        tint = tint
    )
}

/**
 * Standard settings item row.
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
    showBorder: Boolean = true,
    statusContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = { ForwardChevronIcon() }
) {
    SettingsItemCard(
        onClick = onClick,
        enabled = enabled,
        showBorder = showBorder,
        modifier = modifier
    ) {
        IconTextRow(
            modifier = Modifier.padding(SpacingNormal),
            leadingContent = leadingContent ?: icon?.let { { ThemedIcon(icon = it) } },
            title = title,
            description = subtitle,
            trailingContent = statusContent ?: trailingContent
        )
    }
}

/**
 * Clickable settings row for detail screens.
 */
@Composable
fun SettingsClickableRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    enabled: Boolean = true
) {
    SettingsItemCard(
        onClick = onClick,
        enabled = enabled,
        showBorder = false,
        modifier = modifier
    ) {
        IconTextRow(
            modifier = Modifier.padding(SpacingNormal),
            leadingContent = leadingIcon,
            title = title,
            description = description,
            trailingContent = trailingContent
        )
    }
}

/**
 * Settings switch item row.
 */
@Composable
fun SettingsSwitchItem(
    checked: Boolean,
    onToggle: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    subtitle: String? = null,
    showBorder: Boolean = true
) {
    SettingsItem(
        onClick = onToggle,
        title = title,
        modifier = modifier,
        icon = icon,
        leadingContent = leadingContent,
        subtitle = subtitle,
        showBorder = showBorder,
        trailingContent = {
            AppSwitch(
                checked = checked,
                onCheckedChange = null
            )
        }
    )
}

/**
 * Settings switch row for detail screens inside a [SettingsCard].
 */
@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true
) {
    SettingsItemCard(
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
        showBorder = false,
        modifier = modifier
    ) {
        IconTextRow(
            modifier = Modifier.padding(SpacingNormal),
            leadingContent = leadingIcon,
            title = title,
            description = description,
            trailingContent = {
                AppSwitch(
                    checked = checked,
                    onCheckedChange = null,
                    enabled = enabled
                )
            }
        )
    }
}

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
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
                ThemedIcon(icon = icon, size = 20.dp, tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingNormal),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    AppResponsiveScaffold(
        modifier = modifier,
        snackbarHost = snackbarHost,
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
