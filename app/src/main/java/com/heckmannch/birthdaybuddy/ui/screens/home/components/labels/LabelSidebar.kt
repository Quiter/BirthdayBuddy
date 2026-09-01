package com.heckmannch.birthdaybuddy.ui.screens.home.components.labels

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.ui.theme.SidebarHeaderSpacerHeight
import com.heckmannch.birthdaybuddy.ui.theme.SidebarItemWidthCollapsed
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * A sidebar component for wide screens displaying filters/labels.
 * Adapts between an expanded navigation drawer and a collapsed rail.
 */
@Composable
fun LabelSidebar(
    labels: List<String>,
    selectedLabel: String?,
    onLabelSelected: (String?) -> Unit,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = SpacingNormal)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SpacingSmall)
    ) {
        if (isExpanded) {
            Text(
                text = stringResource(R.string.labels_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = SpacingMedium + SpacingSmall,
                    vertical = SpacingSmall
                )
            )
        } else {
            Spacer(modifier = Modifier.height(SidebarHeaderSpacerHeight)) // Vertical alignment placeholder
        }

        // "All" item
        SidebarLabelItem(
            label = stringResource(R.string.home_filter_all),
            isSelected = selectedLabel == null,
            isExpanded = isExpanded,
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource(R.string.home_filter_all)
                )
            },
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLabelSelected(null)
            }
        )

        // Built-in & custom label items
        labels.forEach { label ->
            val displayLabel = label.toDisplayLabel()

            val iconVector = when (label) {
                ContactLabels.LABEL_NO_BIRTHDAY -> Icons.Default.DateRange
                ContactLabels.LABEL_ANNIVERSARY -> Icons.Default.Favorite
                ContactLabels.LABEL_NAME_DAY -> Icons.Default.Face
                else -> Icons.AutoMirrored.Filled.Label
            }

            SidebarLabelItem(
                label = displayLabel,
                isSelected = selectedLabel == label,
                isExpanded = isExpanded,
                icon = {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = displayLabel
                    )
                },
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLabelSelected(label)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SidebarLabelItem(
    label: String,
    isSelected: Boolean,
    isExpanded: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tooltipState = rememberTooltipState()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Above
        ),
        tooltip = {
            PlainTooltip {
                Text(text = label)
            }
        },
        state = tooltipState
    ) {
        NavigationDrawerItem(
            icon = icon,
            label = {
                if (isExpanded) {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.then(
                            if (isSelected) Modifier.basicMarquee() else Modifier
                        )
                    )
                }
            },
            selected = isSelected,
            onClick = onClick,
            modifier = modifier
                .padding(horizontal = SpacingSmall)
                .then(
                    if (!isExpanded) Modifier.width(SidebarItemWidthCollapsed) else Modifier
                ),
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
