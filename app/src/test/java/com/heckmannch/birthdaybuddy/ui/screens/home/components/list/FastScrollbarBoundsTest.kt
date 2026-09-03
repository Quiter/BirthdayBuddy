package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowSizeClass
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.screens.home.rememberHomeState
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Verifies that [FastScrollbar] properly respects [PaddingValues] and constrains its
 * touch track and thumb to the visible [BirthdayList] bounds rather than extending
 * across the entire device screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FastScrollbarBoundsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val testContacts = List(40) { index ->
        SampleData.sampleContacts[index % SampleData.sampleContacts.size].copy(
            id = "contact_$index",
            fullName = "Contact $index",
            monthName = "Month ${(index % 12) + 1}"
        )
    }

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
    fun fastScrollbar_withContentPadding_trackBoundsRespectTopAndBottomPadding() {
        val topPadding = 96.dp
        val bottomPadding = 48.dp
        val containerHeight = 800.dp
        val containerWidth = 400.dp

        composeRule.setContent {
            BirthdayBuddyTheme {
                val listState = rememberLazyListState()
                Box(modifier = Modifier.size(width = containerWidth, height = containerHeight)) {
                    BirthdayList(
                        contacts = testContacts,
                        newlyAddedIdeaId = null,
                        hasContactPermission = true,
                        listState = listState,
                        actions = noOpActions(),
                        contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
                    )

                    FastScrollbar(
                        listState = listState,
                        contacts = testContacts,
                        getLabel = { it.monthName },
                        contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxSize(),
                    )
                }
            }
        }

        val rootBounds = composeRule.onRoot().getBoundsInRoot()
        val trackBounds = composeRule.onNodeWithTag("fast_scrollbar").getBoundsInRoot()

        // The track must start exactly at topPadding (e.g. below TopBar), NOT at 0.dp (screen top)
        assertThat(trackBounds.top).isEqualTo(topPadding)
        // The track must end above bottom insets (rootBounds.bottom - bottomPadding)
        assertThat(trackBounds.bottom).isEqualTo(rootBounds.bottom - bottomPadding)
        assertThat(trackBounds.bottom - trackBounds.top).isEqualTo(rootBounds.bottom - rootBounds.top - topPadding - bottomPadding)
    }

    @Test
    fun fastScrollbar_thumbDoesNotExceedTopPaddingWhenListIsAtTop() {
        val topPadding = 110.dp
        val bottomPadding = 56.dp
        val containerHeight = 800.dp
        val containerWidth = 400.dp

        var listState = androidx.compose.foundation.lazy.LazyListState()

        composeRule.setContent {
            BirthdayBuddyTheme {
                val state = rememberLazyListState()
                listState = state
                Box(modifier = Modifier.size(width = containerWidth, height = containerHeight)) {
                    BirthdayList(
                        contacts = testContacts,
                        newlyAddedIdeaId = null,
                        hasContactPermission = true,
                        listState = state,
                        actions = noOpActions(),
                        contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
                    )

                    FastScrollbar(
                        listState = state,
                        contacts = testContacts,
                        getLabel = { it.monthName },
                        contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxSize(),
                    )
                }
            }
        }

        // Trigger scroll to make scrollbar visible
        composeRule.onNodeWithText("Contact 0", substring = true).performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        // Scroll back to top
        runBlocking {
            listState.scrollToItem(0, 0)
        }
        composeRule.waitForIdle()

        // Verify that the scrollbar thumb is at or below topPadding, never at 0.dp
        val thumbNode = composeRule.onNodeWithContentDescription("Scrollbar")
        val thumbBounds = thumbNode.getBoundsInRoot()

        assertThat(thumbBounds.top).isAtLeast(topPadding)
        assertThat(thumbBounds.top).isLessThan(topPadding + 20.dp)
    }

    @Test
    fun fastScrollbar_withoutContentPadding_defaultsToZeroPadding() {
        composeRule.setContent {
            BirthdayBuddyTheme {
                val listState = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    BirthdayList(
                        contacts = testContacts,
                        newlyAddedIdeaId = null,
                        hasContactPermission = true,
                        listState = listState,
                        actions = noOpActions(),
                    )
                    FastScrollbar(
                        listState = listState,
                        contacts = testContacts,
                        getLabel = { it.monthName },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxSize(),
                    )
                }
            }
        }

        val rootBounds = composeRule.onRoot().getBoundsInRoot()
        val trackBounds = composeRule.onNodeWithTag("fast_scrollbar").getBoundsInRoot()
        assertThat(trackBounds.top).isEqualTo(0.dp)
        assertThat(trackBounds.bottom).isEqualTo(rootBounds.bottom)
    }

    @Test
    fun homeListDetailDisplay_forwardsContentPaddingToFastScrollbar() {
        val topPadding = 120.dp
        val bottomPadding = 64.dp

        val uiState = HomeUiState(
            contacts = testContacts,
            hasContactPermission = true,
            availableLabels = listOf("Family", "Friends"),
            selectedLabel = null,
        )

        composeRule.setContent {
            BirthdayBuddyTheme {
                CompositionLocalProvider(
                    LocalWindowSizeClass provides WindowSizeClass(360, 640),
                ) {
                    val listState = rememberLazyListState()
                    val homeState = rememberHomeState(listState = listState)

                    Box(modifier = Modifier.fillMaxSize()) {
                        HomeListDetailDisplay(
                            uiState = uiState,
                            homeState = homeState,
                            actions = noOpActions(),
                            showLabelFilter = true,
                            contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        val rootBounds = composeRule.onRoot().getBoundsInRoot()
        val scrollbarBounds = composeRule.onNodeWithTag("fast_scrollbar").getBoundsInRoot()

        // Ensures the scrollbar in HomeListDetailDisplay correctly aligns with BirthdayList
        assertThat(scrollbarBounds.top).isEqualTo(topPadding)
        assertThat(scrollbarBounds.bottom).isEqualTo(rootBounds.bottom - bottomPadding)
    }
}
