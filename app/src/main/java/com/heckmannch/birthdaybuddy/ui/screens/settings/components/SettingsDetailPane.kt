package com.heckmannch.birthdaybuddy.ui.screens.settings.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.window.core.layout.WindowSizeClass
import com.heckmannch.birthdaybuddy.ui.components.layout.isWidthCompact
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsNavKey
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsTab
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.about.AboutScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.about.LicensesScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.about.PrivacyPolicyScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.backup.BackupScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.calendar.CalendarSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.notifications.NotificationSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.storage.StorageSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.sync.SyncSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.theme.ThemeSettingsScreen
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * Content composable for rendering the detail screen corresponding to a selected [SettingsTab].
 */
@Composable
internal fun SettingsDetailPane(
    tab: SettingsTab,
    windowSizeClass: WindowSizeClass,
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (tab) {
            SettingsTab.NOTIFICATIONS -> {
                NotificationSettingsScreen(
                    viewModel = hiltViewModel(),
                    showBackButton = windowSizeClass.isWidthCompact,
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }

            SettingsTab.CALENDAR -> {
                CalendarSettingsScreen(
                    viewModel = hiltViewModel(),
                    showBackButton = windowSizeClass.isWidthCompact,
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }

            SettingsTab.BACKUP -> {
                BackupScreen(
                    viewModel = hiltViewModel(),
                    showBackButton = windowSizeClass.isWidthCompact,
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }

            SettingsTab.THEME -> {
                ThemeSettingsScreen(
                    viewModel = hiltViewModel(),
                    showBackButton = windowSizeClass.isWidthCompact,
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }

            SettingsTab.SYNC -> {
                SyncSettingsScreen(
                    viewModel = hiltViewModel(),
                    showBackButton = windowSizeClass.isWidthCompact,
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }

            SettingsTab.ABOUT -> {
                AboutScreen(
                    showBackButton = windowSizeClass.isWidthCompact,
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onNavigateToPrivacyPolicy = {
                        backStack.add(SettingsNavKey.SettingsDetail(SettingsTab.PRIVACY_POLICY))
                    },
                    onNavigateToLicenses = {
                        backStack.add(SettingsNavKey.SettingsDetail(SettingsTab.LICENSES))
                    },
                )
            }

            SettingsTab.PRIVACY_POLICY -> {
                PrivacyPolicyScreen(
                    showBackButton = true,
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }

            SettingsTab.STORAGE -> {
                StorageSettingsScreen(
                    showBackButton = windowSizeClass.isWidthCompact,
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }

            SettingsTab.LICENSES -> {
                LicensesScreen(
                    showBackButton = true,
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun SettingsDetailPanePreview() {
    BirthdayBuddyTheme {
        val backStack = rememberNavBackStack(SettingsNavKey.SettingsDetail(SettingsTab.ABOUT))
        SettingsDetailPane(
            tab = SettingsTab.ABOUT,
            windowSizeClass = WindowSizeClass(360, 640),
            backStack = backStack,
        )
    }
}
