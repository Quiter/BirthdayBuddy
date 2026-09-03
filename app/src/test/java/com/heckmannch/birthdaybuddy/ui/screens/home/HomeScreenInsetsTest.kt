package com.heckmannch.birthdaybuddy.ui.screens.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowSizeClass
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.HomeListDetailDisplay
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Verifies that all elements on the home screen (BirthdayDetailPane, BirthdayQuotePlaceholder,
 * BirthdayList items, FastScrollbar) properly respect insets and never disappear
 * behind [com.heckmannch.birthdaybuddy.ui.screens.home.components.topbar.HomeTopBar].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w840dp-h640dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenInsetsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val tabletWindowSize = WindowSizeClass(840, 640)

    private fun noOpActions() = HomeActions.previewDefaults()

    @Test
    fun tablet_detailPane_doesNotDisappearBehindHomeTopBar() {
        val uiState = SampleData.homeUiState

        composeRule.setContent {
            BirthdayBuddyTheme {
                CompositionLocalProvider(
                    LocalWindowSizeClass provides tabletWindowSize,
                ) {
                    HomeContent(
                        uiState = uiState,
                        homeState = rememberHomeState(),
                        actions = noOpActions(),
                    )
                }
            }
        }

        composeRule.waitForIdle()

        // Click on contact to open detail pane on tablet
        composeRule.onNodeWithText("Max Mustermann").performClick()
        composeRule.waitForIdle()

        val topBarBottom = composeRule.onNodeWithTag("home_top_bar").getBoundsInRoot().bottom
        val detailPaneBounds = composeRule.onNodeWithTag("birthday_detail_pane").getBoundsInRoot()
        val closeButtonBounds = composeRule.onNodeWithTag("detail_close_button").getBoundsInRoot()

        // Detail Pane Card must start at or below the bottom of HomeTopBar
        assertThat(detailPaneBounds.top).isAtLeast(topBarBottom)
        // Close button inside detail pane must also be fully visible below HomeTopBar
        assertThat(closeButtonBounds.top).isAtLeast(topBarBottom)
    }

    @Test
    fun tablet_detailPlaceholder_doesNotDisappearBehindHomeTopBar() {
        val uiState = SampleData.homeUiState

        composeRule.setContent {
            BirthdayBuddyTheme {
                CompositionLocalProvider(
                    LocalWindowSizeClass provides tabletWindowSize,
                ) {
                    HomeContent(
                        uiState = uiState,
                        homeState = rememberHomeState(),
                        actions = noOpActions(),
                    )
                }
            }
        }

        composeRule.waitForIdle()

        val topBarBottom = composeRule.onNodeWithTag("home_top_bar").getBoundsInRoot().bottom
        val placeholderBounds = composeRule.onNodeWithTag("birthday_quote_placeholder").getBoundsInRoot()

        // Placeholder card must start at or below the bottom of HomeTopBar
        assertThat(placeholderBounds.top).isAtLeast(topBarBottom)
    }

    @Test
    fun tablet_listAndScrollbar_doNotDisappearBehindHomeTopBar() {
        val testContacts = List(40) { index ->
            SampleData.sampleContacts[index % SampleData.sampleContacts.size].copy(
                id = "contact_$index",
                fullName = "Contact $index",
            )
        }
        val uiState = SampleData.homeUiState.copy(
            contacts = testContacts,
            availableLabels = listOf("Familie", "Freunde"),
        )

        composeRule.setContent {
            BirthdayBuddyTheme {
                CompositionLocalProvider(
                    LocalWindowSizeClass provides tabletWindowSize,
                ) {
                    HomeContent(
                        uiState = uiState,
                        homeState = rememberHomeState(),
                        actions = noOpActions(),
                    )
                }
            }
        }

        composeRule.waitForIdle()

        val topBarBottom = composeRule.onNodeWithTag("home_top_bar").getBoundsInRoot().bottom
        val scrollbarBounds = composeRule.onNodeWithTag("fast_scrollbar").getBoundsInRoot()
        val firstItemBounds = composeRule.onNodeWithText("Contact 0").getBoundsInRoot()

        // Scrollbar track must start at or below HomeTopBar
        assertThat(scrollbarBounds.top).isAtLeast(topBarBottom)
        // First contact item must start at or below HomeTopBar
        assertThat(firstItemBounds.top).isAtLeast(topBarBottom)
    }

    @Test
    fun homeListDetailDisplay_detailPane_respectsExplicitContentPadding() {
        val topPadding = 100.dp
        val bottomPadding = 48.dp
        val uiState = SampleData.homeUiState

        composeRule.setContent {
            BirthdayBuddyTheme {
                CompositionLocalProvider(
                    LocalWindowSizeClass provides tabletWindowSize,
                ) {
                    HomeListDetailDisplay(
                        uiState = uiState,
                        homeState = rememberHomeState(),
                        actions = noOpActions(),
                        showLabelFilter = true,
                        contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule.waitForIdle()

        // Click on contact to open detail pane
        composeRule.onNodeWithText("Max Mustermann").performClick()
        composeRule.waitForIdle()

        val detailPaneBounds = composeRule.onNodeWithTag("birthday_detail_pane").getBoundsInRoot()
        assertThat(detailPaneBounds.top).isAtLeast(topPadding)
    }
}
