package com.heckmannch.birthdaybuddy.ui.screens.settings.labels

import androidx.window.core.layout.WindowSizeClass
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.components.AppSwitch
import com.heckmannch.birthdaybuddy.ui.components.InfoCard
import com.heckmannch.birthdaybuddy.ui.model.LabelManagementModel
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisDisabled
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelSettingsScreen(
    windowSizeClass: WindowSizeClass,
    viewModel: LabelViewModel,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
) {
    val labels by viewModel.labelManagementList.collectAsStateWithLifecycle()
    val labelsEnabled by viewModel.labelsEnabled.collectAsStateWithLifecycle()

    LabelSettingsScreenContent(
        windowSizeClass = windowSizeClass,
        labels = labels,
        labelsEnabled = labelsEnabled,
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack,
        onLabelsEnabledChanged = { viewModel.onIntent(LabelIntent.SetLabelsEnabled(it)) }
    ) { name, hidden, ignored, isSystem ->
        viewModel.onIntent(LabelIntent.UpdateLabelConfig(name, hidden, ignored, isSystem))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabelSettingsScreenContent(
    windowSizeClass: WindowSizeClass,
    labels: List<LabelManagementModel>,
    labelsEnabled: Boolean,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
    onLabelsEnabledChanged: (Boolean) -> Unit,
    onConfigChanged: (String, Boolean, Boolean, Boolean) -> Unit
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val layoutDirection = LocalLayoutDirection.current

    AppResponsiveScaffold(
        windowSizeClass = windowSizeClass,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.labels_title)) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.notifications_back),
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = paddingValues.calculateStartPadding(layoutDirection) + SpacingNormal,
                top = paddingValues.calculateTopPadding() + SpacingNormal,
                end = paddingValues.calculateEndPadding(layoutDirection) + SpacingNormal,
                bottom = paddingValues.calculateBottomPadding() + SpacingNormal
            ),
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
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
            }

            if (labelsEnabled && labels.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
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
    onConfigChanged: (String, Boolean, Boolean, Boolean) -> Unit
) {
    // Optimierung 2: Callbacks memoizen, um unnötige Recompositions zu vermeiden
    val onHideToggle = remember(label) {
        { onConfigChanged(label.name, !label.isHiddenFromFilter, label.isIgnored, label.isSystem) }
    }
    val onIgnoreToggle = remember(label) {
        { onConfigChanged(label.name, label.isHiddenFromFilter, !label.isIgnored, label.isSystem) }
    }

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
                val displayName = if (label.name == ContactLabels.LABEL_NO_BIRTHDAY) {
                    stringResource(R.string.home_filter_no_birthday)
                } else {
                    label.name
                }
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
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(SpacingNormal))

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
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LabelSettingsPreview() {
    BirthdayBuddyTheme {
        LabelSettingsScreenContent(
            windowSizeClass = WindowSizeClass(360, 640),
            labels = listOf(
                LabelManagementModel(
                    "Familie",
                    isHiddenFromFilter = false,
                    isIgnored = false,
                    isSystem = true
                ),
                LabelManagementModel(
                    "Freunde",
                    isHiddenFromFilter = true,
                    isIgnored = false,
                    isSystem = false
                ),
                LabelManagementModel(
                    "Arbeit",
                    isHiddenFromFilter = false,
                    isIgnored = false,
                    isSystem = false
                ),
                LabelManagementModel(
                    "Ex-Kollegen",
                    isHiddenFromFilter = false,
                    isIgnored = true,
                    isSystem = false
                ),
                LabelManagementModel(
                    "Sport",
                    isHiddenFromFilter = true,
                    isIgnored = true,
                    isSystem = false
                )
            ),
            labelsEnabled = true,
            onNavigateBack = {},
            onLabelsEnabledChanged = {},
            onConfigChanged = { _, _, _, _ -> }
        )
    }
}
