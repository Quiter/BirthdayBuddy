package com.heckmannch.birthdaybuddy.ui.screens.labels.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppSwitch
import com.heckmannch.birthdaybuddy.ui.components.cards.GlassCardExpandButton
import com.heckmannch.birthdaybuddy.ui.components.cards.GlassCardLayout
import com.heckmannch.birthdaybuddy.ui.components.cards.GlassCardTag
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.LabelManagementModel
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.components.labels.toDisplayLabel
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import com.heckmannch.birthdaybuddy.ui.theme.defaultCardGradient
import com.heckmannch.birthdaybuddy.ui.theme.labelActiveCardGradient
import com.heckmannch.birthdaybuddy.ui.theme.labelHiddenCardGradient
import com.heckmannch.birthdaybuddy.ui.theme.labelIgnoredCardGradient

/**
 * Individual expandable Label Card built on the central [GlassCardLayout].
 *
 * Card coloring is controlled centrally via `Color.kt`:
 * - Ignored labels: red ([labelIgnoredCardGradient])
 * - Hidden labels: gray ([labelHiddenCardGradient])
 * - Active filter labels: theme accent ([labelActiveCardGradient])
 * - Default labels: neutral frosted glass ([defaultCardGradient])
 *
 * Tapping the card toggles the active label filter ([onSelectLabel]).
 * Tapping the trailing expand button expands/collapses granular options and assigned contacts.
 */
