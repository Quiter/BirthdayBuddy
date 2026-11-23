package com.heckmannch.birthdaybuddy.ui.components

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
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Unit tests for [AppTopHeader] ensuring state transitions between Title Mode and Search Bar Mode.
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
}
