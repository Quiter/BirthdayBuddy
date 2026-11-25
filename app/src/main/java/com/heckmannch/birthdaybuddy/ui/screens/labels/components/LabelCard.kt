package com.heckmannch.birthdaybuddy.ui.screens.labels.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
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
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppSwitch
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.LabelManagementModel
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.components.labels.toDisplayLabel
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThin
import com.heckmannch.birthdaybuddy.ui.theme.ChipPaddingHorizontal
import com.heckmannch.birthdaybuddy.ui.theme.ChipPaddingVertical
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeLarge
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeMedium
import com.heckmannch.birthdaybuddy.ui.theme.SelectedBorderWidth
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * Individual expandable Label Card with Morphe Manager `PatchCard` architecture.
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

    val colors = MaterialTheme.colorScheme
    val containerColor = when {
        isActive -> colors.secondaryContainer.copy(alpha = 0.55f)
        label.isIgnored -> colors.errorContainer.copy(alpha = 0.2f)
        else -> colors.surfaceContainerLow
    }

    val borderColor = when {
        isActive -> colors.primary
        label.isIgnored -> colors.error.copy(alpha = 0.6f)
        else -> colors.outlineVariant.copy(alpha = 0.5f)
    }

    OutlinedCard(
        onClick = onSelectLabel,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(
            width = if (isActive) SelectedBorderWidth else BorderWidthThin,
            color = borderColor
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(SpacingNormal)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Selection indicator (left)
                SelectionCheckIndicator(
                    selected = isActive,
                    modifier = Modifier.padding(end = SpacingMedium)
                )

                // 2. Label Title & Badges (middle)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpacingSmall)
                    ) {
                        Text(
                            text = label.name.toDisplayLabel(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )

                        if (label.isSystem) {
                            Surface(
                                color = colors.secondaryContainer,
                                shape = MaterialTheme.shapes.extraSmall,
                            ) {
                                Text(
                                    text = stringResource(R.string.labels_system_tag),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(
                                        horizontal = ChipPaddingHorizontal,
                                        vertical = ChipPaddingVertical
                                    ),
                                    color = colors.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(SpacingExtraSmall))

                    val contactCountText = if (assignedContacts.size == 1) {
                        stringResource(R.string.labels_count_contacts_one)
                    } else {
                        stringResource(R.string.labels_count_contacts_many, assignedContacts.size)
                    }

                    Text(
                        text = contactCountText,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }

                // 3. Expand / Collapse button (right) - stops propagation to card click
                FilledTonalIconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(IconSizeLarge)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = stringResource(
                            if (expanded) R.string.labels_collapse_card else R.string.labels_expand_card
                        ),
                        modifier = Modifier.size(IconSizeMedium)
                    )
                }
            }

            // Status badges if hidden or ignored
            if (label.isHiddenFromFilter || label.isIgnored) {
                Spacer(modifier = Modifier.height(SpacingSmall))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SpacingSmall)
                ) {
                    if (label.isHiddenFromFilter) {
                        Surface(
                            color = colors.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = stringResource(R.string.labels_action_hide),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = SpacingSmall, vertical = SpacingExtraSmall),
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                    if (label.isIgnored) {
                        Surface(
                            color = colors.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = stringResource(R.string.labels_action_ignore),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = SpacingSmall, vertical = SpacingExtraSmall),
                                color = colors.onErrorContainer
                            )
                        }
                    }
                }
            }

            // 4. Expanded detail section (options & assigned contacts)
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = SpacingMedium)
                ) {
                    HorizontalDivider(
                        color = colors.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = SpacingSmall)
                    )

                    Text(
                        text = stringResource(R.string.labels_granular_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
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
                            label = { Text(stringResource(R.string.labels_action_hide)) }
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
                                selectedContainerColor = colors.errorContainer,
                                selectedLabelColor = colors.onErrorContainer
                            )
                        )
                    }

                    ListItem(
                        supportingContent = { Text(stringResource(R.string.labels_notifications_desc)) },
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
                        Text(stringResource(R.string.labels_notifications_title))
                    }

                    ListItem(
                        supportingContent = { Text(stringResource(R.string.labels_widget_desc)) },
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
                        Text(stringResource(R.string.labels_widget_title))
                    }

                    if (assignedContacts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(SpacingSmall))
                        Text(
                            text = stringResource(R.string.labels_assigned_contacts, assignedContacts.size),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurfaceVariant,
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
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LabelCardPreview() {
    BirthdayBuddyTheme {
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
    }
}
