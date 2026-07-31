package com.heckmannch.birthdaybuddy.screenshot.home

import androidx.compose.foundation.lazy.rememberLazyListState
import com.heckmannch.birthdaybuddy.screenshot.BaseScreenshotTest
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.BirthdayList
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests for [BirthdayList].
 *
 * Covers:
 * - Contact list with sample data (Light + Dark)
 * - Empty state (permission granted + permission denied)
 * - Loading/shimmer state (contacts = null)
 * - Filter label bar visible
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BirthdayListScreenshotTest : BaseScreenshotTest() {

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

    @Test
    fun birthdayList_withContacts_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            BirthdayList(
                contacts = SampleData.sampleContacts,
                newlyAddedIdeaId = null,
                hasContactPermission = true,
                listState = rememberLazyListState(),
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    @Test
    fun birthdayList_withContacts_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            BirthdayList(
                contacts = SampleData.sampleContacts,
                newlyAddedIdeaId = null,
                hasContactPermission = true,
                listState = rememberLazyListState(),
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    @Test
    fun birthdayList_emptyState_withPermission_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            BirthdayList(
                contacts = emptyList(),
                newlyAddedIdeaId = null,
                hasContactPermission = true,
                listState = rememberLazyListState(),
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    @Test
    fun birthdayList_emptyState_withPermission_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            BirthdayList(
                contacts = emptyList(),
                newlyAddedIdeaId = null,
                hasContactPermission = true,
                listState = rememberLazyListState(),
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    @Test
    fun birthdayList_emptyState_noPermission_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            BirthdayList(
                contacts = emptyList(),
                newlyAddedIdeaId = null,
                hasContactPermission = false,
                listState = rememberLazyListState(),
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    @Test
    fun birthdayList_loadingState_shimmer() {
        // contacts = null triggers the shimmer skeleton loader
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            BirthdayList(
                contacts = null,
                newlyAddedIdeaId = null,
                hasContactPermission = true,
                listState = rememberLazyListState(),
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    @Test
    fun birthdayList_withLabels_selected_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            BirthdayList(
                contacts = SampleData.sampleContacts,
                newlyAddedIdeaId = null,
                hasContactPermission = true,
                listState = rememberLazyListState(),
                actions = noOpActions(),
                availableLabels = listOf("Familie", "Freunde", "Arbeit"),
                selectedLabel = "Familie",
            )
        }
        captureScreenshot()
    }

    @Test
    fun birthdayList_withContacts_tallPhone_lightMode() {
        // Taller screen to show more contact items
        setScreenshotContent(windowSizeClass = phoneTallSize, darkTheme = false) {
            BirthdayList(
                contacts = SampleData.sampleContacts,
                newlyAddedIdeaId = null,
                hasContactPermission = true,
                listState = rememberLazyListState(),
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }
}
