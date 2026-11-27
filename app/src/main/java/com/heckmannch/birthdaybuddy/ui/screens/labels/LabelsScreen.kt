package com.heckmannch.birthdaybuddy.ui.screens.labels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * Screen displaying and managing contact labels.
 *
 * ### ARCHITECTURAL RULE:
 * [LabelsScreen] displays ONLY label management cards and label options.
 * Contacts are exclusively rendered on the main dashboard (HomeScreen).
 * Do NOT append contact lists or contact groups below the label cards.
 * Label creation is triggered via the [LabelsHeader] action icon (NO FloatingActionButton).
 *
 * Uses expandable frosted glass cards built on the central `GlassCardLayout`.
 * Each card allows:
 * - Direct tap on card surface or expand button: Opens/closes label details and granular controls.
 * - Tap on leading selection indicator: Selects/activates the label filter for the app.
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
    val allContacts = uiState.contacts ?: emptyList()
    val availableLabels = labelUiState.labels

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
                    onConfigChanged = onUpdateLabelConfig,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LabelsScreenPreview() {
    BirthdayBuddyTheme {
        LabelsScreenContent(
            uiState = SampleData.homeUiState,
            labelUiState = LabelUiState(
                labels = listOf(
                    LabelManagementModel(
                        name = "Familie",
                        isHiddenFromFilter = false,
                        isIgnored = false,
                        isSystem = true,
                        notificationsEnabled = true,
                        showInWidget = true,
                    ),
                    LabelManagementModel(
                        name = "Freunde",
                        isHiddenFromFilter = false,
                        isIgnored = false,
                        isSystem = false,
                        notificationsEnabled = true,
                        showInWidget = true,
                    ),
                    LabelManagementModel(
                        name = "Arbeit",
                        isHiddenFromFilter = true,
                        isIgnored = false,
                        isSystem = false,
                        notificationsEnabled = false,
                        showInWidget = false,
                    ),
                )
            ),
            actions = SampleData.homeActions,
        )
    }
}
