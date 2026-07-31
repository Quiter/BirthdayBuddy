package com.heckmannch.birthdaybuddy.screenshot.settings

import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.screenshot.BaseScreenshotTest
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.NotificationSettingsContent
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.NotificationSettingsState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests for [NotificationSettingsContent].
 *
 * Covers:
 * - Notifications enabled with sample rules (Light + Dark)
 * - Notifications disabled
 * - No system permission (permission-request state)
 * - Empty rules list
 * - Tablet layout
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NotificationSettingsScreenshotTest : BaseScreenshotTest() {

    private val sampleRules = listOf(
        NotificationRule(id = 1, daysBefore = 0, hour = 8, minute = 0),
        NotificationRule(id = 2, daysBefore = 1, hour = 9, minute = 30),
        NotificationRule(id = 3, daysBefore = 7, hour = 10, minute = 0),
    )

    private fun defaultState() = NotificationSettingsState()

    // --- Notifications enabled with rules ---

    @Test
    fun notificationSettings_enabled_withRules_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            NotificationSettingsContent(
                notificationsEnabled = true,
                persistentNotifications = true,
                rules = sampleRules,
                hasSystemPermission = true,
                state = defaultState(),
                showBackButton = true,
                onToggleNotifications = {},
                onTogglePersistent = {},
                onAddRule = { _, _, _ -> },
                onUpdateRule = {},
                onDeleteRule = {},
                onRequestPermission = {},
                onNavigateBack = {},
            )
        }
        captureScreenshot()
    }

    @Test
    fun notificationSettings_enabled_withRules_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            NotificationSettingsContent(
                notificationsEnabled = true,
                persistentNotifications = true,
                rules = sampleRules,
                hasSystemPermission = true,
                state = defaultState(),
                showBackButton = true,
                onToggleNotifications = {},
                onTogglePersistent = {},
                onAddRule = { _, _, _ -> },
                onUpdateRule = {},
                onDeleteRule = {},
                onRequestPermission = {},
                onNavigateBack = {},
            )
        }
        captureScreenshot()
    }

    // --- Notifications disabled ---

    @Test
    fun notificationSettings_disabled_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            NotificationSettingsContent(
                notificationsEnabled = false,
                persistentNotifications = false,
                rules = emptyList(),
                hasSystemPermission = true,
                state = defaultState(),
                showBackButton = true,
                onToggleNotifications = {},
                onTogglePersistent = {},
                onAddRule = { _, _, _ -> },
                onUpdateRule = {},
                onDeleteRule = {},
                onRequestPermission = {},
                onNavigateBack = {},
            )
        }
        captureScreenshot()
    }

    @Test
    fun notificationSettings_disabled_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            NotificationSettingsContent(
                notificationsEnabled = false,
                persistentNotifications = false,
                rules = emptyList(),
                hasSystemPermission = true,
                state = defaultState(),
                showBackButton = true,
                onToggleNotifications = {},
                onTogglePersistent = {},
                onAddRule = { _, _, _ -> },
                onUpdateRule = {},
                onDeleteRule = {},
                onRequestPermission = {},
                onNavigateBack = {},
            )
        }
        captureScreenshot()
    }

    // --- No system permission (permission request state) ---

    @Test
    fun notificationSettings_noPermission_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            NotificationSettingsContent(
                notificationsEnabled = false,
                persistentNotifications = false,
                rules = emptyList(),
                hasSystemPermission = false,
                state = defaultState(),
                showBackButton = true,
                onToggleNotifications = {},
                onTogglePersistent = {},
                onAddRule = { _, _, _ -> },
                onUpdateRule = {},
                onDeleteRule = {},
                onRequestPermission = {},
                onNavigateBack = {},
            )
        }
        captureScreenshot()
    }

    @Test
    fun notificationSettings_noPermission_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            NotificationSettingsContent(
                notificationsEnabled = false,
                persistentNotifications = false,
                rules = emptyList(),
                hasSystemPermission = false,
                state = defaultState(),
                showBackButton = true,
                onToggleNotifications = {},
                onTogglePersistent = {},
                onAddRule = { _, _, _ -> },
                onUpdateRule = {},
                onDeleteRule = {},
                onRequestPermission = {},
                onNavigateBack = {},
            )
        }
        captureScreenshot()
    }

    // --- Enabled with empty rules list ---

    @Test
    fun notificationSettings_enabled_emptyRules_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            NotificationSettingsContent(
                notificationsEnabled = true,
                persistentNotifications = false,
                rules = emptyList(),
                hasSystemPermission = true,
                state = defaultState(),
                showBackButton = true,
                onToggleNotifications = {},
                onTogglePersistent = {},
                onAddRule = { _, _, _ -> },
                onUpdateRule = {},
                onDeleteRule = {},
                onRequestPermission = {},
                onNavigateBack = {},
            )
        }
        captureScreenshot()
    }

    // --- Tablet layout ---

    @Test
    fun notificationSettings_tablet_withRules_lightMode() {
        setScreenshotContent(windowSizeClass = tabletSize, darkTheme = false) {
            NotificationSettingsContent(
                notificationsEnabled = true,
                persistentNotifications = true,
                rules = sampleRules,
                hasSystemPermission = true,
                state = defaultState(),
                showBackButton = false,
                onToggleNotifications = {},
                onTogglePersistent = {},
                onAddRule = { _, _, _ -> },
                onUpdateRule = {},
                onDeleteRule = {},
                onRequestPermission = {},
                onNavigateBack = {},
            )
        }
        captureScreenshot()
    }
}
