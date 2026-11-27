package com.heckmannch.birthdaybuddy.ui.components.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.window.core.layout.WindowSizeClass
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode
import com.heckmannch.birthdaybuddy.ui.components.layout.LocalWindowSizeClass
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsContent
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.theme.ThemeSettingsScreenContent
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusLargeIncreased
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusSmall
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Unit and Compose UI tests for the Morphe-inspired Settings UI primitives,
 * connected shapes, press-width distribution, search state, and deep settings search.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun connectedShapes_cardInColumn_resolvesSingleFirstMiddleLast() {
        val solo = ConnectedShapes.Card.inColumn(index = 0, count = 1)
        assertThat(solo).isEqualTo(CornerRadii.uniform(CornerRadiusLargeIncreased))

        val first = ConnectedShapes.Card.inColumn(index = 0, count = 3)
        assertThat(first.topStart).isEqualTo(CornerRadiusLargeIncreased)
        assertThat(first.topEnd).isEqualTo(CornerRadiusLargeIncreased)
        assertThat(first.bottomStart).isEqualTo(CornerRadiusExtraSmall)
        assertThat(first.bottomEnd).isEqualTo(CornerRadiusExtraSmall)

        val middle = ConnectedShapes.Card.inColumn(index = 1, count = 3)
        assertThat(middle).isEqualTo(CornerRadii.uniform(CornerRadiusExtraSmall))

        val last = ConnectedShapes.Card.inColumn(index = 2, count = 3)
        assertThat(last.topStart).isEqualTo(CornerRadiusExtraSmall)
        assertThat(last.topEnd).isEqualTo(CornerRadiusExtraSmall)
        assertThat(last.bottomStart).isEqualTo(CornerRadiusLargeIncreased)
        assertThat(last.bottomEnd).isEqualTo(CornerRadiusLargeIncreased)
    }

    @Test
    fun connectedShapes_pillInRow_resolvesFirstMiddleLast() {
        val first = ConnectedShapes.Pill.inRow(index = 0, count = 3)
        assertThat(first.topStart).isEqualTo(CornerRadiusExtraLarge)
        assertThat(first.bottomStart).isEqualTo(CornerRadiusExtraLarge)
        assertThat(first.topEnd).isEqualTo(CornerRadiusSmall)
        assertThat(first.bottomEnd).isEqualTo(CornerRadiusSmall)

        val middle = ConnectedShapes.Pill.inRow(index = 1, count = 3)
        assertThat(middle).isEqualTo(CornerRadii.uniform(CornerRadiusSmall))

        val last = ConnectedShapes.Pill.inRow(index = 2, count = 3)
        assertThat(last.topStart).isEqualTo(CornerRadiusSmall)
        assertThat(last.bottomStart).isEqualTo(CornerRadiusSmall)
        assertThat(last.topEnd).isEqualTo(CornerRadiusExtraLarge)
        assertThat(last.bottomEnd).isEqualTo(CornerRadiusExtraLarge)
    }

    @Test
    fun widenPressed_preservesTotalWidthAndExpandsPressedItem() {
        val idle = widenPressed(availableWidth = 300, pressProgresses = floatArrayOf(0f, 0f, 0f))
        assertThat(idle.toList()).containsExactly(100, 100, 100).inOrder()

        val middlePressed = widenPressed(
            availableWidth = 300,
            pressProgresses = floatArrayOf(0f, 1f, 0f),
            ratio = 0.2f,
        )
        assertThat(middlePressed.sum()).isEqualTo(300)
        assertThat(middlePressed[1]).isEqualTo(120)
        assertThat(middlePressed[0]).isEqualTo(90)
        assertThat(middlePressed[2]).isEqualTo(90)

        val firstPressed = widenPressed(
            availableWidth = 300,
            pressProgresses = floatArrayOf(1f, 0f, 0f),
            ratio = 0.2f,
        )
        assertThat(firstPressed.sum()).isEqualTo(300)
        assertThat(firstPressed[0]).isEqualTo(120)
        assertThat(firstPressed[1]).isEqualTo(80)
        assertThat(firstPressed[2]).isEqualTo(100)
    }

    @Test
    fun settingsSearch_matchesCaseInsensitiveAndTracksShownCount() {
        val search = SettingsSearch()
        assertThat(search.isActive).isFalse()
        assertThat(search.matches("Theme", "Colors")).isTrue()

        search.query = "amoled"
        assertThat(search.isActive).isTrue()
        assertThat(search.matches("AMOLED Black", "Pure black")).isTrue()
        assertThat(search.matches("Backup", "Export JSON")).isFalse()

        val tokenA = Any()
        val tokenB = Any()
        search.report(tokenA, true)
        search.report(tokenB, false)
        assertThat(search.shown).isEqualTo(1)

        search.remove(tokenA)
        assertThat(search.shown).isEqualTo(0)
    }

    @Test
    fun settingsSearchState_transitionsCleanly() {
        val state = SettingsSearchState()
        assertThat(state.visible).isFalse()
        assertThat(state.query).isEmpty()

        state.open()
        state.query = "theme"
        assertThat(state.visible).isTrue()
        assertThat(state.query).isEqualTo("theme")

        state.clear()
        assertThat(state.visible).isTrue()
        assertThat(state.query).isEmpty()

        state.query = "sync"
        state.collapse()
        assertThat(state.visible).isFalse()
        assertThat(state.query).isEmpty()
    }

    @Test
    fun settingsContent_directToggleAndDeepSubpageSearchWork() {
        var labelsEnabled = true
        composeRule.setContent {
            BirthdayBuddyTheme {
                CompositionLocalProvider(LocalWindowSizeClass provides WindowSizeClass(360, 800)) {
                    SettingsContent(
                        labelsEnabled = labelsEnabled,
                        onLabelsEnabledChanged = { labelsEnabled = it },
                    )
                }
            }
        }

        // Toggle labels directly on the overview screen
        composeRule.onNodeWithText("Manage labels").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertThat(labelsEnabled).isFalse()

        // Open header search pill and search for a subpage setting ("AMOLED")
        composeRule.onNodeWithTag("open_search_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("search_field").performTextInput("AMOLED")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("AMOLED Black", substring = true).assertIsDisplayed()

        // Clear search and test unmatched query shows empty state
        composeRule.onNodeWithTag("clear_search_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("search_field").performTextInput("zzzyyyxxx_nonexistent")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("No matching settings found").assertIsDisplayed()
    }

    @Test
    fun themeSettingsContent_selectsThemeModeAndTogglesAmoled() {
        var selectedMode = ThemeMode.DARK
        var amoled = false

        composeRule.setContent {
            BirthdayBuddyTheme {
                ThemeSettingsScreenContent(
                    themeMode = selectedMode,
                    themeAmoled = amoled,
                    onThemeModeChange = { selectedMode = it },
                    onThemeAmoledChange = { amoled = it },
                )
            }
        }

        composeRule.onNodeWithText("AMOLED Black").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertThat(amoled).isTrue()

        composeRule.onNodeWithText("Light").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertThat(selectedMode).isEqualTo(ThemeMode.LIGHT)
    }
}
