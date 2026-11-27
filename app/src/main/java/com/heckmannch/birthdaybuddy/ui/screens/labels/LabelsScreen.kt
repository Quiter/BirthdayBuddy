package com.heckmannch.birthdaybuddy.ui.screens.labels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.model.LabelManagementModel
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.screens.labels.components.AddLabelDialog
import com.heckmannch.birthdaybuddy.ui.screens.labels.components.AllLabelsCard
import com.heckmannch.birthdaybuddy.ui.screens.labels.components.LabelCard
import com.heckmannch.birthdaybuddy.ui.screens.labels.components.LabelsHeader
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.labels.LabelIntent
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.labels.LabelUiState
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.labels.LabelViewModel
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.labels.LabelWithContactsUiModel
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * Screen displaying and managing contact labels and pseudo-labels.
 *
 * ### ARCHITECTURAL RULE:
 * [LabelsScreen] displays ONLY label management cards and label options.
 * Contacts are exclusively rendered on the main dashboard (HomeScreen).
 * Do NOT append contact lists or contact groups below the label cards.
 * Label creation is triggered via the [LabelsHeader] action icon (NO FloatingActionButton).
 *
 * Sections below [AllLabelsCard] are strictly ordered and omitted when empty:
 * 1. Unrestricted active labels (no header)
 * 2. Other Events (`R.string.settings_other_events_title`)
 * 3. Hidden labels (`R.string.labels_section_hidden`)
 * 4. Ignored labels (`R.string.labels_section_ignored`)
 */
@Composable
fun LabelsScreen(
    uiState: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    viewModel: LabelViewModel = hiltViewModel(),
    bottomBarHeight: Dp = 0.dp,
) {
    val labelUiState by viewModel.uiState.collectAsStateWithLifecycle()

    LabelsScreenContent(
        uiState = uiState,
        labelUiState = labelUiState,
        actions = actions,
        modifier = modifier,
        bottomBarHeight = bottomBarHeight,
        onUpdateLabelConfig = { name, hidden, ignored, isSystem, notificationsEnabled, showInWidget ->
            viewModel.onIntent(
                LabelIntent.UpdateLabelConfig(
                    name = name,
                    hidden = hidden,
                    ignored = ignored,
                    isSystem = isSystem,
                    notificationsEnabled = notificationsEnabled,
                    showInWidget = showInWidget,
                )
            )
        },
    )
}

@Composable
fun LabelsScreenContent(
    uiState: HomeUiState,
    labelUiState: LabelUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    bottomBarHeight: Dp = 0.dp,
    onUpdateLabelConfig: (name: String, hidden: Boolean, ignored: Boolean, isSystem: Boolean, notificationsEnabled: Boolean, showInWidget: Boolean) -> Unit = { _, _, _, _, _, _ -> },
) {
    val haptic = LocalHapticFeedback.current
    var showAddLabelDialog by remember { mutableStateOf(false) }

    val activeLabelFilter = uiState.selectedLabel

    if (showAddLabelDialog) {
        AddLabelDialog(
            onDismiss = { showAddLabelDialog = false },
            onConfirm = { name ->
                onUpdateLabelConfig(name, false, false, false, true, true)
                actions.onLabelSelected(name)
                showAddLabelDialog = false
            }
        )
    }

    val onSelectLabel: (String) -> Unit = { labelName ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        actions.onLabelSelected(if (activeLabelFilter == labelName) null else labelName)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LabelsHeader(
                onAddLabel = {
                    showAddLabelDialog = true
                }
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
            // "Alle Labels" Card
            item(key = "all_labels_card") {
                AllLabelsCard(
                    isActive = activeLabelFilter == null,
                    contactsCount = labelUiState.allLabeledContactsCount,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        actions.onLabelSelected(null)
                    }
                )
            }

            // Section 1: Unrestricted active labels (no header)
            labelSection(
                sectionKey = "active",
                headerTitle = null,
                entries = labelUiState.activeLabels,
                activeLabelFilter = activeLabelFilter,
                onSelectLabel = onSelectLabel,
                onUpdateLabelConfig = onUpdateLabelConfig,
            )

            // Section 2: "Weitere Ereignisse" (uses identical string resource as Settings)
            labelSection(
                sectionKey = "other_events",
                headerTitle = { stringResource(R.string.settings_other_events_title) },
                entries = labelUiState.otherEventLabels,
                activeLabelFilter = activeLabelFilter,
                onSelectLabel = onSelectLabel,
                onUpdateLabelConfig = onUpdateLabelConfig,
            )

            // Section 3: Verborgene Labels
            labelSection(
                sectionKey = "hidden",
                headerTitle = { stringResource(R.string.labels_section_hidden) },
                entries = labelUiState.hiddenLabels,
                activeLabelFilter = activeLabelFilter,
                onSelectLabel = onSelectLabel,
                onUpdateLabelConfig = onUpdateLabelConfig,
            )

            // Section 4: Ignorierte Labels
            labelSection(
                sectionKey = "ignored",
                headerTitle = { stringResource(R.string.labels_section_ignored) },
                entries = labelUiState.ignoredLabels,
                activeLabelFilter = activeLabelFilter,
                onSelectLabel = onSelectLabel,
                onUpdateLabelConfig = onUpdateLabelConfig,
            )
        }
    }
}

