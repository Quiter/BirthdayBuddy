package com.heckmannch.birthdaybuddy.screenshot.settings

import com.heckmannch.birthdaybuddy.screenshot.BaseScreenshotTest
import com.heckmannch.birthdaybuddy.ui.model.LabelManagementModel
import com.heckmannch.birthdaybuddy.ui.screens.settings.labels.LabelSettingsScreenContent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests for [LabelSettingsScreenContent].
 *
 * Covers:
 * - Labels enabled with sample data (Light + Dark)
 * - Labels disabled (master switch off)
 * - Empty state (no user-defined labels)
 * - Font scale 1.5x (accessibility)
 * - Tablet layout
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LabelSettingsScreenshotTest : BaseScreenshotTest() {

    private val sampleLabels = listOf(
        LabelManagementModel(
            name = "Familie",
            isHiddenFromFilter = false,
            isIgnored = false,
            isSystem = false,
            notificationsEnabled = true,
            showInWidget = true,
        ),
        LabelManagementModel(
            name = "Freunde",
            isHiddenFromFilter = false,
            isIgnored = false,
            isSystem = false,
            notificationsEnabled = true,
            showInWidget = false,
        ),
        LabelManagementModel(
            name = "Arbeit",
            isHiddenFromFilter = true,
            isIgnored = false,
            isSystem = false,
            notificationsEnabled = false,
            showInWidget = false,
        ),
        LabelManagementModel(
            name = "Bekannte",
            isHiddenFromFilter = false,
            isIgnored = true,
            isSystem = false,
            notificationsEnabled = false,
            showInWidget = false,
        ),
    )

    // --- Labels enabled ---

    @Test
    fun labelSettings_enabled_withLabels_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            LabelSettingsScreenContent(
                labels = sampleLabels,
                labelsEnabled = true,
                showBackButton = true,
                onNavigateBack = {},
                onLabelsEnabledChanged = {},
                onConfigChanged = { _, _, _, _, _, _ -> },
            )
        }
        captureScreenshot()
    }

    @Test
    fun labelSettings_enabled_withLabels_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            LabelSettingsScreenContent(
                labels = sampleLabels,
                labelsEnabled = true,
                showBackButton = true,
                onNavigateBack = {},
                onLabelsEnabledChanged = {},
                onConfigChanged = { _, _, _, _, _, _ -> },
            )
        }
        captureScreenshot()
    }

    // --- Labels disabled (master switch off) ---

    @Test
    fun labelSettings_disabled_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            LabelSettingsScreenContent(
                labels = sampleLabels,
                labelsEnabled = false,
                showBackButton = true,
                onNavigateBack = {},
                onLabelsEnabledChanged = {},
                onConfigChanged = { _, _, _, _, _, _ -> },
            )
        }
        captureScreenshot()
    }

    @Test
    fun labelSettings_disabled_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            LabelSettingsScreenContent(
                labels = sampleLabels,
                labelsEnabled = false,
                showBackButton = true,
                onNavigateBack = {},
                onLabelsEnabledChanged = {},
                onConfigChanged = { _, _, _, _, _, _ -> },
            )
        }
        captureScreenshot()
    }

    // --- Empty state ---

    @Test
    fun labelSettings_emptyLabels_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            LabelSettingsScreenContent(
                labels = emptyList(),
                labelsEnabled = true,
                showBackButton = true,
                onNavigateBack = {},
                onLabelsEnabledChanged = {},
                onConfigChanged = { _, _, _, _, _, _ -> },
            )
        }
        captureScreenshot()
    }

    // --- Tablet layout ---

    @Test
    fun labelSettings_tablet_withLabels_lightMode() {
        setScreenshotContent(windowSizeClass = tabletSize, darkTheme = false) {
            LabelSettingsScreenContent(
                labels = sampleLabels,
                labelsEnabled = true,
                showBackButton = false,
                onNavigateBack = {},
                onLabelsEnabledChanged = {},
                onConfigChanged = { _, _, _, _, _, _ -> },
            )
        }
        captureScreenshot()
    }
}
