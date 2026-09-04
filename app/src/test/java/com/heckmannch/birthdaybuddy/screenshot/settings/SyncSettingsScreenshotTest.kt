package com.heckmannch.birthdaybuddy.screenshot.settings

import com.heckmannch.birthdaybuddy.screenshot.BaseScreenshotTest
import com.heckmannch.birthdaybuddy.ui.screens.settings.sync.SyncSettingsScreenContent
import com.heckmannch.birthdaybuddy.ui.screens.settings.sync.SyncUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests for [SyncSettingsScreenContent].
 *
 * Covers:
 * - Idle state (Light + Dark mode)
 * - Syncing state (isSyncing = true, Light + Dark mode)
 * - Tablet layout
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SyncSettingsScreenshotTest : BaseScreenshotTest() {

    // --- Idle State ---

    @Test
    fun syncSettings_idle_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            SyncSettingsScreenContent(
                uiState = SyncUiState(isSyncing = false),
                showBackButton = true,
            )
        }
        captureScreenshot()
    }

    @Test
    fun syncSettings_idle_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            SyncSettingsScreenContent(
                uiState = SyncUiState(isSyncing = false),
                showBackButton = true,
            )
        }
        captureScreenshot()
    }

    // --- Syncing State (isSyncing = true) ---

    @Test
    fun syncSettings_syncing_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            SyncSettingsScreenContent(
                uiState = SyncUiState(isSyncing = true),
                showBackButton = true,
            )
        }
        captureScreenshot()
    }

    @Test
    fun syncSettings_syncing_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            SyncSettingsScreenContent(
                uiState = SyncUiState(isSyncing = true),
                showBackButton = true,
            )
        }
        captureScreenshot()
    }

    // --- Tablet Layout ---

    @Test
    fun syncSettings_tablet_idle_lightMode() {
        setScreenshotContent(windowSizeClass = tabletSize, darkTheme = false) {
            SyncSettingsScreenContent(
                uiState = SyncUiState(isSyncing = false),
                showBackButton = false,
            )
        }
        captureScreenshot()
    }
}
