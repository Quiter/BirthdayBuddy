package com.heckmannch.birthdaybuddy.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.GiftIdea
import com.heckmannch.birthdaybuddy.ui.screens.home.components.BirthdayItem
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import org.junit.Rule
import org.junit.Test

class BirthdayItemInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun expandingItem_allowsAddingAndTypingGiftIdeas() {
        val sampleContact = ContactUiModel(
            id = "k1",
            contactId = "1",
            lookupKey = "k1",
            fullName = "Test User",
            dateText = "12. Mai",
            monthName = "Mai",
            imageUri = null,
            phoneNumber = "+49123456",
            initials = "T",
            nextAge = 30,
            daysUntilNext = 5,
            isToday = false,
            labels = emptyList(),
            giftIdeas = emptyList(),
        )

        composeTestRule.setContent {
            // Wir nutzen ein mutableStateOf, um das reaktive Verhalten der App zu simulieren
            var contactState by remember { mutableStateOf(sampleContact) }

            BirthdayBuddyTheme {
                BirthdayItem(
                    contact = contactState,
                    isExpanded = true,
                    onExpand = {},
                    onUpdateGiftIdeas = { _, ideasJson ->
                        // Simuliere DB-Update: Wandle JSON zurück in Liste und aktualisiere State
                        contactState = contactState.copy(giftIdeas = GiftIdea.fromString(ideasJson))
                    },
                    onOpenContact = { _: String, _: String -> },
                )
            }
        }

        // 1. Klicke auf den "Eintrag hinzufügen" Button
        composeTestRule.onNodeWithTag("add_gift_idea_button").performClick()
        
        // Warte bis die UI stabil ist (State Update -> Recomposition)
        composeTestRule.waitForIdle()

        // 2. Prüfe ob das Textfeld erscheint und tippe etwas ein
        // Da das Textfeld nun Teil des giftIdeas-Liste ist
        composeTestRule.onNodeWithTag("gift_text_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gift_text_field").performTextInput("Socken")
        
        // 3. Verifiziere dass der Text im Feld steht
        composeTestRule.onNodeWithText("Socken").assertExists()
    }
}
