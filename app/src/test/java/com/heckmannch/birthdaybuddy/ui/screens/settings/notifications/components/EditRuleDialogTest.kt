package com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.components

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
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
        val existingRule = NotificationRule(id = 1, daysBefore = 1, hour = 9, minute = 0)

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
}
