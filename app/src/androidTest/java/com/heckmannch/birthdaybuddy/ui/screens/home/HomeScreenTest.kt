package com.heckmannch.birthdaybuddy.ui.screens.home

import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.window.core.layout.WindowSizeClass
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowAdaptiveInfo
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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
            CompositionLocalProvider(LocalWindowSizeClass provides WindowSizeClass(360, 640)) {
                BirthdayBuddyTheme {
                    HomeScreen(
                        uiState = fakeState,
                        onIntent = { /* no-op */ },
                        scrollToTopEvent = MutableSharedFlow(),
                        onNavigateToSettings = {}
                    )
                }
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
            CompositionLocalProvider(LocalWindowSizeClass provides WindowSizeClass(360, 640)) {
                BirthdayBuddyTheme {
                    HomeScreen(
                        uiState = fakeState,
                        onIntent = { /* no-op */ },
                        scrollToTopEvent = MutableSharedFlow(),
                        onNavigateToSettings = {}
                    )
                }
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
            CompositionLocalProvider(LocalWindowSizeClass provides WindowSizeClass(360, 640)) {
                BirthdayBuddyTheme {
                    HomeScreen(
                        uiState = fakeState,
                        onIntent = { /* no-op */ },
                        scrollToTopEvent = MutableSharedFlow(),
                        onNavigateToSettings = {}
                    )
                }
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

        val sizeClass = WindowSizeClass(840, 640)
        val adaptiveInfo = WindowAdaptiveInfo(sizeClass, Posture())

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalWindowSizeClass provides sizeClass,
                LocalWindowAdaptiveInfo provides adaptiveInfo
            ) {
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
                        onNavigateToSettings = {}
                    )
                }
            }
        }

        // Zu Beginn sollten beide Kontakte (Alice und Bob) angezeigt werden
        composeTestRule.onNodeWithText("Alice Becker").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob Clausen").assertIsDisplayed()

        // Filter-Chip "Familie" anklicken
        composeTestRule.onNode(hasText("Familie") and hasAnyAncestor(hasTestTag("birthday_list"))).performClick()

        // Überprüfen, dass nur Alice (mit Label "Familie") angezeigt wird und Bob ausgeblendet wird
        composeTestRule.onNodeWithText("Alice Becker").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob Clausen").assertDoesNotExist()
    }
}
