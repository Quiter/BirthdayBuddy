package com.heckmannch.birthdaybuddy.ui.screens.settings.labels

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.components.InfoCard
import com.heckmannch.birthdaybuddy.ui.model.LabelManagementModel
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import com.heckmannch.birthdaybuddy.viewmodel.HomeViewModel
import com.heckmannch.birthdaybuddy.viewmodel.LabelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelSettingsScreen(
    windowWidthSizeClass: WindowWidthSizeClass,
    viewModel: LabelViewModel,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
) {
    val labels by viewModel.labelManagementList.collectAsStateWithLifecycle()

    LabelSettingsContent(
        windowWidthSizeClass = windowWidthSizeClass,
        labels = labels,
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack,
    ) { name, hidden, ignored, isSystem ->
        viewModel.updateLabelConfig(name, hidden, ignored, isSystem)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabelSettingsContent(
    windowWidthSizeClass: WindowWidthSizeClass,
    labels: List<LabelManagementModel>,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
    onConfigChanged: (String, Boolean, Boolean, Boolean) -> Unit
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    AppResponsiveScaffold(
        windowWidthSizeClass = windowWidthSizeClass,
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
        if (labels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.labels_empty),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            val layoutDirection = LocalLayoutDirection.current
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
                item(span = { GridItemSpan(maxLineSpan) }) {
                    InfoCard(
                        title = stringResource(R.string.labels_info_title),
                        description = stringResource(R.string.labels_info_hide) + "\n\n" + stringResource(R.string.labels_info_ignore)
                    )
                }

                items(
                    items = labels,
                    key = { it.name }
                ) { label ->
                    LabelConfigCard(
                        label = label,
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
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
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
                val displayName = if (label.name == HomeViewModel.LABEL_NO_BIRTHDAY) {
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
        LabelSettingsContent(
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
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
            onNavigateBack = {},
            onConfigChanged = { _, _, _, _ -> }
        )
    }
}
