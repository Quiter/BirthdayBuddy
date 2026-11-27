package com.heckmannch.birthdaybuddy.ui.components.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.heckmannch.birthdaybuddy.ui.theme.AlphaContainerLow
import com.heckmannch.birthdaybuddy.ui.theme.AlphaContainerSubtle
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSettingsEmptyIcon
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSettingsInfoStatContainer
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSettingsNoticeDivider
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSettingsOutlineMuted
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSettingsSubtitleMuted
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThin
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusLarge
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusLargeIncreased
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeMedium
import com.heckmannch.birthdaybuddy.ui.theme.SettingsEmptyStateIconSize
import com.heckmannch.birthdaybuddy.ui.theme.SettingsNoticeActionMinHeight
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingTiny

/**
 * Semantic color tones for [SettingsNotice] containers.
 */
enum class SettingsSemanticTone {
    Neutral,
    Primary,
    Warning,
    Error,
}

/**
 * Muted explanatory caption placed underneath a [SettingsGroup] that automatically hides
 * while a page-level search is active.
 */
@Composable
fun SettingsFooter(
    text: String,
    modifier: Modifier = Modifier,
) {
    val search = LocalSettingsSearch.current
    if (search?.isActive == true) return
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaSettingsSubtitleMuted),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = SpacingExtraSmall)
            .padding(horizontal = SpacingExtraSmall),
    )
}

/**
 * Expressive tinted callout block for notices, permissions, and contextual guidance.
 */
@Composable
fun SettingsNotice(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    tone: SettingsSemanticTone = SettingsSemanticTone.Neutral,
    action: (@Composable () -> Unit)? = null,
) {
    val colorScheme = MaterialTheme.colorScheme
    val (containerColor, accentColor, borderColor) = when (tone) {
        SettingsSemanticTone.Neutral -> Triple(
            colorScheme.surfaceContainerHigh.copy(alpha = AlphaSettingsInfoStatContainer + AlphaSettingsInfoStatContainer),
            colorScheme.onSurfaceVariant,
            colorScheme.outlineVariant.copy(alpha = AlphaSettingsOutlineMuted),
        )
        SettingsSemanticTone.Primary -> Triple(
            colorScheme.primaryContainer.copy(alpha = AlphaContainerSubtle + AlphaContainerLow),
            colorScheme.primary,
            colorScheme.primary.copy(alpha = AlphaSettingsNoticeDivider),
        )
        SettingsSemanticTone.Warning -> Triple(
            colorScheme.tertiaryContainer.copy(alpha = AlphaContainerSubtle + AlphaContainerLow),
            colorScheme.tertiary,
            colorScheme.tertiary.copy(alpha = AlphaSettingsNoticeDivider),
        )
        SettingsSemanticTone.Error -> Triple(
            colorScheme.errorContainer.copy(alpha = AlphaContainerSubtle + AlphaContainerLow),
            colorScheme.error,
            colorScheme.error.copy(alpha = AlphaSettingsNoticeDivider),
        )
    }

    val shape = RoundedCornerShape(CornerRadiusLargeIncreased)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor, shape)
            .border(BorderStroke(BorderWidthThin, borderColor), shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingNormal),
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium),
            verticalAlignment = Alignment.Top,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier
                        .padding(top = SpacingTiny)
                        .size(IconSizeMedium),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SpacingExtraSmall),
            ) {
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (tone == SettingsSemanticTone.Neutral) {
                            colorScheme.onSurface
                        } else {
                            accentColor
                        },
                    )
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                )
            }
        }

        if (action != null) {
            HorizontalDivider(color = accentColor.copy(alpha = AlphaSettingsNoticeDivider))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = SettingsNoticeActionMinHeight)
                    .padding(horizontal = SpacingNormal, vertical = SpacingSmall),
                contentAlignment = Alignment.CenterEnd,
            ) {
                action()
            }
        }
    }
}

/**
 * Prominent highlighted metric block (e.g. total storage consumption).
 */
@Composable
fun InfoStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val shape = RoundedCornerShape(CornerRadiusLarge)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = AlphaSettingsInfoStatContainer),
                shape,
            )
            .padding(SpacingNormal),
        horizontalArrangement = Arrangement.spacedBy(SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            ThemedIcon(icon = icon, tint = accentColor)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SpacingTiny),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Centered empty-state placeholder for search queries with zero results.
 */
@Composable
fun SettingsEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector = Icons.Rounded.SearchOff,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingNormal, vertical = SpacingExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SpacingSmall),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaSettingsEmptyIcon),
            modifier = Modifier.size(SettingsEmptyStateIconSize),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
