package com.heckmannch.birthdaybuddy.ui.screens.home

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
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
                    windowWidthSizeClass = WindowWidthSizeClass.Compact,
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
                    windowWidthSizeClass = WindowWidthSizeClass.Compact,
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
                    windowWidthSizeClass = WindowWidthSizeClass.Compact,
                    onNavigateToSettings = {}
                )
            }
        }

        composeTestRule.onRoot().assertIsDisplayed()
    }
}
