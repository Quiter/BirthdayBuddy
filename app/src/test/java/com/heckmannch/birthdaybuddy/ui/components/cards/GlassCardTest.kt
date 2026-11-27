package com.heckmannch.birthdaybuddy.ui.components.cards

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsItem
import com.heckmannch.birthdaybuddy.ui.model.LabelManagementModel
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.labels.components.LabelCard
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.LabelHiddenDarkStart
import com.heckmannch.birthdaybuddy.ui.theme.LabelHiddenLightStart
import com.heckmannch.birthdaybuddy.ui.theme.LabelIgnoredDarkStart
import com.heckmannch.birthdaybuddy.ui.theme.LabelIgnoredLightStart
import com.heckmannch.birthdaybuddy.ui.theme.defaultCardGradient
import com.heckmannch.birthdaybuddy.ui.theme.labelHiddenCardGradient
import com.heckmannch.birthdaybuddy.ui.theme.labelIgnoredCardGradient
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Unit tests for the centralized frosted glass card architecture in `ui.components.cards`
 * and its usage across Home ([ContactCard]), Labels ([LabelCard]), and Settings
 * ([SettingsItem]), as well as centralized color tokens in `Color.kt`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlassCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contactCard_clickTriggersDetailCallback() {
        var clicked = false

        composeRule.setContent {
            BirthdayBuddyTheme {
                ContactCard(
                    contact = SampleData.contact1,
                    onClick = { clicked = true }
                )
            }
        }

        composeRule.onNodeWithText(SampleData.contact1.fullName).assertIsDisplayed()
        composeRule.onNodeWithText(SampleData.contact1.fullName).performClick()
        composeRule.waitForIdle()

        assertThat(clicked).isTrue()
    }

    @Test
    fun labelCard_clickExpandsAndRevealsGranularControls() {
        val label = LabelManagementModel(
            name = "Familie",
            isHiddenFromFilter = false,
            isIgnored = false,
            isSystem = false,
            notificationsEnabled = true,
            showInWidget = true
        )

        composeRule.setContent {
            BirthdayBuddyTheme {
                LabelCard(
                    label = label,
                    isActive = false,
                    assignedContacts = emptyList(),
                    onSelectLabel = {},
                    onConfigChanged = { _, _, _, _, _, _ -> }
                )
            }
        }

        composeRule.onNodeWithText("Familie").assertIsDisplayed()
        composeRule.onNodeWithText("Granular controls").assertDoesNotExist()

        composeRule.onNodeWithText("Familie").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Granular controls").assertIsDisplayed()
    }

    @Test
    fun labelCard_ignoredAndHiddenRenderStatusTags() {
        val ignoredLabel = LabelManagementModel(
            name = "Spam",
            isHiddenFromFilter = false,
            isIgnored = true,
            isSystem = false,
            notificationsEnabled = false,
            showInWidget = false
        )
        val hiddenLabel = LabelManagementModel(
            name = "Work",
            isHiddenFromFilter = true,
            isIgnored = false,
            isSystem = false,
            notificationsEnabled = false,
            showInWidget = false
        )

        composeRule.setContent {
            BirthdayBuddyTheme {
                androidx.compose.foundation.layout.Column {
                    LabelCard(
                        label = ignoredLabel,
                        isActive = false,
                        assignedContacts = emptyList(),
                        onSelectLabel = {},
                        onConfigChanged = { _, _, _, _, _, _ -> }
                    )
                    LabelCard(
                        label = hiddenLabel,
                        isActive = false,
                        assignedContacts = emptyList(),
                        onSelectLabel = {},
                        onConfigChanged = { _, _, _, _, _, _ -> }
                    )
                }
            }
        }

        composeRule.onNodeWithText("Spam").assertIsDisplayed()
        composeRule.onNodeWithText("Ignore").assertIsDisplayed()
        composeRule.onNodeWithText("Work").assertIsDisplayed()
        composeRule.onNodeWithText("Hide").assertIsDisplayed()
    }

    @Test
    fun settingsItemAndGlassCard_supportClickableNavigationAndExpansion() {
        var navigated = false
        var expanded by mutableStateOf(false)

        composeRule.setContent {
            BirthdayBuddyTheme {
                androidx.compose.foundation.layout.Column {
                    SettingsItem(
                        onClick = { navigated = true },
                        title = "Theme",
                        subtitle = "Customize look"
                    )
                    GlassCardLayout(
                        gradientColors = defaultCardGradient(),
                        onClick = { expanded = !expanded },
                        expanded = expanded,
                        expandedContent = {
                            Text("Expanded card content")
                        }
                    ) {
                        Text("Expandable card")
                    }
                }
            }
        }

        composeRule.onNodeWithText("Theme").performClick()
        composeRule.waitForIdle()
        assertThat(navigated).isTrue()

        composeRule.onNodeWithText("Expanded card content").assertDoesNotExist()
        composeRule.onNodeWithText("Expandable card").performClick()
        composeRule.waitForIdle()
        assertThat(expanded).isTrue()
        composeRule.onNodeWithText("Expanded card content").assertIsDisplayed()
    }

    @Test
    fun labelCardGradients_resolveLightAndDarkColorsFromColorKt() {
        var lightIgnored: List<Color> = emptyList()
        var lightHidden: List<Color> = emptyList()
        var darkIgnored: List<Color> = emptyList()
        var darkHidden: List<Color> = emptyList()

        composeRule.setContent {
            BirthdayBuddyTheme(themeMode = ThemeMode.LIGHT) {
                lightIgnored = labelIgnoredCardGradient()
                lightHidden = labelHiddenCardGradient()
            }
            BirthdayBuddyTheme(themeMode = ThemeMode.DARK) {
                darkIgnored = labelIgnoredCardGradient()
                darkHidden = labelHiddenCardGradient()
            }
        }

        assertThat(lightIgnored.first()).isEqualTo(LabelIgnoredLightStart)
        assertThat(lightHidden.first()).isEqualTo(LabelHiddenLightStart)
        assertThat(darkIgnored.first()).isEqualTo(LabelIgnoredDarkStart)
        assertThat(darkHidden.first()).isEqualTo(LabelHiddenDarkStart)
    }
}