private fun LazyListScope.labelSection(
    sectionKey: String,
    headerTitle: (@Composable () -> String)?,
    entries: List<LabelWithContactsUiModel>,
    activeLabelFilter: String?,
    onSelectLabel: (String) -> Unit,
    onUpdateLabelConfig: (String, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
) {
    if (entries.isEmpty()) return

    if (headerTitle != null) {
        item(key = "section_header_$sectionKey") {
            LabelsSectionHeader(title = headerTitle())
        }
    }

    items(
        items = entries,
        key = { it.label.name },
    ) { entry ->
        LabelCard(
            label = entry.label,
            isActive = activeLabelFilter == entry.label.name,
            assignedContacts = entry.assignedContacts,
            onSelectLabel = { onSelectLabel(entry.label.name) },
            onConfigChanged = onUpdateLabelConfig,
        )
    }
}

@Composable
private fun LabelsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = SpacingExtraSmall,
                end = SpacingExtraSmall,
                top = SpacingSmall,
            )
            .semantics { heading() },
    )
}

@Preview(showBackground = true)
@Composable
private fun LabelsScreenPreview() {
    BirthdayBuddyTheme {
        LabelsScreenContent(
            uiState = SampleData.homeUiState,
            labelUiState = LabelUiState(
                activeLabels = listOf(
                    LabelWithContactsUiModel(
                        label = LabelManagementModel(
                            name = "Familie",
                            isHiddenFromFilter = false,
                            isIgnored = false,
                            isSystem = true,
                            notificationsEnabled = true,
                            showInWidget = true,
                        ),
                        assignedContacts = SampleData.sampleContacts,
                    ),
                    LabelWithContactsUiModel(
                        label = LabelManagementModel(
                            name = ContactLabels.LABEL_NO_BIRTHDAY,
                            isHiddenFromFilter = false,
                            isIgnored = false,
                            isSystem = false,
                            notificationsEnabled = false,
                            showInWidget = false,
                        ),
                        assignedContacts = emptyList(),
                    ),
                ),
                otherEventLabels = listOf(
                    LabelWithContactsUiModel(
                        label = LabelManagementModel(
                            name = ContactLabels.LABEL_ANNIVERSARY,
                            isHiddenFromFilter = false,
                            isIgnored = false,
                            isSystem = false,
                            notificationsEnabled = true,
                            showInWidget = true,
                        ),
                        assignedContacts = SampleData.sampleContacts.take(2),
                    ),
                    LabelWithContactsUiModel(
                        label = LabelManagementModel(
                            name = ContactLabels.customEventLabel("Jahrestag"),
                            isHiddenFromFilter = false,
                            isIgnored = false,
                            isSystem = false,
                            notificationsEnabled = true,
                            showInWidget = true,
                        ),
                        assignedContacts = SampleData.sampleContacts.take(1),
                    ),
                ),
                hiddenLabels = listOf(
                    LabelWithContactsUiModel(
                        label = LabelManagementModel(
                            name = "Arbeit",
                            isHiddenFromFilter = true,
                            isIgnored = false,
                            isSystem = false,
                            notificationsEnabled = false,
                            showInWidget = false,
                        ),
                        assignedContacts = SampleData.sampleContacts.take(1),
                    ),
                ),
                ignoredLabels = listOf(
                    LabelWithContactsUiModel(
                        label = LabelManagementModel(
                            name = "Spam",
                            isHiddenFromFilter = false,
                            isIgnored = true,
                            isSystem = false,
                            notificationsEnabled = false,
                            showInWidget = false,
                        ),
                        assignedContacts = emptyList(),
                    ),
                ),
                allLabeledContactsCount = SampleData.sampleContacts.size,
            ),
            actions = SampleData.homeActions,
        )
    }
}
