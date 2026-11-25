package com.heckmannch.birthdaybuddy.ui.screens.labels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.ui.components.AppSwitch
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.model.LabelManagementModel
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.screens.home.components.labels.toDisplayLabel
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.ContactImage
import com.heckmannch.birthdaybuddy.ui.screens.settings.labels.LabelIntent
import com.heckmannch.birthdaybuddy.ui.screens.settings.labels.LabelViewModel
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
 * Screen displaying and managing contact labels.
 *
 * ### ARCHITECTURAL RULE:
 * [LabelsScreen] displays ONLY label management cards and label options.
 * Contacts are exclusively rendered on the main dashboard (HomeScreen).
 * Do NOT append contact lists or contact groups below the label cards.
 * Label creation is triggered via the TopAppBar header action icon (NO FloatingActionButton).
 *
 * Uses expandable Label Cards inspired by Morphe Manager's `PatchCard` design.
 * Each card allows:
 * - Direct tap on card surface: Selects/activates the label filter for the app.
 * - Tap on expansion arrow: Opens/closes label details and granular controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelsScreen(
    uiState: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    viewModel: LabelViewModel = hiltViewModel(),
    bottomBarHeight: Dp = 0.dp,
) {
    val haptic = LocalHapticFeedback.current
    val labelUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allContacts = uiState.contacts ?: emptyList()
    val availableLabels = labelUiState.labels

    var showAddLabelDialog by remember { mutableStateOf(false) }
    var newLabelNameInput by remember { mutableStateOf("") }

    val activeLabelFilter = uiState.selectedLabel

    if (showAddLabelDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddLabelDialog = false
                newLabelNameInput = ""
            },
            title = {
                Text(
                    text = stringResource(R.string.labels_add_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                OutlinedTextField(
                    value = newLabelNameInput,
                    onValueChange = { newLabelNameInput = it },
                    label = { Text(stringResource(R.string.labels_add_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newLabelNameInput.trim()
                        if (trimmed.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onIntent(
                                LabelIntent.UpdateLabelConfig(
                                    name = trimmed,
                                    hidden = false,
                                    ignored = false,
                                    isSystem = false,
                                    notificationsEnabled = true,
                                    showInWidget = true,
                                )
                            )
                            actions.onLabelSelected(trimmed)
                        }
                        showAddLabelDialog = false
                        newLabelNameInput = ""
                    },
                    enabled = newLabelNameInput.isNotBlank(),
                ) {
                    Text(stringResource(R.string.labels_add_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddLabelDialog = false
                        newLabelNameInput = ""
                    }
                ) {
                    Text(stringResource(R.string.labels_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.labels_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showAddLabelDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.labels_add_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = SpacingNormal,
                end = SpacingNormal,
                top = SpacingSmall,
                bottom = bottomBarHeight + SpacingNormal,
            ),
            verticalArrangement = Arrangement.spacedBy(SpacingSmall),
        ) {
            // 1. "Alle Labels" Card
            item(key = "all_labels_card") {
                AllLabelsCard(
                    isActive = activeLabelFilter == null,
                    contactsCount = allContacts.count { it.labels.isNotEmpty() },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        actions.onLabelSelected(null)
                    }
                )
            }

            // 2. Expandable Morphe-style Label Cards
            items(
                items = availableLabels,
                key = { it.name }
            ) { label ->
                val assignedContacts = remember(allContacts, label.name) {
                    if (label.name == ContactLabels.LABEL_NO_BIRTHDAY) {
                        allContacts.filter { it.birthday == null }
                    } else {
                        allContacts.filter { it.labels.contains(label.name) }
                    }
                }

                LabelCard(
                    label = label,
                    isActive = activeLabelFilter == label.name,
                    assignedContacts = assignedContacts,
                    onSelectLabel = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        actions.onLabelSelected(if (activeLabelFilter == label.name) null else label.name)
                    },
                    onConfigChanged = { name, hidden, ignored, isSystem, notifs, widget ->
                        viewModel.onIntent(
                            LabelIntent.UpdateLabelConfig(
                                name = name,
                                hidden = hidden,
                                ignored = ignored,
                                isSystem = isSystem,
                                notificationsEnabled = notifs,
                                showInWidget = widget
                            )
                        )
                    }
                )
            }
        }
    }
}

/**
 * Card representing "All Labels" selection.
 */
@Composable
private fun AllLabelsCard(
    isActive: Boolean,
    contactsCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = if (isActive) colors.secondaryContainer.copy(alpha = 0.55f) else colors.surfaceContainerLow
    val borderColor = if (isActive) colors.primary else colors.outlineVariant.copy(alpha = 0.5f)

    OutlinedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(width = if (isActive) SelectedBorderWidth else BorderWidthThin, color = borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingNormal)
        ) {
            SelectionCheckIndicator(
                selected = isActive,
                modifier = Modifier.padding(end = SpacingMedium)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.labels_all_card),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.height(SpacingExtraSmall))
                Text(
                    text = stringResource(R.string.labels_all_card_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            Surface(
                color = colors.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = if (contactsCount == 1) {
                        stringResource(R.string.labels_count_contacts_one)
                    } else {
                        stringResource(R.string.labels_count_contacts_many, contactsCount)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = SpacingSmall, vertical = SpacingExtraSmall),
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Individual expandable Label Card with Morphe Manager `PatchCard` architecture.
 */
@Composable
private fun LabelCard(
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

/**
 * Selection indicator circle showing checkmark when selected.
 */
@Composable
private fun SelectionCheckIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = if (selected) colors.primary else colors.surfaceContainerHigh,
        border = if (selected) null else BorderStroke(BorderWidthThin, colors.outline),
        modifier = modifier.size(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Compact contact chip for assigned contacts row.
 */
@Composable
private fun ContactChip(
    contact: ContactUiModel,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = SpacingSmall, vertical = SpacingExtraSmall)
        ) {
            ContactImage(
                imageUri = contact.imageUri,
                fullName = contact.fullName,
                initials = contact.initials,
                lookupKey = contact.lookupKey,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(SpacingExtraSmall))
            Text(
                text = contact.fullName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
