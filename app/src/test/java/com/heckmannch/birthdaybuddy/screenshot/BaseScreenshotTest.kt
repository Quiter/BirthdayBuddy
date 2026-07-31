package com.heckmannch.birthdaybuddy.screenshot

import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.window.core.layout.WindowSizeClass
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.RoborazziRule.Options
import com.github.takahirom.roborazzi.captureRoboImage
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowAdaptiveInfo
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowSizeClass
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import org.junit.Rule

/**
 * Abstract base class for Roborazzi screenshot tests.
 *
 * Provides helpers for theme wrapping, window size class overriding,
 * and adaptive layout setup matching the app's existing preview patterns.
 *
 * Usage:
 *   - Extend this class in each screenshot test file.
 *   - Use [phoneSize] for compact-width phone layout (360×640 dp).
 *   - Use [tabletSize] for expanded-width tablet layout (840×640 dp).
 *   - Call [setScreenshotContent] to apply theme + locals before [captureRoboImage].
 */
abstract class BaseScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = Options(
            outputDirectoryPath = "src/test/snapshots/images",
        )
    )

    // --- Window Size Presets ---

    /** Compact phone layout: 360×640 dp */
    val phoneSize = WindowSizeClass(360, 640)

    /** Phone with extra height for scrollable content: 360×1000 dp */
    val phoneTallSize = WindowSizeClass(360, 1000)

    /** Expanded tablet layout: 840×640 dp */
    val tabletSize = WindowSizeClass(840, 640)

    /**
     * Creates a WindowAdaptiveInfo matching a given WindowSizeClass.
     * Uses default [Posture] (no folding features active).
     * This mirrors what [LocalWindowAdaptiveInfo] would provide in production
     * on a non-folding device.
     */
    fun adaptiveInfoFor(windowSizeClass: WindowSizeClass): WindowAdaptiveInfo {
        return WindowAdaptiveInfo(
            windowSizeClass = windowSizeClass,
            windowPosture = Posture(),
        )
    }

    /**
     * Sets the Compose content with theme + window size class overrides.
     * Mirrors the existing @Preview patterns in the app.
     *
     * Uses [ThemeMode.LIGHT] or [ThemeMode.DARK] to force a theme, bypassing the
     * system setting (which is not available in Robolectric tests).
     *
     * @param windowSizeClass The simulated window size. Defaults to [phoneSize].
     * @param darkTheme If true, uses [ThemeMode.DARK]; otherwise [ThemeMode.LIGHT].
     * @param content The composable to render.
     */
    fun setScreenshotContent(
        windowSizeClass: WindowSizeClass = phoneSize,
        darkTheme: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        val adaptiveInfo = adaptiveInfoFor(windowSizeClass)
        val themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT
        composeRule.setContent {
            BirthdayBuddyTheme(themeMode = themeMode) {
                CompositionLocalProvider(
                    LocalWindowSizeClass provides windowSizeClass,
                    LocalWindowAdaptiveInfo provides adaptiveInfo,
                ) {
                    content()
                }
            }
        }
    }

    /**
     * Captures the current Compose content as a screenshot.
     * Must be called after [setScreenshotContent].
     * The file name is automatically derived from the test method name by [RoborazziRule].
     */
    fun captureScreenshot() {
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage()
    }
}
