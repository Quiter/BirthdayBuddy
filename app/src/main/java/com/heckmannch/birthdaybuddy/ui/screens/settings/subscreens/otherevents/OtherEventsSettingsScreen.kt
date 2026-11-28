package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.otherevents

import androidx.annotation.VisibleForTesting
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.ui.components.settings.FoldingVisibility
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsFooter
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsGroup
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsNotice
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSection
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSemanticTone
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSubpageScaffold
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSwitchItem
import com.heckmannch.birthdaybuddy.ui.screens.home.components.labels.toDisplayLabel
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * Entry point for the "Other Events" settings subpage.
 */
@Composable
fun OtherEventsSettingsScreen(
    viewModel: OtherEventsViewModel,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    OtherEventsSettingsScreenContent(
        uiState = uiState,
        showBackButton = showBackButton,
        onOtherEventsEnabledChanged = { enabled ->
            viewModel.onIntent(OtherEventsIntent.SetOtherEventsEnabled(enabled))
        },
        onShowOtherEventsInHomeListChanged = { enabled ->
            viewModel.onIntent(OtherEventsIntent.SetShowOtherEventsInHomeList(enabled))
        },
        onEventLabelEnabledChanged = { eventKey, enabled ->
            viewModel.onIntent(OtherEventsIntent.SetEventLabelEnabled(eventKey, enabled))
        },
        onNavigateBack = onNavigateBack,
    )
}

/**
 * Stateless UI content for the "Other Events" settings subpage.
 *
 * Displays a global enable/disable toggle and an optional toggle to sort enabled non-birthday
 * events chronologically into the home screen list, followed by individual toggles for every
 * non-birthday date-field label discovered across the user's contacts (or an informative empty
 * notice when none are present).
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun OtherEventsSettingsScreenContent(
    uiState: OtherEventsUiState,
    showBackButton: Boolean = true,
    onOtherEventsEnabledChanged: (Boolean) -> Unit = {},
    onShowOtherEventsInHomeListChanged: (Boolean) -> Unit = {},
    onEventLabelEnabledChanged: (eventKey: String, enabled: Boolean) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
) {
    SettingsSubpageScaffold(
        title = stringResource(R.string.settings_other_events_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack,
    ) {
        SettingsSection(title = stringResource(R.string.settings_section_general)) {
            SettingsGroup {
                SettingsSwitchItem(
                    title = stringResource(R.string.other_events_enable),
                    icon = Icons.Rounded.Star,
                    checked = uiState.otherEventsEnabled,
                    onCheckedChange = onOtherEventsEnabledChanged,
                )
                FoldingVisibility(visible = uiState.otherEventsEnabled) {
                    SettingsSwitchItem(
                        title = stringResource(R.string.other_events_show_in_home_list),
                        subtitle = stringResource(R.string.other_events_show_in_home_list_desc),
                        icon = Icons.AutoMirrored.Rounded.ViewList,
                        checked = uiState.showOtherEventsInHomeList,
                        onCheckedChange = onShowOtherEventsInHomeListChanged,
                    )
                }
            }
            SettingsFooter(text = stringResource(R.string.other_events_desc))
        }

        FoldingVisibility(visible = uiState.otherEventsEnabled) {
            if (uiState.detectedEvents.isEmpty()) {
                SettingsNotice(
                    title = stringResource(R.string.other_events_empty_title),
                    message = stringResource(R.string.other_events_empty_desc),
                    icon = Icons.AutoMirrored.Rounded.EventNote,
                    tone = SettingsSemanticTone.Neutral,
                )
            } else {
                SettingsSection(title = stringResource(R.string.other_events_section_detected)) {
                    SettingsGroup {
                        uiState.detectedEvents.forEach { event ->
                            val subtitle = if (event.contactCount == 1) {
                                stringResource(R.string.labels_count_contacts_one)
                            } else {
                                stringResource(R.string.labels_count_contacts_many, event.contactCount)
                            }

                            SettingsSwitchItem(
                                title = event.eventKey.toDisplayLabel(),
                                subtitle = subtitle,
                                icon = eventIconForKey(event.eventKey),
                                checked = event.isEnabled,
                                onCheckedChange = { enabled ->
                                    onEventLabelEnabledChanged(event.eventKey, enabled)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun eventIconForKey(eventKey: String): ImageVector = when (eventKey) {
    ContactLabels.LABEL_ANNIVERSARY -> Icons.Rounded.Favorite
    ContactLabels.LABEL_NAME_DAY -> Icons.Rounded.Celebration
    else -> Icons.Rounded.Event
}

@Preview(name = "Enabled with Detected Events", showBackground = true)
@Composable
private fun OtherEventsSettingsEnabledPreview() {
    BirthdayBuddyTheme {
        OtherEventsSettingsScreenContent(
            uiState = OtherEventsUiState(
                otherEventsEnabled = true,
                showOtherEventsInHomeList = true,
                detectedEvents = listOf(
                    DetectedOtherEventUiModel(
                        eventKey = ContactLabels.LABEL_ANNIVERSARY,
                        contactCount = 4,
                        isEnabled = true,
                    ),
                    DetectedOtherEventUiModel(
                        eventKey = ContactLabels.LABEL_NAME_DAY,
                        contactCount = 2,
                        isEnabled = true,
                    ),
                    DetectedOtherEventUiModel(
                        eventKey = ContactLabels.customEventLabel("Jahrestag"),
                        contactCount = 1,
                        isEnabled = false,
                    ),
                    DetectedOtherEventUiModel(
                        eventKey = ContactLabels.LABEL_OTHER_EVENT,
                        contactCount = 3,
                        isEnabled = true,
                    ),
                ),
            ),
        )
    }
}

@Preview(name = "Enabled Empty State", showBackground = true)
@Composable
private fun OtherEventsSettingsEmptyPreview() {
    BirthdayBuddyTheme {
        OtherEventsSettingsScreenContent(
            uiState = OtherEventsUiState(
                otherEventsEnabled = true,
                detectedEvents = emptyList(),
            ),
        )
    }
}

@Preview(name = "Disabled", showBackground = true)
@Composable
private fun OtherEventsSettingsDisabledPreview() {
    BirthdayBuddyTheme {
        OtherEventsSettingsScreenContent(
            uiState = OtherEventsUiState(
                otherEventsEnabled = false,
                detectedEvents = emptyList(),
            ),
        )
    }
}
