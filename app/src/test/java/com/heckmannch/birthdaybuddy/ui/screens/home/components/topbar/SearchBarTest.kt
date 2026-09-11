package com.heckmannch.birthdaybuddy.ui.screens.home.components.topbar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Unit and integration tests for [SearchBar] ensuring typing order, cursor stability,
 * clear action handling, and seamless external synchronization without text scrambling.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SearchBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun SemanticsNodeInteraction.assertEditableText(expected: String) {
        val editable = fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text ?: ""
        assertThat(editable).isEqualTo(expected)
    }

    @Test
    fun searchBar_rapidTypingWithDelayedEcho_preservesExactCharacterOrder() {
        var externalQuery by mutableStateOf("")
        val emittedQueries = mutableListOf<String>()

        composeRule.setContent {
            BirthdayBuddyTheme {
                val focusRequester = remember { FocusRequester() }
                SearchBar(
                    query = externalQuery,
                    placeholder = "Search...",
                    onQueryChange = { newQuery ->
                        emittedQueries.add(newQuery)
                        // Do not immediately update externalQuery to simulate asynchronous / delayed StateFlow emission
                    },
                    onClearQuery = { externalQuery = "" },
                    onSettingsClick = {},
                    focusRequester = focusRequester
                )
            }
        }

        // Type characters sequentially simulating rapid keystrokes before parent catches up
        val searchNode = composeRule.onNodeWithTag("search_field")
        searchNode.performTextInput("B")
        searchNode.performTextInput("i")
        searchNode.performTextInput("r")
        searchNode.performTextInput("t")
        searchNode.performTextInput("h")
        searchNode.performTextInput("d")
        searchNode.performTextInput("a")
        searchNode.performTextInput("y")

        // In-flight text field must contain "Birthday" without reversed or scrambled letters
        searchNode.assertEditableText("Birthday")

        // Now deliver the delayed echoes one by one to verify local state is not reverted
        emittedQueries.forEach { queryEcho ->
            externalQuery = queryEcho
            composeRule.waitForIdle()
            searchNode.assertEditableText("Birthday")
        }

        assertThat(emittedQueries).contains("Birthday")
    }

    @Test
    fun searchBar_typingInMiddleOfWord_insertsCorrectlyWithoutJumpingToEnd() {
        var externalQuery by mutableStateOf("ac")

        composeRule.setContent {
            BirthdayBuddyTheme {
                val focusRequester = remember { FocusRequester() }
                SearchBar(
                    query = externalQuery,
                    placeholder = "Search...",
                    onQueryChange = { externalQuery = it },
                    onClearQuery = { externalQuery = "" },
                    onSettingsClick = {},
                    focusRequester = focusRequester
                )
            }
        }

        val searchNode = composeRule.onNodeWithTag("search_field")
        searchNode.assertEditableText("ac")

        // Move cursor between 'a' and 'c' (index 1)
        searchNode.performTextInputSelection(TextRange(1))

        // Insert 'b'
        searchNode.performTextInput("b")
        composeRule.waitForIdle()

        searchNode.assertEditableText("abc")
        assertThat(externalQuery).isEqualTo("abc")
    }

    @Test
    fun searchBar_clearButton_clearsInputImmediatelyAndNotifiesCallback() {
        var externalQuery by mutableStateOf("")
        var clearCalled = false

        composeRule.setContent {
            BirthdayBuddyTheme {
                val focusRequester = remember { FocusRequester() }
                SearchBar(
                    query = externalQuery,
                    placeholder = "Search...",
                    onQueryChange = { externalQuery = it },
                    onClearQuery = {
                        clearCalled = true
                        externalQuery = ""
                    },
                    onSettingsClick = {},
                    focusRequester = focusRequester
                )
            }
        }

        val searchNode = composeRule.onNodeWithTag("search_field")
        searchNode.performTextInput("Mustermann")
        composeRule.waitForIdle()

        searchNode.assertEditableText("Mustermann")

        // Clear button (Close icon) should be visible
        val clearButton = composeRule.onNodeWithTag("clear_search_button")
        clearButton.assertIsDisplayed()
        clearButton.performClick()
        composeRule.waitForIdle()

        // Text field is cleared and callback was fired
        searchNode.assertEditableText("")
        assertThat(clearCalled).isTrue()
        assertThat(externalQuery).isEmpty()
    }

    @Test
    fun searchBar_externalQueryReset_updatesTextCorrectly() {
        var externalQuery by mutableStateOf("Initial")

        composeRule.setContent {
            BirthdayBuddyTheme {
                val focusRequester = remember { FocusRequester() }
                SearchBar(
                    query = externalQuery,
                    placeholder = "Search...",
                    onQueryChange = { externalQuery = it },
                    onClearQuery = { externalQuery = "" },
                    onSettingsClick = {},
                    focusRequester = focusRequester
                )
            }
        }

        val searchNode = composeRule.onNodeWithTag("search_field")
        searchNode.assertEditableText("Initial")

        // Programmatically reset the query from external state
        externalQuery = ""
        composeRule.waitForIdle()
        searchNode.assertEditableText("")

        // Programmatically update to another query
        externalQuery = "ExternalUpdate"
        composeRule.waitForIdle()
        searchNode.assertEditableText("ExternalUpdate")
    }
}
