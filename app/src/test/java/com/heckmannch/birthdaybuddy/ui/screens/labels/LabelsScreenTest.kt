package com.heckmannch.birthdaybuddy.ui.screens.labels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.model.LabelManagementModel
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.labels.LabelUiState
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.labels.LabelWithContactsUiModel
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * UI tests for [LabelsScreenContent] verifying label creation via `AddLabelDialog`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LabelsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addLabelDialog_confirmPersistsConfigAndDoesNotSelectHomeFilter() {
        val context = RuntimeEnvironment.getApplication()
        val placeholder = context.getString(R.string.labels_add_placeholder)
        val confirmText = context.getString(R.string.labels_add_confirm)

        val selectedLabels = mutableListOf<String?>()
        var savedLabel: LabelManagementModel? = null
        var labelUiState by mutableStateOf(LabelUiState())

        composeRule.setContent {
            BirthdayBuddyTheme {
                LabelsScreenContent(
                    uiState = SampleData.homeUiState.copy(selectedLabel = null),
                    labelUiState = labelUiState,
                    actions = SampleData.homeActions.copy(
                        onLabelSelected = { selectedLabels.add(it) },
                    ),
                    onUpdateLabelConfig = { name, hidden, ignored, isSystem, notificationsEnabled, showInWidget ->
                        val model = LabelManagementModel(
                            name = name,
                            isHiddenFromFilter = hidden,
                            isIgnored = ignored,
                            isSystem = isSystem,
                            notificationsEnabled = notificationsEnabled,
                            showInWidget = showInWidget,
                        )
                        savedLabel = model
                        labelUiState = labelUiState.copy(
                            activeLabels = labelUiState.activeLabels + LabelWithContactsUiModel(
                                label = model,
                                assignedContacts = emptyList(),
                            ),
                        )
                    },
                )
            }
        }

        // Open AddLabelDialog via header button
        composeRule.onNodeWithTag("add_label_button").performClick()
        composeRule.waitForIdle()

        // Enter label name and confirm
        composeRule.onNodeWithText(placeholder).performTextInput("  Sportverein  ")
        composeRule.onNodeWithText(confirmText).performClick()
        composeRule.waitForIdle()

        // Persisted config is trimmed and non-system with defaults enabled
        assertThat(savedLabel).isEqualTo(
            LabelManagementModel(
                name = "Sportverein",
                isHiddenFromFilter = false,
                isIgnored = false,
                isSystem = false,
                notificationsEnabled = true,
                showInWidget = true,
            )
        )
        // Home filter must NOT be changed to the empty label
        assertThat(selectedLabels).isEmpty()
        // Newly created label appears immediately in the LabelsScreen list
        composeRule.onNodeWithText("Sportverein").assertIsDisplayed()
    }
}
