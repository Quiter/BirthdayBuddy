package com.heckmannch.birthdaybuddy.ui.screens.settings.labels

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppSwitch
import com.heckmannch.birthdaybuddy.ui.components.InfoCard
import com.heckmannch.birthdaybuddy.ui.components.SettingsDetailScaffold
import com.heckmannch.birthdaybuddy.ui.components.withSettingsInsets
import com.heckmannch.birthdaybuddy.ui.model.LabelManagementModel
import com.heckmannch.birthdaybuddy.ui.screens.home.components.labels.toDisplayLabel
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisDisabled
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.ChipPaddingHorizontal
import com.heckmannch.birthdaybuddy.ui.theme.ChipPaddingVertical
import com.heckmannch.birthdaybuddy.ui.theme.EmptyStatePadding
import com.heckmannch.birthdaybuddy.ui.theme.GridColumnMinWidth
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

@Composable
fun LabelSettingsScreen(
    viewModel: LabelViewModel,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
) {
    val labels by viewModel.labelManagementList.collectAsStateWithLifecycle()
    val labelsEnabled by viewModel.labelsEnabled.collectAsStateWithLifecycle()

    LabelSettingsScreenContent(
        labels = labels,
        labelsEnabled = labelsEnabled,
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack,
        onLabelsEnabledChanged = { viewModel.onIntent(LabelIntent.SetLabelsEnabled(it)) }
    ) { name, hidden, ignored, isSystem, notificationsEnabled, showInWidget ->
        viewModel.onIntent(
            LabelIntent.UpdateLabelConfig(
                name = name,
                hidden = hidden,
                ignored = ignored,
                isSystem = isSystem,
                notificationsEnabled = notificationsEnabled,
                showInWidget = showInWidget
            )
        )
    }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun LabelSettingsScreenContent(
    labels: List<LabelManagementModel>,
    labelsEnabled: Boolean,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
    onLabelsEnabledChanged: (Boolean) -> Unit,
    onConfigChanged: (String, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    SettingsDetailScaffold(
        title = stringResource(R.string.settings_labels_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = GridColumnMinWidth),
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues.withSettingsInsets(vertical = SpacingNormal),
            horizontalArrangement = Arrangement.spacedBy(SpacingNormal),
            verticalArrangement = Arrangement.spacedBy(SpacingNormal)
        ) {
            // Master-Switch ganz oben
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.labels_enable)) },
                        supportingContent = { Text(stringResource(R.string.labels_enable_desc)) },
                        trailingContent = {
                            AppSwitch(
                                checked = labelsEnabled,
                                onCheckedChange = onLabelsEnabledChanged
                            )
                        },
                        modifier = Modifier.clickable { onLabelsEnabledChanged(!labelsEnabled) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            if (labelsEnabled && labels.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = EmptyStatePadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.labels_empty),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else if (!labelsEnabled && labels.isEmpty()) {
                // Leerer Zustand wenn deaktiviert: Nichts weiter anzeigen außer dem Switch
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    InfoCard(
                        title = stringResource(R.string.labels_info_title),
                        description = stringResource(R.string.labels_info_hide) + "\n\n" + stringResource(
                            R.string.labels_info_ignore
                        ),
                        modifier = Modifier.then(
                            if (!labelsEnabled) Modifier.alpha(AlphaEmphasisDisabled) else Modifier
                        )
                    )
                }

                items(
                    items = labels,
                    key = { it.name }
                ) { label ->
                    LabelConfigCard(
                        label = label,
                        enabled = labelsEnabled,
                        onConfigChanged = onConfigChanged
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabelConfigCard(
    label: LabelManagementModel,
    enabled: Boolean = true,
    onConfigChanged: (String, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(label.isIgnored) {
        if (label.isIgnored) {
            expanded = false
        }
    }

    val onHideToggle = remember(label, onConfigChanged) {
        {
            val newHidden = !label.isHiddenFromFilter
            val newNotifications = if (newHidden) false else true
            val newWidget = if (newHidden) false else true
            onConfigChanged(
                label.name,
                newHidden,
                label.isIgnored,
                label.isSystem,
                newNotifications,
                newWidget
            )
        }
    }
    val onIgnoreToggle = remember(label, onConfigChanged) {
        {
            val newIgnored = !label.isIgnored
            val newNotifications = if (newIgnored) false else label.notificationsEnabled
            val newWidget = if (newIgnored) false else label.showInWidget
            onConfigChanged(
                label.name,
                label.isHiddenFromFilter,
                newIgnored,
                label.isSystem,
                newNotifications,
                newWidget
            )
        }
    }
    val onNotificationsToggle = remember(label, onConfigChanged) {
        { newNotificationsEnabled: Boolean ->
            onConfigChanged(
                label.name,
                label.isHiddenFromFilter,
                label.isIgnored,
                label.isSystem,
                newNotificationsEnabled,
                label.showInWidget
            )
        }
    }
    val onWidgetToggle = remember(label, onConfigChanged) {
        { newShowInWidget: Boolean ->
            onConfigChanged(
                label.name,
                label.isHiddenFromFilter,
                label.isIgnored,
                label.isSystem,
                label.notificationsEnabled,
                newShowInWidget
            )
        }
    }

    val isExpandEnabled = enabled && !label.isIgnored

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!enabled) Modifier.alpha(AlphaEmphasisDisabled) else Modifier),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .padding(SpacingNormal)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
                    modifier = Modifier.weight(1f)
                ) {
                    val displayName = label.name.toDisplayLabel()
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (label.isSystem) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.extraSmall,
                        ) {
                            Text(
                                text = stringResource(R.string.labels_system_tag),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(
                                    horizontal = ChipPaddingHorizontal,
                                    vertical = ChipPaddingVertical
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    enabled = isExpandEnabled
                ) {
                    Icon(
                        imageVector = if (expanded && !label.isIgnored) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = stringResource(
                            if (expanded && !label.isIgnored) R.string.labels_collapse_card else R.string.labels_expand_card
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingSmall))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpacingSmall)
            ) {
                FilterChip(
                    selected = label.isHiddenFromFilter,
                    onClick = onHideToggle,
                    enabled = enabled,
                    label = { Text(stringResource(R.string.labels_action_hide)) },
                    leadingIcon = if (label.isHiddenFromFilter) {
                        {
                            Icon(
                                Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(IconSizeSmall)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                FilterChip(
                    selected = label.isIgnored,
                    onClick = onIgnoreToggle,
                    enabled = enabled,
                    label = { Text(stringResource(R.string.labels_action_ignore)) },
                    leadingIcon = if (label.isIgnored) {
                        {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(IconSizeSmall)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }

            AnimatedVisibility(visible = expanded && !label.isIgnored) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = SpacingSmall),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Text(
                        text = stringResource(R.string.labels_granular_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = SpacingSmall)
                    )

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.labels_notifications_title)) },
                        supportingContent = { Text(stringResource(R.string.labels_notifications_desc)) },
                        trailingContent = {
                            AppSwitch(
                                checked = label.notificationsEnabled,
                                onCheckedChange = { onNotificationsToggle(it) },
                                enabled = enabled
                            )
                        },
                        modifier = Modifier.clickable(enabled = enabled) {
                            onNotificationsToggle(!label.notificationsEnabled)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.labels_widget_title)) },
                        supportingContent = { Text(stringResource(R.string.labels_widget_desc)) },
                        trailingContent = {
                            AppSwitch(
                                checked = label.showInWidget,
                                onCheckedChange = { onWidgetToggle(it) },
                                enabled = enabled
                            )
                        },
                        modifier = Modifier.clickable(enabled = enabled) {
                            onWidgetToggle(!label.showInWidget)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LabelSettingsPreview() {
    BirthdayBuddyTheme {
        LabelSettingsScreenContent(
            labels = listOf(
                LabelManagementModel(
                    "Familie",
                    isHiddenFromFilter = false,
                    isIgnored = false,
                    isSystem = true,
                    notificationsEnabled = true,
                    showInWidget = true
                ),
                LabelManagementModel(
                    "Freunde",
                    isHiddenFromFilter = true,
                    isIgnored = false,
                    isSystem = false,
                    notificationsEnabled = true,
                    showInWidget = false
                ),
                LabelManagementModel(
                    "Arbeit",
                    isHiddenFromFilter = false,
                    isIgnored = false,
                    isSystem = false,
                    notificationsEnabled = false,
                    showInWidget = true
                ),
                LabelManagementModel(
                    "Ex-Kollegen",
                    isHiddenFromFilter = false,
                    isIgnored = true,
                    isSystem = false,
                    notificationsEnabled = false,
                    showInWidget = false
                ),
                LabelManagementModel(
                    "Sport",
                    isHiddenFromFilter = true,
                    isIgnored = true,
                    isSystem = false,
                    notificationsEnabled = true,
                    showInWidget = true
                )
            ),
            labelsEnabled = true,
            onNavigateBack = {},
            onLabelsEnabledChanged = {},
            onConfigChanged = { _, _, _, _, _, _ -> }
        )
    }
}
