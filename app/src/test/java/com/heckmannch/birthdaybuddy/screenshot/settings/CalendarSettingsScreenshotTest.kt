package com.heckmannch.birthdaybuddy.screenshot.settings

import com.heckmannch.birthdaybuddy.screenshot.BaseScreenshotTest
import com.heckmannch.birthdaybuddy.ui.screens.settings.calendar.CalendarSettingsScreenContent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests for [CalendarSettingsScreenContent].
 *
 * Covers:
 * - Synced calendar with custom calendar colors (Light + Dark mode)
 * - Calendar sync disabled
 * - No calendar permission state
 * - Tablet layout
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CalendarSettingsScreenshotTest : BaseScreenshotTest() {

    private val customBirthdayColor = 0xFFE91E63.toInt()
    private val customAnniversaryColor = 0xFF9C27B0.toInt()
    private val customNameDayColor = 0xFF2196F3.toInt()

    // --- Synced calendar with custom colors ---

    @Test
    fun calendarSettings_synced_customColors_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            CalendarSettingsScreenContent(
                calendarSyncEnabled = true,
                hasPermission = true,
                otherEventsEnabled = true,
                birthdayColor = customBirthdayColor,
                anniversaryColor = customAnniversaryColor,
                nameDayColor = customNameDayColor,
                showBackButton = true,
            )
        }
        captureScreenshot()
    }

    @Test
    fun calendarSettings_synced_customColors_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            CalendarSettingsScreenContent(
                calendarSyncEnabled = true,
                hasPermission = true,
                otherEventsEnabled = true,
                birthdayColor = customBirthdayColor,
                anniversaryColor = customAnniversaryColor,
                nameDayColor = customNameDayColor,
                showBackButton = true,
            )
        }
        captureScreenshot()
    }

    // --- Calendar sync disabled ---

    @Test
    fun calendarSettings_disabled_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            CalendarSettingsScreenContent(
                calendarSyncEnabled = false,
                hasPermission = true,
                otherEventsEnabled = false,
                birthdayColor = customBirthdayColor,
                anniversaryColor = customAnniversaryColor,
                nameDayColor = customNameDayColor,
                showBackButton = true,
            )
        }
        captureScreenshot()
    }

    // --- No permission state ---

    @Test
    fun calendarSettings_noPermission_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            CalendarSettingsScreenContent(
                calendarSyncEnabled = false,
                hasPermission = false,
                otherEventsEnabled = false,
                birthdayColor = customBirthdayColor,
                anniversaryColor = customAnniversaryColor,
                nameDayColor = customNameDayColor,
                showBackButton = true,
            )
        }
        captureScreenshot()
    }

    // --- Tablet layout ---

    @Test
    fun calendarSettings_tablet_synced_customColors_lightMode() {
        setScreenshotContent(windowSizeClass = tabletSize, darkTheme = false) {
            CalendarSettingsScreenContent(
                calendarSyncEnabled = true,
                hasPermission = true,
                otherEventsEnabled = true,
                birthdayColor = customBirthdayColor,
                anniversaryColor = customAnniversaryColor,
                nameDayColor = customNameDayColor,
                showBackButton = false,
            )
        }
        captureScreenshot()
    }
}
