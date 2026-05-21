package com.heckmannch.birthdaybuddy.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.heckmannch.birthdaybuddy.ui.model.GiftIdea
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.BirthdayItem
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import org.junit.Rule
import org.junit.Test

class BirthdayItemInteractionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun expandingItem_allowsAddingAndTypingGiftIdeas() {
        val sampleContact = SampleData.contact1.copy(giftIdeas = emptyList())

        val actions = HomeActions(
            onSearchQueryChange = {},
            onLabelSelected = {},
            onClearSearch = {},
            onNavigateToSettings = {},
            onAddContact = {},
            onRequestPermission = {},
            onAddGiftIdea = { _ -> },
            onToggleGiftIdea = { _, _, _ -> },
            onUpdateGiftIdeaText = { _, _, _ -> },
            onDeleteGiftIdea = { _, _ -> },
            onUpdateBirthday = { _, _ -> },
            onOpenContact = { _, _ -> },
            onDial = {},
            onSendSms = {},
            onWhatsApp = {},
            onSignal = {},
            onRefresh = {},
        )

        composeTestRule.setContent {
            var contactState by remember { mutableStateOf(sampleContact) }

            BirthdayBuddyTheme {
                BirthdayItem(
                    contact = contactState,
                    isExpanded = true,
                    newlyAddedIdeaId = null,
                    onExpand = {},
                    actions = actions.copy(
                        onAddGiftIdea = { _ ->
                            contactState = contactState.copy(giftIdeas = listOf(GiftIdea(text = "")))
                        },
                        onUpdateGiftIdeaText = { _, ideaId, text ->
                            contactState = contactState.copy(
                                giftIdeas = contactState.giftIdeas.map {
                                    if (it.id == ideaId) it.copy(text = text) else it
                                }
                            )
                        }
                    )
                )
            }
        }

        // Warte bis UI bereit ist
        composeTestRule.waitForIdle()

        // 0. Klappe den Geschenkideen-Bereich auf
        composeTestRule.onNodeWithTag("gift_ideas_toggle").performClick()
        composeTestRule.waitForIdle()

        // 1. Klicke auf den "Eintrag hinzufügen" Button
        composeTestRule.onNodeWithTag("add_gift_idea_button").performClick()
        
        // Warte auf Recomposition
        composeTestRule.waitForIdle()

        // 2. Prüfe ob das Textfeld erscheint und tippe etwas ein
        composeTestRule.onNodeWithTag("gift_text_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gift_text_field").performTextInput("Socken")
        
        // 3. Verifiziere dass der Text im Feld steht
        composeTestRule.onNodeWithText("Socken").assertExists()
    }
}