@Composable
internal fun LabelCard(
    label: LabelManagementModel,
    isActive: Boolean,
    assignedContacts: List<ContactUiModel>,
    onSelectLabel: () -> Unit,
    onConfigChanged: (String, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(label.name) { mutableStateOf(false) }

    val gradientColors = when {
        label.isIgnored -> labelIgnoredCardGradient()
        label.isHiddenFromFilter -> labelHiddenCardGradient()
        isActive -> labelActiveCardGradient()
        else -> defaultCardGradient()
    }

    val contactCountText = if (assignedContacts.size == 1) {
        stringResource(R.string.labels_count_contacts_one)
    } else {
        stringResource(R.string.labels_count_contacts_many, assignedContacts.size)
    }

    GlassCardLayout(
        modifier = modifier,
        gradientColors = gradientColors,
        onClick = onSelectLabel,
        expanded = expanded,
        trailing = {
            GlassCardExpandButton(
                expanded = expanded,
                onToggle = { expanded = !expanded },
                style = style,
                contentDescription = stringResource(
                    if (expanded) R.string.labels_collapse_card else R.string.labels_expand_card
                )
            )
        },
        expandedContent = { cardStyle ->
            val colors = MaterialTheme.colorScheme

            Text(
                text = stringResource(R.string.labels_granular_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = cardStyle.titleColor,
                modifier = Modifier.padding(bottom = SpacingExtraSmall)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpacingSmall)
            ) {
                FilterChip(
                    selected = label.isHiddenFromFilter,
                    onClick = {
                        val newHidden = !label.isHiddenFromFilter
                        onConfigChanged(
                            label.name,
                            newHidden,
                            label.isIgnored,
                            label.isSystem,
                            !newHidden,
                            !newHidden
                        )
                    },
                    label = { Text(stringResource(R.string.labels_action_hide)) },
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = cardStyle.titleColor,
                        selectedContainerColor = cardStyle.actionContainerColor,
                        selectedLabelColor = cardStyle.actionContentColor
                    )
                )

                FilterChip(
                    selected = label.isIgnored,
                    onClick = {
                        val newIgnored = !label.isIgnored
                        onConfigChanged(
                            label.name,
                            label.isHiddenFromFilter,
                            newIgnored,
                            label.isSystem,
                            !newIgnored && label.notificationsEnabled,
                            !newIgnored && label.showInWidget
                        )
                    },
                    label = { Text(stringResource(R.string.labels_action_ignore)) },
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = cardStyle.titleColor,
                        selectedContainerColor = colors.errorContainer,
                        selectedLabelColor = colors.onErrorContainer
                    )
                )
            }

            ListItem(
                supportingContent = {
                    Text(
                        text = stringResource(R.string.labels_notifications_desc),
                        color = cardStyle.subtitleColor
                    )
                },
                trailingContent = {
                    AppSwitch(
                        checked = label.notificationsEnabled,
                        onCheckedChange = { notifications ->
                            onConfigChanged(
                                label.name,
                                label.isHiddenFromFilter,
                                label.isIgnored,
                                label.isSystem,
                                notifications,
                                label.showInWidget
                            )
                        }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            ) {
                Text(
                    text = stringResource(R.string.labels_notifications_title),
                    color = cardStyle.titleColor
                )
            }

            ListItem(
                supportingContent = {
                    Text(
                        text = stringResource(R.string.labels_widget_desc),
                        color = cardStyle.subtitleColor
                    )
                },
                trailingContent = {
                    AppSwitch(
                        checked = label.showInWidget,
                        onCheckedChange = { showInWidget ->
                            onConfigChanged(
                                label.name,
                                label.isHiddenFromFilter,
                                label.isIgnored,
                                label.isSystem,
                                label.notificationsEnabled,
                                showInWidget
                            )
                        }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            ) {
                Text(
                    text = stringResource(R.string.labels_widget_title),
                    color = cardStyle.titleColor
                )
            }

            if (assignedContacts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(SpacingSmall))
                Text(
                    text = stringResource(R.string.labels_assigned_contacts, assignedContacts.size),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cardStyle.subtitleColor,
                    modifier = Modifier.padding(bottom = SpacingSmall)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
                    contentPadding = PaddingValues(vertical = SpacingExtraSmall)
                ) {
                    items(assignedContacts, key = { it.id }) { contact ->
                        ContactChip(contact = contact)
                    }
                }
            }
        }
    ) {
        // 1. Selection indicator (left) - reflects active filter state
        SelectionCheckIndicator(
            selected = isActive,
            ink = style.titleColor
        )

        // 2. Label Title, Badges & Contact Count (middle)
        Column(
            modifier = with(row) { Modifier.weight(1f) },
            verticalArrangement = Arrangement.spacedBy(style.textSpacing)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpacingSmall)
            ) {
                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    text = label.name.toDisplayLabel(),
                    style = style.titleStyle,
                    fontWeight = FontWeight.Bold,
                    color = style.titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (label.isSystem) {
                    GlassCardTag(text = stringResource(R.string.labels_system_tag))
                }
            }

            Text(
                text = contactCountText,
                style = style.subtitleStyle,
                color = style.subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (label.isHiddenFromFilter || label.isIgnored) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
                    modifier = Modifier.padding(top = SpacingExtraSmall)
                ) {
                    if (label.isHiddenFromFilter) {
                        GlassCardTag(text = stringResource(R.string.labels_action_hide))
                    }
                    if (label.isIgnored) {
                        GlassCardTag(text = stringResource(R.string.labels_action_ignore))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LabelCardPreview() {
    BirthdayBuddyTheme {
        Column(
            modifier = Modifier.padding(SpacingNormal),
            verticalArrangement = Arrangement.spacedBy(SpacingSmall)
        ) {
            LabelCard(
                label = LabelManagementModel(
                    name = "Familie",
                    isHiddenFromFilter = false,
                    isIgnored = false,
                    isSystem = true,
                    notificationsEnabled = true,
                    showInWidget = true
                ),
                isActive = true,
                assignedContacts = SampleData.sampleContacts,
                onSelectLabel = {},
                onConfigChanged = { _, _, _, _, _, _ -> }
            )
            LabelCard(
                label = LabelManagementModel(
                    name = "Arbeit",
                    isHiddenFromFilter = true,
                    isIgnored = false,
                    isSystem = false,
                    notificationsEnabled = false,
                    showInWidget = false
                ),
                isActive = false,
                assignedContacts = emptyList(),
                onSelectLabel = {},
                onConfigChanged = { _, _, _, _, _, _ -> }
            )
            LabelCard(
                label = LabelManagementModel(
                    name = "Ignoriert",
                    isHiddenFromFilter = false,
                    isIgnored = true,
                    isSystem = false,
                    notificationsEnabled = false,
                    showInWidget = false
                ),
                isActive = false,
                assignedContacts = emptyList(),
                onSelectLabel = {},
                onConfigChanged = { _, _, _, _, _, _ -> }
            )
        }
    }
}
