package com.heckmannch.birthdaybuddy.ui.components.bars

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsPageHeader
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.screens.home.components.header.HomeHeader
import com.heckmannch.birthdaybuddy.ui.screens.labels.components.LabelsHeader
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Unit tests for [AppTopHeader], [HomeHeader], [SettingsPageHeader], and [LabelsHeader] ensuring central layout inheritance,
 * navigation icon display, action buttons, and search mode transitions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppTopHeaderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appTopHeader_inTitleMode_displaysTitleAndOpenSearchButton() {
        composeRule.setContent {
            BirthdayBuddyTheme {
                val focusRequester = remember { FocusRequester() }
                AppTopHeader(
                    title = "BirthdayBuddy",
                    isSearchActive = false,
                    searchQuery = "",
                    onSearchQueryChange = {},
                    onSearchActiveChange = {},
                    focusRequester = focusRequester,
                )
            }
        }

        composeRule.onNodeWithText("BirthdayBuddy").assertIsDisplayed()
        composeRule.onNodeWithTag("open_search_button").assertIsDisplayed()
    }

    @Test
    fun appTopHeader_clickingOpenSearch_invokesCallback() {
        var isSearchActive by mutableStateOf(false)

        composeRule.setContent {
            BirthdayBuddyTheme {
                val focusRequester = remember { FocusRequester() }
                AppTopHeader(
                    title = "BirthdayBuddy",
                    isSearchActive = isSearchActive,
                    searchQuery = "",
                    onSearchQueryChange = {},
                    onSearchActiveChange = { active -> isSearchActive = active },
                    focusRequester = focusRequester,
                )
            }
        }

        composeRule.onNodeWithTag("open_search_button").performClick()
        composeRule.waitForIdle()

        assertThat(isSearchActive).isTrue()
        composeRule.onNodeWithTag("search_field").assertIsDisplayed()
        composeRule.onNodeWithTag("close_search_button").assertIsDisplayed()
    }

    @Test
    fun appTopHeader_clickingCloseSearch_deactivatesSearchMode() {
        var isSearchActive by mutableStateOf(true)

        composeRule.setContent {
            BirthdayBuddyTheme {
                val focusRequester = remember { FocusRequester() }
                AppTopHeader(
                    title = "BirthdayBuddy",
                    isSearchActive = isSearchActive,
                    searchQuery = "Mustermann",
                    onSearchQueryChange = {},
                    onSearchActiveChange = { active -> isSearchActive = active },
                    focusRequester = focusRequester,
                )
            }
        }

        composeRule.onNodeWithTag("close_search_button").performClick()
        composeRule.waitForIdle()

        assertThat(isSearchActive).isFalse()
        composeRule.onNodeWithText("BirthdayBuddy").assertIsDisplayed()
        composeRule.onNodeWithTag("open_search_button").assertIsDisplayed()
    }

    @Test
    fun homeHeader_displaysLogoTitleAndAddContactAction() {
        var addContactClicked = false

        composeRule.setContent {
            BirthdayBuddyTheme {
                HomeHeader(
                    searchQuery = "",
                    isSearchActive = false,
                    actions = HomeActions.previewDefaults().copy(
                        onAddContact = { addContactClicked = true },
                    ),
                )
            }
        }

        composeRule.onNodeWithText("BirthdayBuddy").assertIsDisplayed()
        composeRule.onNodeWithTag("add_contact_button").assertIsDisplayed()
        composeRule.onNodeWithTag("open_search_button").assertIsDisplayed()

        composeRule.onNodeWithTag("add_contact_button").performClick()
        composeRule.waitForIdle()

        assertThat(addContactClicked).isTrue()
    }

    @Test
    fun settingsPageHeader_displaysTitleAndBackButton_withoutSearchButton() {
        var backClicked = false

        composeRule.setContent {
            BirthdayBuddyTheme {
                SettingsPageHeader(
                    title = "Benachrichtigungen",
                    showBackButton = true,
                    onNavigateBack = { backClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Benachrichtigungen").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_back_button").assertIsDisplayed()
        composeRule.onNodeWithTag("open_search_button").assertDoesNotExist()

        composeRule.onNodeWithTag("settings_back_button").performClick()
        composeRule.waitForIdle()

        assertThat(backClicked).isTrue()
    }

    @Test
    fun settingsPageHeader_mainScreen_hidesBackButtonAndSearchButton() {
        composeRule.setContent {
            BirthdayBuddyTheme {
                SettingsPageHeader(
                    title = "Einstellungen",
                    showBackButton = false,
                )
            }
        }

        composeRule.onNodeWithText("Einstellungen").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_back_button").assertDoesNotExist()
        composeRule.onNodeWithTag("open_search_button").assertDoesNotExist()
    }

    @Test
    fun labelsHeader_displaysTitleAndAddLabelAction_withoutSearchButton() {
        var addLabelClicked = false

        composeRule.setContent {
            BirthdayBuddyTheme {
                LabelsHeader(
                    onAddLabel = { addLabelClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Labels").assertIsDisplayed()
        composeRule.onNodeWithTag("add_label_button").assertIsDisplayed()
        composeRule.onNodeWithTag("open_search_button").assertDoesNotExist()

        composeRule.onNodeWithTag("add_label_button").performClick()
        composeRule.waitForIdle()

        assertThat(addLabelClicked).isTrue()
    }
}
