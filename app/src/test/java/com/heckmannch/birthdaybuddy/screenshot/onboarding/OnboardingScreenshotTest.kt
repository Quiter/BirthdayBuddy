package com.heckmannch.birthdaybuddy.screenshot.onboarding

import com.heckmannch.birthdaybuddy.screenshot.BaseScreenshotTest
import com.heckmannch.birthdaybuddy.ui.model.OnboardingUiState
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.OnboardingScreenContent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests for [OnboardingScreenContent].
 *
 * Covers:
 * - Page 0 (Welcome) in Light and Dark mode
 * - Page 1 (Permissions) in Light and Dark mode
 * - Page 2 (Settings) in Light and Dark mode
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnboardingScreenshotTest : BaseScreenshotTest() {

    // --- Page 0: Welcome ---

    @Test
    fun onboarding_page0_welcome_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            OnboardingScreenContent(
                uiState = OnboardingUiState(currentPage = 0),
            )
        }
        captureScreenshot()
    }

    @Test
    fun onboarding_page0_welcome_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            OnboardingScreenContent(
                uiState = OnboardingUiState(currentPage = 0),
            )
        }
        captureScreenshot()
    }

    // --- Page 1: Permissions (Contacts) ---

    @Test
    fun onboarding_page1_permissions_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            OnboardingScreenContent(
                uiState = OnboardingUiState(
                    currentPage = 1,
                    hasContactPermission = false,
                ),
            )
        }
        captureScreenshot()
    }

    @Test
    fun onboarding_page1_permissions_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            OnboardingScreenContent(
                uiState = OnboardingUiState(
                    currentPage = 1,
                    hasContactPermission = false,
                ),
            )
        }
        captureScreenshot()
    }

    // --- Page 2: Settings (Notifications) ---

    @Test
    fun onboarding_page2_settings_lightMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = false) {
            OnboardingScreenContent(
                uiState = OnboardingUiState(
                    currentPage = 2,
                    hasNotificationPermission = false,
                    isPersistentNotificationEnabled = false,
                ),
            )
        }
        captureScreenshot()
    }

    @Test
    fun onboarding_page2_settings_darkMode() {
        setScreenshotContent(windowSizeClass = phoneSize, darkTheme = true) {
            OnboardingScreenContent(
                uiState = OnboardingUiState(
                    currentPage = 2,
                    hasNotificationPermission = false,
                    isPersistentNotificationEnabled = false,
                ),
            )
        }
        captureScreenshot()
    }
}
