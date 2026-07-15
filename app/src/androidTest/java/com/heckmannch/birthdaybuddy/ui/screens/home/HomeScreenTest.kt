package com.heckmannch.birthdaybuddy.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.heckmannch.birthdaybuddy.ui.components.AppWidthSizeClass
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose-UI-Smoke-Tests für [HomeScreen].
 *
 * Kein Hilt notwendig: Die neue Signatur akzeptiert nur noch [HomeUiState] und
 * einen Intent-Handler-Lambda, sodass der Screen vollständig isoliert getestet
 * werden kann.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Smoke-Test: HomeScreen mit leerem Kontakt-State rendert ohne Crash.
     */
    @Test
    fun homeScreen_withEmptyContacts_rendersWithoutCrash() {
        val fakeState = HomeUiState(contacts = emptyList())

        composeTestRule.setContent {
            BirthdayBuddyTheme {
                HomeScreen(
                    uiState = fakeState,
                    onIntent = { /* no-op */ },
                    scrollToTopEvent = MutableSharedFlow(),
                    windowWidthSizeClass = AppWidthSizeClass.COMPACT,
                    onNavigateToSettings = {}
                )
            }
        }

        composeTestRule.onRoot().assertIsDisplayed()
    }

    /**
     * Smoke-Test: HomeScreen mit Beispiel-Kontakten rendert ohne Crash.
     */
    @Test
    fun homeScreen_withSampleContacts_rendersWithoutCrash() {
        val fakeState = SampleData.homeUiState

        composeTestRule.setContent {
            BirthdayBuddyTheme {
                HomeScreen(
                    uiState = fakeState,
                    onIntent = { /* no-op */ },
                    scrollToTopEvent = MutableSharedFlow(),
                    windowWidthSizeClass = AppWidthSizeClass.COMPACT,
                    onNavigateToSettings = {}
                )
            }
        }

        composeTestRule.onRoot().assertIsDisplayed()
    }

    /**
     * Smoke-Test: Überprüft, dass im Lade-State (contacts = null) kein Crash auftritt.
     */
    @Test
    fun homeScreen_withNullContacts_rendersWithoutCrash() {
        val fakeState = HomeUiState(contacts = null)

        composeTestRule.setContent {
            BirthdayBuddyTheme {
                HomeScreen(
                    uiState = fakeState,
                    onIntent = { /* no-op */ },
                    scrollToTopEvent = MutableSharedFlow(),
                    windowWidthSizeClass = AppWidthSizeClass.COMPACT,
                    onNavigateToSettings = {}
                )
            }
        }

        composeTestRule.onRoot().assertIsDisplayed()
    }

    /**
     * Testet, ob das Filtern nach Labels im Tablet-Layout (Expanded) korrekt funktioniert
     * und die Kontaktliste aktualisiert wird, anstatt mit veralteten State-Werten zu arbeiten.
     */
    @Test
    fun homeScreen_tabletLayout_labelSelectionFiltersList() {
        var uiState by mutableStateOf(
            HomeUiState(
                contacts = listOf(
                    ContactUiModel(
                        id = "1",
                        contactId = "1",
                        lookupKey = "key1",
                        fullName = "Alice Becker",
                        dateText = "12. Jan.",
                        monthName = "Januar",
                        imageUri = null,
                        phoneNumber = null,
                        initials = "AB",
                        nextAge = 30,
                        daysUntilNext = 10L,
                        isToday = false,
                        hasWhatsApp = false,
                        hasSignal = false,
                        labels = listOf("Familie"),
                        giftIdeas = emptyList()
                    ),
                    ContactUiModel(
                        id = "2",
                        contactId = "2",
                        lookupKey = "key2",
                        fullName = "Bob Clausen",
                        dateText = "24. Feb.",
                        monthName = "Februar",
                        imageUri = null,
                        phoneNumber = null,
                        initials = "BC",
                        nextAge = 40,
                        daysUntilNext = 50L,
                        isToday = false,
                        hasWhatsApp = false,
                        hasSignal = false,
                        labels = listOf("Freunde"),
                        giftIdeas = emptyList()
                    )
                ),
                availableLabels = listOf("Familie", "Freunde"),
                selectedLabel = null
            )
        )

        composeTestRule.setContent {
            BirthdayBuddyTheme {
                HomeScreen(
                    uiState = uiState,
                    onIntent = { intent ->
                        if (intent is HomeIntent.LabelSelected) {
                            val newLabel = if (uiState.selectedLabel == intent.label) null else intent.label
                            val allContacts = listOf(
                                ContactUiModel(
                                    id = "1",
                                    contactId = "1",
                                    lookupKey = "key1",
                                    fullName = "Alice Becker",
                                    dateText = "12. Jan.",
                                    monthName = "Januar",
                                    imageUri = null,
                                    phoneNumber = null,
                                    initials = "AB",
                                    nextAge = 30,
                                    daysUntilNext = 10L,
                                    isToday = false,
                                    hasWhatsApp = false,
                                    hasSignal = false,
                                    labels = listOf("Familie"),
                                    giftIdeas = emptyList()
                                ),
                                ContactUiModel(
                                    id = "2",
                                    contactId = "2",
                                    lookupKey = "key2",
                                    fullName = "Bob Clausen",
                                    dateText = "24. Feb.",
                                    monthName = "Februar",
                                    imageUri = null,
                                    phoneNumber = null,
                                    initials = "BC",
                                    nextAge = 40,
                                    daysUntilNext = 50L,
                                    isToday = false,
                                    hasWhatsApp = false,
                                    hasSignal = false,
                                    labels = listOf("Freunde"),
                                    giftIdeas = emptyList()
                                )
                            )
                            val filtered = if (newLabel == null) allContacts else allContacts.filter { it.labels.contains(newLabel) }
                            uiState = uiState.copy(
                                selectedLabel = newLabel,
                                contacts = filtered
                            )
                        }
                    },
                    scrollToTopEvent = MutableSharedFlow(),
                    windowWidthSizeClass = AppWidthSizeClass.EXPANDED,
                    onNavigateToSettings = {}
                )
            }
        }

        // Zu Beginn sollten beide Kontakte (Alice und Bob) angezeigt werden
        composeTestRule.onNodeWithText("Alice Becker").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob Clausen").assertIsDisplayed()

        // Filter-Chip "Familie" anklicken
        composeTestRule.onNodeWithText("Familie").performClick()

        // Überprüfen, dass nur Alice (mit Label "Familie") angezeigt wird und Bob ausgeblendet wird
        composeTestRule.onNodeWithText("Alice Becker").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob Clausen").assertDoesNotExist()
    }
}
