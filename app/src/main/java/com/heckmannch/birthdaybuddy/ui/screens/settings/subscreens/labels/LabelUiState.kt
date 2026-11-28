package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.labels

import androidx.compose.runtime.Immutable
import com.heckmannch.birthdaybuddy.ui.model.LabelManagementModel

/**
 * UI model pairing a configurable label (or pseudo-label) with its assigned contact count.
 *
 * @property label Configuration state of the label.
 * @property contactsCount Number of unfiltered contacts belonging to this label.
 */
@Immutable
data class LabelItemUiModel(
    val label: LabelManagementModel,
    val contactsCount: Int = 0,
)

/**
 * Coherent, sectioned UI state for the label management screen and global label toggle.
 *
 * @property labelsEnabled Whether label management and filtering are enabled globally.
 * @property activeLabels Unrestricted active regular labels (and "No Birthday" if active).
 * @property otherEventLabels Active pseudo-labels for enabled "Other Events".
 * @property hiddenLabels Labels hidden from the home filter bar (`isHiddenFromFilter && !isIgnored`).
 * @property ignoredLabels Fully ignored labels (`isIgnored`).
 * @property allLabeledContactsCount Total count of contacts with at least one regular label.
 */
@Immutable
data class LabelUiState(
    val labelsEnabled: Boolean = true,
    val activeLabels: List<LabelItemUiModel> = emptyList(),
    val otherEventLabels: List<LabelItemUiModel> = emptyList(),
    val hiddenLabels: List<LabelItemUiModel> = emptyList(),
    val ignoredLabels: List<LabelItemUiModel> = emptyList(),
    val allLabeledContactsCount: Int = 0,
)
