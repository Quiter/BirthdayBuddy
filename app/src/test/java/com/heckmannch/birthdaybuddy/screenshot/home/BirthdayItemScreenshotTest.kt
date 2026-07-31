package com.heckmannch.birthdaybuddy.screenshot.home

import com.heckmannch.birthdaybuddy.screenshot.BaseScreenshotTest
import com.heckmannch.birthdaybuddy.ui.model.BirthdayTier
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.BirthdayItem
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Screenshot tests for [BirthdayItem].
 *
 * Covers all visual variants:
 * - REGULAR tier (collapsed, Light + Dark)
 * - MILESTONE_GOLD tier (today's birthday with golden border)
 * - MILESTONE_SILVER tier
 * - CHILD tier (colorful gradient border)
 * - Expanded state (showing gift ideas)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BirthdayItemScreenshotTest : BaseScreenshotTest() {

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

    // --- REGULAR Tier ---

    @Test
    fun birthdayItem_regular_collapsed_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            BirthdayItem(
                contact = SampleData.contact1.copy(
                    birthdayTier = BirthdayTier.REGULAR,
                    isToday = false,
                    daysUntilNext = 14,
                ),
                isExpanded = false,
                newlyAddedIdeaId = null,
                onExpand = {},
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    @Test
    fun birthdayItem_regular_collapsed_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            BirthdayItem(
                contact = SampleData.contact1.copy(
                    birthdayTier = BirthdayTier.REGULAR,
                    isToday = false,
                    daysUntilNext = 14,
                ),
                isExpanded = false,
                newlyAddedIdeaId = null,
                onExpand = {},
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    // --- MILESTONE_GOLD Tier (today's birthday) ---

    @Test
    fun birthdayItem_milestoneGold_today_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            BirthdayItem(
                contact = SampleData.contact1.copy(
                    birthdayTier = BirthdayTier.MILESTONE_GOLD,
                    isToday = true,
                    daysUntilNext = 0,
                    nextAge = 30,
                    birthday = LocalDate.now(),
                ),
                isExpanded = false,
                newlyAddedIdeaId = null,
                onExpand = {},
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    @Test
    fun birthdayItem_milestoneGold_today_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            BirthdayItem(
                contact = SampleData.contact1.copy(
                    birthdayTier = BirthdayTier.MILESTONE_GOLD,
                    isToday = true,
                    daysUntilNext = 0,
                    nextAge = 30,
                    birthday = LocalDate.now(),
                ),
                isExpanded = false,
                newlyAddedIdeaId = null,
                onExpand = {},
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    // --- MILESTONE_SILVER Tier ---

    @Test
    fun birthdayItem_milestoneSilver_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            BirthdayItem(
                contact = SampleData.contact2.copy(
                    birthdayTier = BirthdayTier.MILESTONE_SILVER,
                    isToday = true,
                    daysUntilNext = 0,
                    nextAge = 25,
                ),
                isExpanded = false,
                newlyAddedIdeaId = null,
                onExpand = {},
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    // --- CHILD Tier ---

    @Test
    fun birthdayItem_child_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            BirthdayItem(
                contact = SampleData.contact3.copy(
                    birthdayTier = BirthdayTier.CHILD,
                    isToday = true,
                    daysUntilNext = 0,
                    nextAge = 5,
                ),
                isExpanded = false,
                newlyAddedIdeaId = null,
                onExpand = {},
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    @Test
    fun birthdayItem_child_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            BirthdayItem(
                contact = SampleData.contact3.copy(
                    birthdayTier = BirthdayTier.CHILD,
                    isToday = true,
                    daysUntilNext = 0,
                    nextAge = 5,
                ),
                isExpanded = false,
                newlyAddedIdeaId = null,
                onExpand = {},
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    // --- Expanded state ---

    @Test
    fun birthdayItem_expanded_withGiftIdeas_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            BirthdayItem(
                contact = SampleData.contact3, // contact3 has 2 gift ideas
                isExpanded = true,
                newlyAddedIdeaId = null,
                onExpand = {},
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    @Test
    fun birthdayItem_expanded_withGiftIdeas_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            BirthdayItem(
                contact = SampleData.contact3,
                isExpanded = true,
                newlyAddedIdeaId = null,
                onExpand = {},
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }

    // --- Selected (Tablet detail pane) ---

    @Test
    fun birthdayItem_selected_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            BirthdayItem(
                contact = SampleData.contact1,
                isExpanded = false,
                isSelected = true,
                newlyAddedIdeaId = null,
                onExpand = {},
                actions = noOpActions(),
            )
        }
        captureScreenshot()
    }
}
