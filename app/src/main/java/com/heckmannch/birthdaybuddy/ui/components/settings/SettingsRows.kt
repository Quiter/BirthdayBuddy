package com.heckmannch.birthdaybuddy.ui.components.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisDisabled
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisHigh
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSettingsOutlineMuted
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSettingsSubtitleMuted
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThin
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusLargeIncreased
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeNormal
import com.heckmannch.birthdaybuddy.ui.theme.SettingsCardElevationLow
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingTiny

/**
 * Resolves the surface fill color for settings cards with crisp contrast across
 * Light, Dark, and AMOLED Black themes.
 */
@Composable
fun cardFill(): Color {
    val colorScheme = MaterialTheme.colorScheme
    return if (colorScheme.isDarkSurface()) {
        colorScheme.surfaceContainer
    } else {
        colorScheme.surfaceContainerLowest
    }
}

/**
 * Reusable icon component with standard settings styling.
 */
@Composable
fun ThemedIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = IconSizeNormal,
    tint: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size),
    )
}

/**
 * Forward chevron icon pointing towards subpages or details.
 */
@Composable
fun ForwardChevronIcon(
    modifier: Modifier = Modifier,
    size: Dp = IconSizeNormal,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaSettingsSubtitleMuted),
) {
    ThemedIcon(
        icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        modifier = modifier,
        size = size,
        tint = tint,
    )
}

/**
 * Expressive Material 3 toggle switch displaying a checkmark when enabled and a close mark
 * when disabled inside the thumb.
 */
@Composable
fun ToggleSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colorScheme = MaterialTheme.colorScheme
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                contentDescription = null,
                tint = when {
                    !enabled -> colorScheme.onSurface.copy(alpha = AlphaEmphasisDisabled)
                    checked -> colorScheme.primary
                    else -> colorScheme.surfaceContainerHighest
                },
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
    )
}

/**
 * Segmented surface card that automatically connects its corners with sibling cards inside a
 * [SettingsGroup] (or an explicit [ListSegment]) and responds to presses with corner morphing.
 */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    segment: ListSegment? = null,
    containerColor: Color = cardFill(),
    contentPadding: PaddingValues = PaddingValues(horizontal = SpacingNormal, vertical = SpacingMedium),
    content: @Composable () -> Unit,
) {
    val corners = rememberSegmentCorners(segment)
    val interactionSource = remember { MutableInteractionSource() }
    val shape = if (onClick != null) {
        rememberPressShape(base = corners, interactionSource = interactionSource)
    } else {
        animateCornersAsState(corners)
    }
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AlphaSettingsOutlineMuted)

    val interactiveModifier = if (onClick != null) {
        Modifier.shapedClickable(
            shape = shape,
            interactionSource = interactionSource,
            enabled = enabled,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) AlphaEmphasisHigh else AlphaEmphasisDisabled)
            .background(containerColor, shape)
            .border(BorderStroke(BorderWidthThin, borderColor), shape)
            .then(interactiveModifier)
            .padding(contentPadding),
    ) {
        content()
    }
}

/**
 * Segmented toggleable surface card used by [SettingsSwitchItem] so the entire card participates
 * in press corner morphing, ripple feedback, and accessible toggle semantics.
 */
@Composable
private fun ToggleableSurfaceCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    segment: ListSegment? = null,
    containerColor: Color = cardFill(),
    contentPadding: PaddingValues = PaddingValues(horizontal = SpacingNormal, vertical = SpacingMedium),
    content: @Composable () -> Unit,
) {
    val corners = rememberSegmentCorners(segment)
    val interactionSource = remember { MutableInteractionSource() }
    val shape = rememberPressShape(base = corners, interactionSource = interactionSource)
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AlphaSettingsOutlineMuted)
    val stateDesc = stringResource(
        if (checked) R.string.settings_state_enabled else R.string.settings_state_disabled
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) AlphaEmphasisHigh else AlphaEmphasisDisabled)
            .background(containerColor, shape)
            .border(BorderStroke(BorderWidthThin, borderColor), shape)
            .shapedToggleable(
                value = checked,
                shape = shape,
                interactionSource = interactionSource,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics { stateDescription = stateDesc }
            .padding(contentPadding),
    ) {
        content()
    }
}

/**
 * General-purpose segmented card for arbitrary custom content inside a [SettingsGroup].
 */
@Composable
fun SettingsItemCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    segment: ListSegment? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = SpacingNormal, vertical = SpacingNormal),
    content: @Composable () -> Unit,
) {
    SurfaceCard(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        segment = segment,
        contentPadding = contentPadding,
        content = content,
    )
}

/**
 * Standalone rounded container card for hero headers, selectors, and instructional blocks.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    containerColor: Color = cardFill(),
    contentPadding: PaddingValues = PaddingValues(SpacingNormal),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(CornerRadiusLargeIncreased)
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AlphaSettingsOutlineMuted)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
        tonalElevation = SettingsCardElevationLow,
        border = BorderStroke(BorderWidthThin, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content,
        )
    }
}

/**
 * Row with optional leading visual, title, description, and trailing content.
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
    descriptionColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaSettingsSubtitleMuted),
    trailingContent: @Composable (() -> Unit)? = null,
    spacing: Dp = SpacingNormal,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingContent?.invoke()

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SpacingTiny),
        ) {
            Text(
                text = title,
                style = titleStyle,
                fontWeight = titleWeight,
                color = titleColor,
            )
            description?.let {
                Text(
                    text = it,
                    style = descriptionStyle,
                    color = descriptionColor,
                )
            }
        }

        trailingContent?.invoke()
    }
}

/**
 * Standard expressive segmented settings row. Automatically registers with [LocalSettingsSearch]
 * and connects with sibling rows inside a [SettingsGroup].
 */
@Composable
fun SettingsItem(
    onClick: (() -> Unit)?,
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
    segment: ListSegment? = null,
    searchKeywords: List<String> = emptyList(),
    statusContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = if (onClick != null) {
        { ForwardChevronIcon() }
    } else {
        null
    },
) {
    val searchTerms = remember(title, subtitle, searchKeywords) {
        buildList {
            add(title)
            if (subtitle != null) add(subtitle)
            addAll(searchKeywords)
        }.toTypedArray()
    }

    SearchableSetting(*searchTerms) {
        SurfaceCard(
            modifier = modifier,
            onClick = onClick,
            enabled = enabled,
            segment = segment,
        ) {
            IconTextRow(
                leadingContent = leadingContent ?: icon?.let { vector ->
                    { ThemedIcon(icon = vector) }
                },
                title = title,
                description = subtitle,
                trailingContent = statusContent ?: trailingContent,
            )
        }
    }
}

/**
 * Expressive segmented switch row for boolean settings. Automatically registers with
 * [LocalSettingsSearch] and connects with sibling rows inside a [SettingsGroup].
 */
@Composable
fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    segment: ListSegment? = null,
    searchKeywords: List<String> = emptyList(),
) {
    val searchTerms = remember(title, subtitle, searchKeywords) {
        buildList {
            add(title)
            if (subtitle != null) add(subtitle)
            addAll(searchKeywords)
        }.toTypedArray()
    }

    SearchableSetting(*searchTerms) {
        ToggleableSurfaceCard(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            segment = segment,
        ) {
            IconTextRow(
                leadingContent = leadingContent ?: icon?.let { vector ->
                    { ThemedIcon(icon = vector) }
                },
                title = title,
                description = subtitle,
                trailingContent = {
                    ToggleSwitch(
                        checked = checked,
                        onCheckedChange = null,
                        enabled = enabled,
                    )
                },
            )
        }
    }
}
