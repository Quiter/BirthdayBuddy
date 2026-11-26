package com.heckmannch.birthdaybuddy.screenshot.settings

import com.heckmannch.birthdaybuddy.screenshot.BaseScreenshotTest
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsContent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests for [SettingsContent] overview menu.
 *
 * Covers:
 * - Compact mode on phone (Light + Dark mode)
 * - Split-pane mode on tablet/expanded screen (Light + Dark mode)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsOverviewScreenshotTest : BaseScreenshotTest() {

    // --- Compact Mode (Phone) ---

    @Test
    fun settingsOverview_phone_compact_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            SettingsContent(
                initialTab = null,
                onNavigateBack = {},
                labelsEnabled = true,
                otherEventsEnabled = false,
            )
        }
        captureScreenshot()
    }

    @Test
    fun settingsOverview_phone_compact_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            SettingsContent(
                initialTab = null,
                onNavigateBack = {},
                labelsEnabled = true,
                otherEventsEnabled = false,
            )
        }
        captureScreenshot()
    }

    // --- Split-Pane Mode (Tablet / Expanded) ---

    @Test
    fun settingsOverview_tablet_splitPane_lightMode() {
        setScreenshotContent(windowSizeClass = tabletSize, darkTheme = false) {
            SettingsContent(
                initialTab = null,
                onNavigateBack = {},
                labelsEnabled = true,
                otherEventsEnabled = false,
            )
        }
        captureScreenshot()
    }

    @Test
    fun settingsOverview_tablet_splitPane_darkMode() {
        setScreenshotContent(windowSizeClass = tabletSize, darkTheme = true) {
            SettingsContent(
                initialTab = null,
                onNavigateBack = {},
                labelsEnabled = true,
                otherEventsEnabled = false,
            )
        }
        captureScreenshot()
    }
}
