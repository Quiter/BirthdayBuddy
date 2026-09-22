package com.heckmannch.birthdaybuddy.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.FastScrollbar
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class FastScrollbarTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    private val scrollbarDesc: String
        get() = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.scrollbar_content_description)

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
        composeTestRule.onNodeWithContentDescription(scrollbarDesc, substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun fastScrollbar_appears_on_scroll_and_shows_bubble_on_drag() {
        composeTestRule.setContent {
            BirthdayBuddyTheme {
                val listState = rememberLazyListState()
                val contacts = remember { List(50) { SampleData.sampleContacts.first() } }
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().testTag("lazy_column")
                    ) {
                        itemsIndexed(contacts) { index, _ ->
                            Text(
                                "Item $index",
                                modifier = Modifier.size(100.dp)
                            )
                        }
                    }
                    FastScrollbar(
                        listState = listState,
                        contacts = contacts,
                        getLabel = { "TestLabel" },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                    )
                }
            }
        }

        // Scrollen triggern (langsamer, mehrstufiger Drag, damit die Liste scrollt während der Finger gedrückt ist)
        composeTestRule.onNodeWithTag("lazy_column").performTouchInput {
            down(center)
            moveBy(Offset(0f, -100f))
            moveBy(Offset(0f, -100f))
            moveBy(Offset(0f, -100f))
            moveBy(Offset(0f, -100f))
            up()
        }

        // Warten bis die Sichtbarkeit durch Scrollen triggert
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithContentDescription(scrollbarDesc, substring = true).assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        // Drag the scrollbar thumb
        composeTestRule.onNodeWithContentDescription(scrollbarDesc, substring = true)
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, 50f))
            }

        // The bubble should appear while dragging
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("TestLabel", useUnmergedTree = true).assertIsDisplayed()

        // Release the drag
        composeTestRule.onNodeWithContentDescription(scrollbarDesc, substring = true)
            .performTouchInput {
                up()
            }
    }
}
