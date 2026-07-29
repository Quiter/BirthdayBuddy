package com.heckmannch.birthdaybuddy.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ColorPickerDialogTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun dialog_rendersCorrectly_andHandlesCancel() {
        var dismissClicked = false

        composeTestRule.setContent {
            BirthdayBuddyTheme {
                ColorPickerDialog(
                    initialColor = Color.Red,
                    title = "Wähle eine Farbe",
                    onDismissRequest = { dismissClicked = true },
                    onColorSelected = {}
                )
            }
        }

        // Title should be displayed
        composeTestRule.onNodeWithText("Wähle eine Farbe").assertIsDisplayed()

        // Cancel button click
        val cancelText = composeTestRule.activity.getString(R.string.dialog_cancel)
        composeTestRule.onNodeWithText(cancelText).performClick()

        assertTrue(dismissClicked)
    }

    @Test
    fun dialog_handlesConfirm_whenColorIsValid() {
        var selectedColor: Color? = null

        composeTestRule.setContent {
            BirthdayBuddyTheme {
                ColorPickerDialog(
                    initialColor = Color.Red,
                    title = "Wähle eine Farbe",
                    onDismissRequest = { },
                    onColorSelected = { selectedColor = it }
                )
            }
        }

        // Click save
        val saveText = composeTestRule.activity.getString(R.string.dialog_save)
        composeTestRule.onNodeWithText(saveText).performClick()

        // selectedColor should be Color.Red (which is what we initialized with)
        assertTrue(selectedColor != null)
    }
}
