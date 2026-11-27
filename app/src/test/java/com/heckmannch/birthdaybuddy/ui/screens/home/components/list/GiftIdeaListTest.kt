package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tests for [GiftIdeaList] and [GiftIdeaItem] validating debouncing, focus tracking,
 * and prevention of race conditions caused by asynchronous Room database echoes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GiftIdeaListTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun SemanticsNodeInteraction.assertEditableText(expected: String) {
        val editable = fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text ?: ""
        assertThat(editable).isEqualTo(expected)
    }

    @Test
    fun giftIdeaList_rapidTyping_debouncesCallsToOnTextChange() {
        val idea = GiftIdea(id = "idea_1", text = "")
        val textChanges = mutableListOf<String>()

        composeRule.mainClock.autoAdvance = false

        composeRule.setContent {
            BirthdayBuddyTheme {
                GiftIdeaList(
                    giftIdeas = listOf(idea),
                    newlyAddedId = null,
                    onAddNewIdea = {},
                    onCheckedChange = { _, _ -> },
                    onTextChange = { _, newText -> textChanges.add(newText) },
                    onDelete = {},
                    onDone = {},
                )
            }
        }

        val textNode = composeRule.onNodeWithTag("gift_text_field")
        textNode.performTextInput("A")
        composeRule.mainClock.advanceTimeBy(100)
        textNode.performTextInput("B")
        composeRule.mainClock.advanceTimeBy(100)
        textNode.performTextInput("C")
        composeRule.mainClock.advanceTimeByFrame()

        // In-flight text field must show "ABC" immediately
        textNode.assertEditableText("ABC")

        // Before 300ms has elapsed since the last keystroke, onTextChange must not have fired yet
        assertThat(textChanges).isEmpty()

        // Advance beyond the 300ms debounce threshold
        composeRule.mainClock.advanceTimeBy(350)

        // Exactly one debounced emission with the full text "ABC"
        assertThat(textChanges).containsExactly("ABC")
    }

    @Test
    fun giftIdeaList_externalEchoDuringTyping_doesNotOverwriteLocalText() {
        var ideaState by mutableStateOf(GiftIdea(id = "idea_1", text = ""))

        composeRule.setContent {
            BirthdayBuddyTheme {
                GiftIdeaList(
                    giftIdeas = listOf(ideaState),
                    newlyAddedId = null,
                    onAddNewIdea = {},
                    onCheckedChange = { _, _ -> },
                    onTextChange = { _, _ -> },
                    onDelete = {},
                    onDone = {},
                )
            }
        }

        val textNode = composeRule.onNodeWithTag("gift_text_field")
        textNode.performTextInput("Socken")
        textNode.assertEditableText("Socken")

        // Simulate asynchronous Room flow emitting an outdated echo or intermediate write
        ideaState = ideaState.copy(text = "S")
        composeRule.waitForIdle()

        // Text in the focused text field must NOT be overwritten by the stale external state
        textNode.assertEditableText("Socken")
    }

    @Test
    fun giftIdeaList_focusLoss_immediatelyCommitsTextChange() {
        val idea = GiftIdea(id = "idea_1", text = "")
        val textChanges = mutableListOf<String>()
        var focusManager: FocusManager? = null

        composeRule.mainClock.autoAdvance = false

        composeRule.setContent {
            focusManager = LocalFocusManager.current
            BirthdayBuddyTheme {
                GiftIdeaList(
                    giftIdeas = listOf(idea),
                    newlyAddedId = null,
                    onAddNewIdea = {},
                    onCheckedChange = { _, _ -> },
                    onTextChange = { _, newText -> textChanges.add(newText) },
                    onDelete = {},
                    onDone = {},
                )
            }
        }

        val textNode = composeRule.onNodeWithTag("gift_text_field")
        textNode.performTextInput("Buch")
        composeRule.mainClock.advanceTimeByFrame()

        // Before 300ms debounce finishes, onTextChange has not fired
        assertThat(textChanges).isEmpty()

        // Trigger focus loss explicitly
        focusManager?.clearFocus()
        composeRule.mainClock.advanceTimeByFrame()

        // Focus loss must immediately commit the pending change without waiting for 300ms
        assertThat(textChanges).containsExactly("Buch")
    }

    @Test
    fun giftIdeaList_keyboardOnNext_immediatelyCommitsTextChangeAndCallsOnDone() {
        val idea = GiftIdea(id = "idea_1", text = "")
        val textChanges = mutableListOf<String>()
        var doneIdea: GiftIdea? = null

        composeRule.mainClock.autoAdvance = false

        composeRule.setContent {
            BirthdayBuddyTheme {
                GiftIdeaList(
                    giftIdeas = listOf(idea),
                    newlyAddedId = null,
                    onAddNewIdea = {},
                    onCheckedChange = { _, _ -> },
                    onTextChange = { _, newText -> textChanges.add(newText) },
                    onDelete = {},
                    onDone = { doneIdea = it },
                )
            }
        }

        val textNode = composeRule.onNodeWithTag("gift_text_field")
        textNode.performTextInput("Kaffee")
        composeRule.mainClock.advanceTimeByFrame()

        // Trigger IME Next action before debounce timer expires
        textNode.performImeAction()
        composeRule.mainClock.advanceTimeByFrame()

        // Both onTextChange and onDone must be triggered with the updated text
        assertThat(textChanges).containsExactly("Kaffee")
        assertThat(doneIdea).isNotNull()
        assertThat(doneIdea?.text).isEqualTo("Kaffee")
    }
}
