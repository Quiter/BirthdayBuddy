package com.heckmannch.birthdaybuddy.ui.screens.home.components.header

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Visual style variant for top header [TitleAction] buttons.
 */
enum class TitleActionStyle {
    /** Flat button with transparent background. */
    Plain,
    /** Tonal button with primary container color fill. */
    Accent,
    /** Neutral tonal button with surface variant background fill. */
    Neutral,
}

/**
 * Standardized title action icon button used in header bars.
 *
 * Modeled after Morphe Manager's `TitleAction` component. Provides uniform sizing,
 * shape, accessibility description, and optional container styling.
 *
 * @param icon Vector graphic for the icon button.
 * @param contentDescription Localized description for accessibility screen readers.
 * @param onClick Action callback executed upon tapping the button.
 * @param modifier Optional [Modifier] for layout adjustments.
 * @param style Visual styling preset for container tinting.
 * @param enabled Whether the action button is active and clickable.
 */
@Composable
fun TitleAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TitleActionStyle = TitleActionStyle.Plain,
    enabled: Boolean = true,
) {
    val containerColor = when (style) {
        TitleActionStyle.Plain -> Color.Transparent
        TitleActionStyle.Accent -> MaterialTheme.colorScheme.primaryContainer
        TitleActionStyle.Neutral -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when (style) {
        TitleActionStyle.Plain -> MaterialTheme.colorScheme.onSurfaceVariant
        TitleActionStyle.Accent -> MaterialTheme.colorScheme.onPrimaryContainer
        TitleActionStyle.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}
