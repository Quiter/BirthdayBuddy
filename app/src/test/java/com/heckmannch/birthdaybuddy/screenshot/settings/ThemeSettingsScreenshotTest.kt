package com.heckmannch.birthdaybuddy.screenshot.settings

import com.heckmannch.birthdaybuddy.domain.model.ThemeAccent
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode
import com.heckmannch.birthdaybuddy.screenshot.BaseScreenshotTest
import com.heckmannch.birthdaybuddy.ui.screens.settings.theme.ThemeSettingsScreenContent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests for [ThemeSettingsScreenContent].
 *
 * Covers:
 * - System mode
 * - Light mode
 * - Dark mode
 * - AMOLED mode (Dark + AMOLED enabled)
 * - Tablet layout
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThemeSettingsScreenshotTest : BaseScreenshotTest() {

    @Test
    fun themeSettings_systemMode() {
        setScreenshotContent(windowSizeClass = phoneSize, themeMode = ThemeMode.SYSTEM) {
            ThemeSettingsScreenContent(
                themeMode = ThemeMode.SYSTEM,
                themeAmoled = false,
                themeAccent = ThemeAccent.PURPLE,
                showBackButton = true,
            )
        }
        captureScreenshot()
    }

    @Test
    fun themeSettings_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false, themeMode = ThemeMode.LIGHT) {
            ThemeSettingsScreenContent(
                themeMode = ThemeMode.LIGHT,
                themeAmoled = false,
                themeAccent = ThemeAccent.PURPLE,
                showBackButton = true,
            )
        }
        captureScreenshot()
    }

    @Test
    fun themeSettings_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true, themeMode = ThemeMode.DARK) {
            ThemeSettingsScreenContent(
                themeMode = ThemeMode.DARK,
                themeAmoled = false,
                themeAccent = ThemeAccent.PURPLE,
                showBackButton = true,
            )
        }
        captureScreenshot()
    }

    @Test
    fun themeSettings_amoledMode() {
        setScreenshotContent(
            windowSizeClass = phoneSize,
            darkTheme = true,
            themeMode = ThemeMode.DARK,
            themeAmoled = true,
        ) {
            ThemeSettingsScreenContent(
                themeMode = ThemeMode.DARK,
                themeAmoled = true,
                themeAccent = ThemeAccent.PURPLE,
                showBackButton = true,
            )
        }
        captureScreenshot()
    }

    @Test
    fun themeSettings_tablet_lightMode() {
        setScreenshotContent(windowSizeClass = tabletSize, darkTheme = false, themeMode = ThemeMode.LIGHT) {
            ThemeSettingsScreenContent(
                themeMode = ThemeMode.LIGHT,
                themeAmoled = false,
                themeAccent = ThemeAccent.BLUE,
                showBackButton = false,
            )
        }
        captureScreenshot()
    }
}
