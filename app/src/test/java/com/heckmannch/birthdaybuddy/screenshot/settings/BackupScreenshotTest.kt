package com.heckmannch.birthdaybuddy.screenshot.settings

import com.heckmannch.birthdaybuddy.screenshot.BaseScreenshotTest
import com.heckmannch.birthdaybuddy.ui.model.BackupMessage
import com.heckmannch.birthdaybuddy.ui.screens.settings.backup.components.BackupScreenContent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests for [BackupScreenContent].
 *
 * Covers:
 * - Normal/idle state (Light + Dark mode)
 * - Success message on export (ExportSuccess in Light + Dark mode)
 * - Loading state
 * - Tablet layout
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackupScreenshotTest : BaseScreenshotTest() {

    // --- Normal / Idle state ---

    @Test
    fun backup_idle_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            BackupScreenContent(
                isLoading = false,
                message = null,
                showBackButton = true,
            )
        }
        captureScreenshot()
    }

    @Test
    fun backup_idle_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            BackupScreenContent(
                isLoading = false,
                message = null,
                showBackButton = true,
            )
        }
        captureScreenshot()
    }

    // --- With Success Message (ExportSuccess) ---

    @Test
    fun backup_exportSuccess_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            BackupScreenContent(
                isLoading = false,
                message = BackupMessage.ExportSuccess,
                showBackButton = true,
            )
        }
        captureScreenshot()
    }

    @Test
    fun backup_exportSuccess_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            BackupScreenContent(
                isLoading = false,
                message = BackupMessage.ExportSuccess,
                showBackButton = true,
            )
        }
        captureScreenshot()
    }

    // --- Loading State ---

    @Test
    fun backup_loading_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            BackupScreenContent(
                isLoading = true,
                message = null,
                showBackButton = true,
            )
        }
        captureScreenshot()
    }

    // --- Tablet Layout ---

    @Test
    fun backup_tablet_idle_lightMode() {
        setScreenshotContent(windowSizeClass = tabletSize, darkTheme = false) {
            BackupScreenContent(
                isLoading = false,
                message = null,
                showBackButton = false,
            )
        }
        captureScreenshot()
    }
}
