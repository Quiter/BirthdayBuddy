package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tests for [BirthdayList] and [rememberSkipPlacementAnimation], verifying that filter/query
 * changes skip placement animations for exactly one composition frame without triggering a
 * second recomposition via [androidx.compose.runtime.SideEffect].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BirthdayListTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rememberSkipPlacementAnimation_labelChange_skipsOnceWithoutExtraRecomposition() {
        var selectedLabel by mutableStateOf<String?>(null)
        var searchQuery by mutableStateOf("")
        var unrelatedTrigger by mutableIntStateOf(0)

        var compositionCount = 0
        val observedSkipValues = mutableListOf<Boolean>()

        composeRule.setContent {
            // Read unrelatedTrigger so we can force a normal recomposition later
            val currentTrigger = unrelatedTrigger
            check(currentTrigger >= 0)

            compositionCount++
            observedSkipValues += rememberSkipPlacementAnimation(
                selectedLabel = selectedLabel,
                searchQuery = searchQuery,
            )
        }

        composeRule.waitForIdle()
        assertThat(compositionCount).isEqualTo(1)
        assertThat(observedSkipValues).containsExactly(false)

        // Changing selectedLabel must evaluate skipPlacementAnimation to true and compose only ONCE
        selectedLabel = "Familie"
        composeRule.waitForIdle()
        assertThat(compositionCount).isEqualTo(2)
        assertThat(observedSkipValues).containsExactly(false, true).inOrder()

        // Subsequent normal recomposition with unchanged filter must evaluate to false
        unrelatedTrigger++
        composeRule.waitForIdle()
        assertThat(compositionCount).isEqualTo(3)
        assertThat(observedSkipValues).containsExactly(false, true, false).inOrder()
    }

    @Test
    fun rememberSkipPlacementAnimation_queryChange_skipsOnceWithoutExtraRecomposition() {
        var selectedLabel by mutableStateOf<String?>("Freunde")
        var searchQuery by mutableStateOf("")
        var unrelatedTrigger by mutableIntStateOf(0)

        var compositionCount = 0
        val observedSkipValues = mutableListOf<Boolean>()

        composeRule.setContent {
            val currentTrigger = unrelatedTrigger
            check(currentTrigger >= 0)

            compositionCount++
            observedSkipValues += rememberSkipPlacementAnimation(
                selectedLabel = selectedLabel,
                searchQuery = searchQuery,
            )
        }

        composeRule.waitForIdle()
        assertThat(compositionCount).isEqualTo(1)
        assertThat(observedSkipValues).containsExactly(false)

        // Changing searchQuery must evaluate skipPlacementAnimation to true and compose only ONCE
        searchQuery = "Max"
        composeRule.waitForIdle()
        assertThat(compositionCount).isEqualTo(2)
        assertThat(observedSkipValues).containsExactly(false, true).inOrder()

        // Subsequent normal recomposition with unchanged query must evaluate to false
        unrelatedTrigger++
        composeRule.waitForIdle()
        assertThat(compositionCount).isEqualTo(3)
        assertThat(observedSkipValues).containsExactly(false, true, false).inOrder()
    }

    @Test
    fun birthdayList_filterAndQueryChanges_updateDisplayedContactsCleanly() {
        var contacts by mutableStateOf(SampleData.sampleContacts)
        var selectedLabel by mutableStateOf<String?>(null)
        var searchQuery by mutableStateOf("")

        composeRule.setContent {
            BirthdayBuddyTheme {
                BirthdayList(
                    contacts = contacts,
                    newlyAddedIdeaId = null,
                    hasContactPermission = true,
                    listState = rememberLazyListState(),
                    selectedLabel = selectedLabel,
                    searchQuery = searchQuery,
                    actions = HomeActions.previewDefaults(),
                )
            }
        }

        composeRule.onNodeWithText("Max Mustermann").assertIsDisplayed()
        composeRule.onNodeWithText("Erika Mustermann").assertIsDisplayed()

        selectedLabel = "Familie"
        contacts = listOf(SampleData.contact2, SampleData.contact3)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Max Mustermann").assertDoesNotExist()
        composeRule.onNodeWithText("Erika Mustermann").assertIsDisplayed()
        composeRule.onNodeWithText("Lukas Kind").assertIsDisplayed()

        searchQuery = "Lukas"
        contacts = listOf(SampleData.contact3)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Erika Mustermann").assertDoesNotExist()
        composeRule.onNodeWithText("Lukas Kind").assertIsDisplayed()
    }
}
