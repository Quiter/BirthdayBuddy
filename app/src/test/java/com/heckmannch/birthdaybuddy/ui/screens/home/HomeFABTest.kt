package com.heckmannch.birthdaybuddy.ui.screens.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.window.core.layout.WindowSizeClass
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowSizeClass
import com.heckmannch.birthdaybuddy.ui.model.BirthdayTier
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.components.actions.HomeFAB
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w840dp-h640dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeFABTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleContact = ContactUiModel(
        id = "101",
        contactId = "101",
        lookupKey = "lookup_101",
        fullName = "Anna Schmidt",
        dateText = "12. Mai",
        monthName = "Mai",
        imageUri = null,
        phoneNumber = "+49 170 1234567",
        initials = "AS",
        nextAge = 30,
        daysUntilNext = 50,
        isToday = false,
        isFavorite = false,
        hasWhatsApp = false,
        hasSignal = false,
        labels = emptyList(),
        giftIdeas = emptyList(),
        birthday = LocalDate.of(1995, 5, 12),
        birthdayTier = BirthdayTier.REGULAR,
    )

    @Test
    fun homeFAB_showsAddContact_whenNotScrolledAndNoContactSelected() {
        var addClicked = false
        val actions = HomeActions.previewDefaults().copy(
            onAddContact = { addClicked = true }
        )

        composeRule.setContent {
            BirthdayBuddyTheme {
                HomeFAB(
                    showScrollUp = false,
                    selectedContact = null,
                    actions = actions,
                    onScrollToTop = {},
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Add contact").assertIsDisplayed()
        composeRule.onNodeWithTag("home_fab").performClick()

        assertThat(addClicked).isTrue()
    }

    @Test
    fun homeFAB_showsScrollToTop_whenScrolledAndNoContactSelected() {
        var scrollToTopClicked = false

        composeRule.setContent {
            BirthdayBuddyTheme {
                HomeFAB(
                    showScrollUp = true,
                    selectedContact = null,
                    actions = HomeActions.previewDefaults(),
                    onScrollToTop = { scrollToTopClicked = true },
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Scroll to top").assertIsDisplayed()
        composeRule.onNodeWithTag("home_fab").performClick()

        assertThat(scrollToTopClicked).isTrue()
    }

    @Test
    fun homeFAB_showsEditContact_whenContactSelected() {
        var editedContactId: String? = null
        var editedLookupKey: String? = null
        val actions = HomeActions.previewDefaults().copy(
            onEditContact = { id, key ->
                editedContactId = id
                editedLookupKey = key
            }
        )

        composeRule.setContent {
            BirthdayBuddyTheme {
                HomeFAB(
                    showScrollUp = true, // Scroll-Status should be overridden by selected contact
                    selectedContact = sampleContact,
                    actions = actions,
                    onScrollToTop = {},
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Edit contact").assertIsDisplayed()
        composeRule.onNodeWithTag("home_fab").performClick()

        assertThat(editedContactId).isEqualTo("101")
        assertThat(editedLookupKey).isEqualTo("lookup_101")
    }

    @Test
    fun homeContent_integratesHomeFAB_transitionsToEditAndBack() {
        var editedContactId: String? = null
        val actions = HomeActions.previewDefaults().copy(
            onEditContact = { id, _ -> editedContactId = id }
        )
        val uiState = SampleData.homeUiState.copy(
            contacts = listOf(sampleContact)
        )

        lateinit var capturedState: HomeState
        composeRule.setContent {
            BirthdayBuddyTheme {
                CompositionLocalProvider(
                    LocalWindowSizeClass provides WindowSizeClass(840, 640)
                ) {
                    val state = rememberHomeState()
                    capturedState = state
                    HomeContent(
                        uiState = uiState,
                        homeState = state,
                        actions = actions,
                    )
                }
            }
        }

        composeRule.waitForIdle()

        // Initially on list: FAB is "Add contact"
        composeRule.onNodeWithContentDescription("Add contact").assertIsDisplayed()

        // Select contact card to open detail pane
        composeRule.onNodeWithText("Anna Schmidt").performClick()
        composeRule.waitForIdle()

        // FAB should now be "Edit contact"
        composeRule.onNodeWithContentDescription("Edit contact").assertIsDisplayed()
        composeRule.onNodeWithTag("home_fab").performClick()
        assertThat(editedContactId).isEqualTo("101")

        // Close detail pane
        composeRule.onNodeWithTag("detail_close_button").performClick()
        composeRule.waitForIdle()

        // FAB should revert back to "Add contact"
        composeRule.onNodeWithContentDescription("Add contact").assertIsDisplayed()
    }
}
