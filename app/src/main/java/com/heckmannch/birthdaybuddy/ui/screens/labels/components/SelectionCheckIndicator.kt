package com.heckmannch.birthdaybuddy.ui.screens.labels.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.ui.components.cards.contrastingContent
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSurfaceContainerHigh
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThin
import com.heckmannch.birthdaybuddy.ui.theme.GlassCardMarkSize
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeExtraSmall

/**
 * Selection indicator circle showing a checkmark when selected.
 * Adapts to [ink] when rendered on colored frosted glass cards.
 */
@Composable
internal fun SelectionCheckIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
    ink: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = when {
        selected && ink != null -> ink
        selected -> colors.primary
        ink != null -> Color.Transparent
        else -> colors.surfaceContainerHigh
    }
    val checkTint = if (ink != null) ink.contrastingContent() else colors.onPrimary
    val borderColor = ink?.copy(alpha = AlphaSurfaceContainerHigh) ?: colors.outline

    Surface(
        shape = CircleShape,
        color = containerColor,
        border = if (selected) null else BorderStroke(BorderWidthThin, borderColor),
        modifier = modifier
            .size(GlassCardMarkSize)
            .clip(CircleShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Checkbox, onClick = onClick)
                } else {
                    Modifier
                }
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = checkTint,
                    modifier = Modifier.size(IconSizeExtraSmall)
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
