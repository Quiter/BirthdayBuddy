package com.heckmannch.birthdaybuddy.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.FastScrollbar
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import org.junit.Rule
import org.junit.Test

class FastScrollbarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun fastScrollbar_initially_invisible() {
        composeTestRule.setContent {
            BirthdayBuddyTheme {
                val listState = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(state = listState) {
                        items(SampleData.sampleContacts) { Text(it.fullName) }
                    }
                    FastScrollbar(
                        listState = listState,
                        contacts = SampleData.sampleContacts,
                        getLabel = { it.monthName }
                    )
                }
            }
        }

        // Der Scrollbar sollte am Anfang nicht existieren oder alpha 0 haben
        // Wir prüfen hier auf das Fehlen der Content Description
        composeTestRule.onNodeWithContentDescription("Scrollbar", substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun fastScrollbar_appears_on_scroll() {
        composeTestRule.setContent {
            BirthdayBuddyTheme {
                val listState = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        // Wir brauchen genug Items, um über den Threshold (10) zu kommen
                        items(List(50) { "Item $it" }) {
                            Text(
                                it,
                                modifier = Modifier.size(100.dp)
                            )
                        }
                    }
                    FastScrollbar(
                        listState = listState,
                        contacts = List(15) { SampleData.sampleContacts.first() }, // Explizit 15 Items
                        getLabel = { "Label" }
                    )
                }
            }
        }

        // Scrollen triggern
        composeTestRule.onNodeWithText("Item 0").performTouchInput { swipeUp() }

        // Warten bis die Sichtbarkeit durch Scrollen triggert
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Scrollbar", substring = true)
            .assertIsDisplayed()
    }
}
