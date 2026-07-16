package com.heckmannch.birthdaybuddy.ui.navigation

import androidx.window.core.layout.WindowSizeClass
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeScreen
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeViewModel
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.OnboardingScreen
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.OnboardingViewModel
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.about.AboutScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.about.PrivacyPolicyScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.backup.BackupScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.backup.BackupViewModel
import com.heckmannch.birthdaybuddy.ui.screens.settings.calendar.CalendarSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.calendar.CalendarViewModel
import com.heckmannch.birthdaybuddy.ui.screens.settings.labels.LabelSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.labels.LabelViewModel
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.NotificationSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.NotificationViewModel
import com.heckmannch.birthdaybuddy.ui.screens.settings.otherevents.OtherEventsSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.sync.SyncSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.theme.ThemeSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.theme.ThemeViewModel

/**
 * Zentrale Navigations-Komponente der App.
 *
 * Verwaltet den NavDisplay (Navigation 3) mit allen Routen, Screen-zu-Screen-Transitions
 * und ViewModel-Verknüpfungen. Die Route-Definitionen selbst befinden sich in NavRoutes.kt.
 *
 * @param backStack Der gemeinsame Back-Stack, der von der Activity gehalten wird.
 * @param homeViewModel Activity-weites ViewModel für den Home-Screen.
 * @param onboardingViewModel Activity-weites ViewModel für den Onboarding-Flow.
 * @param windowSizeClass Aktuelle Fensterbreiten-Klasse für adaptive Layouts.
 */
@Composable
fun AppNavHost(
    backStack: MutableList<NavKey>,
    homeViewModel: HomeViewModel,
    onboardingViewModel: OnboardingViewModel,
    windowSizeClass: WindowSizeClass,
) {
    val onboardingCompleted by onboardingViewModel.onboardingCompleted.collectAsStateWithLifecycle()

    // Warten bis der Status geladen wurde, um Flackern zu vermeiden
    if (onboardingCompleted == null) return

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        transitionSpec = {
            val enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
            val exit = slideOutHorizontally(
                targetOffsetX = { -it / 4 },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
            enter togetherWith exit
        },
        popTransitionSpec = {
            val enter = slideInHorizontally(
                initialOffsetX = { -it / 4 },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
            val exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
            (enter togetherWith exit).apply {
                targetContentZIndex = -1f
            }
        },
        // Override default predictive back gesture scale/fade animation to use the same
        // horizontal slide transition with parallax as standard back (pop) navigation.
        predictivePopTransitionSpec = { _ ->
            val enter = slideInHorizontally(
                initialOffsetX = { -it / 4 },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
            val exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
            (enter togetherWith exit).apply {
                targetContentZIndex = -1f
            }
        },
        entryProvider = { key ->
            when (key) {
                is Onboarding -> NavEntry(key) {
                    OnboardingScreen(
                        viewModel = onboardingViewModel,
                        windowSizeClass = windowSizeClass
                    ) {
                        backStack.clear()
                        backStack.add(Home)
                    }
                }

                is Home -> NavEntry(key) {
                    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
                    HomeScreen(
                        uiState = uiState,
                        onIntent = homeViewModel::onIntent,
                        scrollToTopEvent = homeViewModel.scrollToTopEvent,
                        windowSizeClass = windowSizeClass,
                        onNavigateToSettings = {
                            backStack.add(Settings)
                        }
                    )
                }

                is Settings -> NavEntry(key) {
                    SettingsScreen(
                        windowSizeClass = windowSizeClass,
                        homeViewModel = homeViewModel,
                        onNavigateToLabels = { backStack.add(LabelSettings) },
                        onNavigateToNotifications = { backStack.add(NotificationSettings) },
                        onNavigateToCalendar = { backStack.add(CalendarSettings) },
                        onNavigateToBackup = { backStack.add(BackupSettings) },
                        onNavigateToTheme = { backStack.add(ThemeSettings) },
                        onNavigateToSync = { backStack.add(SyncSettings) },
                        onNavigateToAbout = { backStack.add(About) },
                        onNavigateToOtherEvents = { backStack.add(OtherEventsSettings) },
                    ) {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                }

                is LabelSettings -> NavEntry(key) {
                    val labelViewModel: LabelViewModel = hiltViewModel()
                    LabelSettingsScreen(
                        windowSizeClass = windowSizeClass,
                        viewModel = labelViewModel
                    ) {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                }

                is NotificationSettings -> NavEntry(key) {
                    val notificationViewModel: NotificationViewModel = hiltViewModel()
                    NotificationSettingsScreen(
                        windowSizeClass = windowSizeClass,
                        viewModel = notificationViewModel
                    ) {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                }

                is OtherEventsSettings -> NavEntry(key) {
                    val notificationViewModel: NotificationViewModel = hiltViewModel()
                    OtherEventsSettingsScreen(
                        windowSizeClass = windowSizeClass,
                        viewModel = notificationViewModel
                    ) {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                }

                is CalendarSettings -> NavEntry(key) {
                    val calendarViewModel: CalendarViewModel = hiltViewModel()
                    CalendarSettingsScreen(
                        windowSizeClass = windowSizeClass,
                        viewModel = calendarViewModel
                    ) {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                }

                is BackupSettings -> NavEntry(key) {
                    val backupViewModel: BackupViewModel = hiltViewModel()
                    BackupScreen(
                        windowSizeClass = windowSizeClass,
                        viewModel = backupViewModel
                    ) {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                }

                is ThemeSettings -> NavEntry(key) {
                    val themeViewModel: ThemeViewModel = hiltViewModel()
                    ThemeSettingsScreen(
                        windowSizeClass = windowSizeClass,
                        viewModel = themeViewModel
                    ) {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                }

                is SyncSettings -> NavEntry(key) {
                    SyncSettingsScreen(
                        windowSizeClass = windowSizeClass,
                        viewModel = homeViewModel
                    ) {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                }

                is About -> NavEntry(key) {
                    AboutScreen(
                        windowSizeClass = windowSizeClass,
                        onNavigateBack = {
                            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        },
                        onNavigateToPrivacyPolicy = {
                            backStack.add(PrivacyPolicy)
                        },
                    )
                }

                is PrivacyPolicy -> NavEntry(key) {
                    PrivacyPolicyScreen(windowSizeClass = windowSizeClass) {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                }

                else -> throw IllegalArgumentException("Unknown key: $key")
            }
        }
    )
}
