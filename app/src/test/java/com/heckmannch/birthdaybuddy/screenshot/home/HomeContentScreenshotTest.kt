package com.heckmannch.birthdaybuddy.screenshot.home

import com.heckmannch.birthdaybuddy.screenshot.BaseScreenshotTest
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeContent
import com.heckmannch.birthdaybuddy.ui.screens.home.rememberHomeState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests for [HomeContent].
 *
 * Covers:
 * - Phone layout with sample contacts (Light + Dark)
 * - Phone layout with empty contacts list
 * - Phone layout with loading state (contacts = null / shimmer)
 * - Tablet layout with contacts (adaptive layout with sidebar)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeContentScreenshotTest : BaseScreenshotTest() {

    private fun noOpActions() = HomeActions(
        onSearchQueryChange = {},
        onLabelSelected = {},
        onClearSearch = {},
        onNavigateToSettings = {},
        onAddContact = {},
        onRequestPermission = {},
        onAddGiftIdea = {},
        onToggleGiftIdea = { _, _, _ -> },
        onUpdateGiftIdeaText = { _, _, _ -> },
        onDeleteGiftIdea = { _, _ -> },
        onUpdateBirthday = { _, _ -> },
        onOpenContact = { _, _ -> },
        onDial = {},
        onSendSms = {},
        onOpenMessengerApp = { _, _ -> },
        onRefresh = {},
    )

    // --- Light Mode ---

    @Test
    fun homeContent_phone_withContacts_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            HomeContent(
                uiState = SampleData.homeUiState,
                homeState = rememberHomeState(),
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    @Test
    fun homeContent_phone_withContacts_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            HomeContent(
                uiState = SampleData.homeUiState,
                homeState = rememberHomeState(),
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    @Test
    fun homeContent_phone_emptyState_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            HomeContent(
                uiState = HomeUiState(
                    contacts = emptyList(),
                    hasContactPermission = true,
                ),
                homeState = rememberHomeState(),
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    @Test
    fun homeContent_phone_emptyState_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            HomeContent(
                uiState = HomeUiState(
                    contacts = emptyList(),
                    hasContactPermission = true,
                ),
                homeState = rememberHomeState(),
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    @Test
    fun homeContent_phone_loadingState_shimmer() {
        // contacts = null → triggers shimmer skeleton loader
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            HomeContent(
                uiState = HomeUiState(contacts = null),
                homeState = rememberHomeState(),
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    @Test
    fun homeContent_phone_noPermission_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            HomeContent(
                uiState = HomeUiState(
                    contacts = emptyList(),
                    hasContactPermission = false,
                ),
                homeState = rememberHomeState(),
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    @Test
    fun homeContent_tablet_withContacts_lightMode() {
        // Tablet triggers the PermanentNavigationDrawer sidebar in HomeContent
        setScreenshotContent(windowSizeClass = tabletSize, darkTheme = false) {
            HomeContent(
                uiState = SampleData.homeUiState,
                homeState = rememberHomeState(),
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    @Test
    fun homeContent_tablet_withContacts_darkMode() {
        setScreenshotContent(windowSizeClass = tabletSize, darkTheme = true) {
            HomeContent(
                uiState = SampleData.homeUiState,
                homeState = rememberHomeState(),
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    @Test
    fun homeContent_phone_withLabels_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            HomeContent(
                uiState = SampleData.homeUiState.copy(
                    availableLabels = listOf("Familie", "Freunde", "Arbeit"),
                    selectedLabel = "Familie",
                ),
                homeState = rememberHomeState(),
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }
}
