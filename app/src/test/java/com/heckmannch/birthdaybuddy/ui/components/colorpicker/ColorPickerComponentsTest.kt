package com.heckmannch.birthdaybuddy.ui.components.colorpicker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.CalendarPresetColors
import com.heckmannch.birthdaybuddy.ui.util.toHexString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Robolectric Compose UI tests for [ColorPickerDialog] and [ColorPresetGrid], verifying preset
 * selection, hex input validation, cancellation, and landscape layout rendering.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ColorPickerComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun presetGrid_selectsSwatchAndNotifiesCaller() {
        var selected: Color? = null
        val target = CalendarPresetColors[2]

        composeRule.setContent {
            BirthdayBuddyTheme {
                ColorPresetGrid(
                    colors = CalendarPresetColors,
                    selected = CalendarPresetColors.first(),
                    onSelect = { selected = it },
                    columns = PICKER_PRESET_COLUMNS,
                )
            }
        }

        composeRule.onNodeWithContentDescription(target.toHexString()).performClick()

        assertThat(selected).isEqualTo(target)
    }

    @Test
    fun dialog_selectingPresetUpdatesSelectionAndSavesPresetColor() {
        var confirmed: Color? = null
        var dismissed = false
        val preset = CalendarPresetColors[3]

        composeRule.setContent {
            BirthdayBuddyTheme {
                ColorPickerDialog(
                    initialColor = Color(0xFFE91E63),
                    title = "Pick Color",
                    onDismissRequest = { dismissed = true },
                    onColorSelected = { confirmed = it },
                    presets = CalendarPresetColors,
                )
            }
        }

        composeRule.onNodeWithContentDescription(preset.toHexString()).performClick()
        composeRule.onNodeWithText("Save").performClick()

        assertThat(confirmed).isEqualTo(preset)
        assertThat(dismissed).isTrue()
    }

    @Test
    fun dialog_typingValidHexSavesEnteredColor() {
        var confirmed: Color? = null

        composeRule.setContent {
            BirthdayBuddyTheme {
                ColorPickerDialog(
                    initialColor = Color(0xFFE91E63),
                    title = "Pick Color",
                    onDismissRequest = {},
                    onColorSelected = { confirmed = it },
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextReplacement("00bcd4")
        composeRule.onNodeWithText("Save").performClick()

        assertThat(confirmed).isEqualTo(Color(0xFF00BCD4))
    }

    @Test
    fun dialog_typingInvalidHexFallsBackToCurrentHsvColorOnSave() {
        var confirmed: Color? = null
        val initial = Color(0xFFE91E63)

        composeRule.setContent {
            BirthdayBuddyTheme {
                ColorPickerDialog(
                    initialColor = initial,
                    title = "Pick Color",
                    onDismissRequest = {},
                    onColorSelected = { confirmed = it },
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextReplacement("ZZ12")
        composeRule.onNodeWithText("Save").performClick()

        assertThat(confirmed).isEqualTo(initial)
    }

    @Test
    fun dialog_cancelDismissesWithoutConfirming() {
        var confirmed: Color? = null
        var dismissed = false

        composeRule.setContent {
            BirthdayBuddyTheme {
                ColorPickerDialog(
                    initialColor = Color(0xFFE91E63),
                    title = "Pick Color",
                    onDismissRequest = { dismissed = true },
                    onColorSelected = { confirmed = it },
                )
            }
        }

        composeRule.onNodeWithText("Cancel").performClick()

        assertThat(dismissed).isTrue()
        assertThat(confirmed).isNull()
    }

    @Test
    @Config(sdk = [35], qualifiers = "w891dp-h411dp-land")
    fun dialog_landscape_rendersAllControlsAndConfirms() {
        var confirmed: Color? = null

        composeRule.setContent {
            BirthdayBuddyTheme {
                ColorPickerDialog(
                    initialColor = Color(0xFF2196F3),
                    title = "Pick Color",
                    onDismissRequest = {},
                    onColorSelected = { confirmed = it },
                    presets = CalendarPresetColors,
                )
            }
        }

        composeRule.onNodeWithText("Pick Color").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Color shade panel").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Hue spectrum slider").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsDisplayed().performClick()

        assertThat(confirmed).isEqualTo(Color(0xFF2196F3))
    }
}
