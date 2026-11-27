package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.notifications.components

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditRuleDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun duplicateDaysBefore_showsErrorMessage_andDisablesSaveButton() {
        // Arrange: 0 and 1 are already taken. Default candidate chosen is 2.
        // User then enters "0" (conflict with 0).
        composeRule.setContent {
            BirthdayBuddyTheme {
                EditRuleDialog(
                    existingDaysBefore = setOf(0, 1),
                    onDismiss = {},
                    onConfirm = { _, _, _ -> },
                )
            }
        }

        val saveText = context.getString(R.string.dialog_save)
        val errorText = context.getString(R.string.dialog_rule_duplicate_error)

        // Save should initially be enabled (for 2 days before)
        val saveNode = composeRule.onNodeWithText(saveText)
        saveNode.assertIsEnabled()

        // Replace text with "0" -> should trigger duplicate error
        composeRule.onNode(hasSetTextAction()).performTextReplacement("0")

        // Assert: error message shown and save button disabled
        composeRule.onNodeWithText(errorText).assertExists()
        saveNode.assertIsNotEnabled()
    }

    @Test
    fun nonDuplicateDaysBefore_enablesSaveButton() {
        composeRule.setContent {
            BirthdayBuddyTheme {
                EditRuleDialog(
                    existingDaysBefore = setOf(0, 1),
                    onDismiss = {},
                    onConfirm = { _, _, _ -> },
                )
            }
        }

        val saveText = context.getString(R.string.dialog_save)
        val errorText = context.getString(R.string.dialog_rule_duplicate_error)

        val saveNode = composeRule.onNodeWithText(saveText)
        saveNode.assertIsEnabled()

        // Change from 2 to 3 (which is also not taken)
        composeRule.onNode(hasSetTextAction()).performTextReplacement("3")

        saveNode.assertIsEnabled()
        composeRule.onNodeWithText(errorText).assertDoesNotExist()
    }

    @Test
    fun editingExistingRule_withSameDaysBefore_allowsSaving() {
        val existingRule = NotificationRule(daysBefore = 1, hour = 9, minute = 0)

        // When editing existingRule, existingDaysBefore excludes id=1 (only has id=2 with daysBefore=0)
        composeRule.setContent {
            BirthdayBuddyTheme {
                EditRuleDialog(
                    rule = existingRule,
                    existingDaysBefore = setOf(0), // rule 2 is at 0 days before
                    onDismiss = {},
                    onConfirm = { _, _, _ -> },
                )
            }
        }

        val saveText = context.getString(R.string.dialog_save)
        val errorText = context.getString(R.string.dialog_rule_duplicate_error)

        val saveNode = composeRule.onNodeWithText(saveText)
        saveNode.assertIsEnabled()
        composeRule.onNodeWithText(errorText).assertDoesNotExist()
    }

    @Test
    fun daysOver365_showsRangeError_andDisablesSaveButton() {
        composeRule.setContent {
            BirthdayBuddyTheme {
                EditRuleDialog(
                    existingDaysBefore = emptySet(),
                    onDismiss = {},
                    onConfirm = { _, _, _ -> },
                )
            }
        }

        val saveNode = composeRule.onNodeWithText(context.getString(R.string.dialog_save))
        val rangeErrorText = context.getString(R.string.dialog_rule_range_error, 0, 365)

        composeRule.onNode(hasSetTextAction()).performTextReplacement("366")

        composeRule.onNodeWithText(rangeErrorText).assertExists()
        saveNode.assertIsNotEnabled()
    }

    @Test
    fun days365_enablesSaveButton_andConfirms365() {
        var confirmedDays: Int? = null
        composeRule.setContent {
            BirthdayBuddyTheme {
                EditRuleDialog(
                    existingDaysBefore = emptySet(),
                    onDismiss = {},
                    onConfirm = { days, _, _ -> confirmedDays = days },
                )
            }
        }

        val saveNode = composeRule.onNodeWithText(context.getString(R.string.dialog_save))
        val rangeErrorText = context.getString(R.string.dialog_rule_range_error, 0, 365)

        composeRule.onNode(hasSetTextAction()).performTextReplacement("365")

        composeRule.onNodeWithText(rangeErrorText).assertDoesNotExist()
        saveNode.assertIsEnabled()
        saveNode.performClick()
        assertThat(confirmedDays).isEqualTo(365)
    }

    @Test
    fun weeksLargeNumber_preventsIntOverflow_showsRangeError_andDisablesSaveButton() {
        composeRule.setContent {
            BirthdayBuddyTheme {
                EditRuleDialog(
                    existingDaysBefore = emptySet(),
                    onDismiss = {},
                    onConfirm = { _, _, _ -> },
                )
            }
        }

        val saveNode = composeRule.onNodeWithText(context.getString(R.string.dialog_save))
        val rangeErrorText = context.getString(R.string.dialog_rule_range_error, 0, 365)
        val weeksLabel = context.resources.getQuantityString(
            R.plurals.dialog_rule_unit_weeks,
            500000000,
            500000000
        )

        composeRule.onNode(hasSetTextAction()).performTextReplacement("500000000")
        composeRule.onNodeWithText(weeksLabel).performClick()

        composeRule.onNodeWithText(rangeErrorText).assertExists()
        saveNode.assertIsNotEnabled()
    }

    @Test
    fun weeks53_showsRangeError_andDisablesSaveButton() {
        composeRule.setContent {
            BirthdayBuddyTheme {
                EditRuleDialog(
                    existingDaysBefore = emptySet(),
                    onDismiss = {},
                    onConfirm = { _, _, _ -> },
                )
            }
        }

        val saveNode = composeRule.onNodeWithText(context.getString(R.string.dialog_save))
        val rangeErrorText = context.getString(R.string.dialog_rule_range_error, 0, 365)
        val weeksLabel = context.resources.getQuantityString(
            R.plurals.dialog_rule_unit_weeks,
            53,
            53
        )

        composeRule.onNode(hasSetTextAction()).performTextReplacement("53")
        composeRule.onNodeWithText(weeksLabel).performClick()

        composeRule.onNodeWithText(rangeErrorText).assertExists()
        saveNode.assertIsNotEnabled()
    }

    @Test
    fun weeks52_enablesSaveButton_andConfirms364Days() {
        var confirmedDays: Int? = null
        composeRule.setContent {
            BirthdayBuddyTheme {
                EditRuleDialog(
                    existingDaysBefore = emptySet(),
                    onDismiss = {},
                    onConfirm = { days, _, _ -> confirmedDays = days },
                )
            }
        }

        val saveNode = composeRule.onNodeWithText(context.getString(R.string.dialog_save))
        val rangeErrorText = context.getString(R.string.dialog_rule_range_error, 0, 365)
        val weeksLabel = context.resources.getQuantityString(
            R.plurals.dialog_rule_unit_weeks,
            52,
            52
        )

        composeRule.onNode(hasSetTextAction()).performTextReplacement("52")
        composeRule.onNodeWithText(weeksLabel).performClick()

        composeRule.onNodeWithText(rangeErrorText).assertDoesNotExist()
        saveNode.assertIsEnabled()
        saveNode.performClick()
        assertThat(confirmedDays).isEqualTo(364)
    }

    @Test
    fun blankInput_disablesSaveButton_withoutRangeError() {
        composeRule.setContent {
            BirthdayBuddyTheme {
                EditRuleDialog(
                    existingDaysBefore = emptySet(),
                    onDismiss = {},
                    onConfirm = { _, _, _ -> },
                )
            }
        }

        val saveNode = composeRule.onNodeWithText(context.getString(R.string.dialog_save))
        val rangeErrorText = context.getString(R.string.dialog_rule_range_error, 0, 365)

        composeRule.onNode(hasSetTextAction()).performTextReplacement("")

        composeRule.onNodeWithText(rangeErrorText).assertDoesNotExist()
        saveNode.assertIsNotEnabled()
    }
}

