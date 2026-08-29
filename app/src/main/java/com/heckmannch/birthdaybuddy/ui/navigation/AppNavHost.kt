package com.heckmannch.birthdaybuddy.ui.navigation

import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.heckmannch.birthdaybuddy.ui.components.ContactSyncEffect
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeIntent
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeScreen
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeViewModel
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.OnboardingScreen
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.OnboardingViewModel
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsTab
import com.heckmannch.birthdaybuddy.util.IntentExtras
import com.heckmannch.birthdaybuddy.util.safeGetAndRemoveBooleanExtra

/**
 * Zentrale Navigations-Komponente der App.
 *
 * Verwaltet den NavDisplay (Navigation 3) mit allen Routen, Screen-zu-Screen-Transitions
 * und ViewModel-Verknüpfungen. Die Route-Definitionen selbst befinden sich in NavRoutes.kt.
 *
 * ViewModels wie HomeViewModel und OnboardingViewModel werden via hiltViewModel() direkt
 * in ihren jeweiligen NavEntry-Blöcken instanziiert und über rememberViewModelStoreNavEntryDecorator()
 * an den Lifecycle des Eintrags gebunden.
 *
 * @param backStack Der gemeinsame Back-Stack, der von der Activity gehalten wird.
 * @param intent Der aktuelle Intent der Activity für Deep-Links und Intent-Aktionen.
 */
@Composable
fun AppNavHost(
    backStack: MutableList<NavKey>,
    intent: Intent? = null,
) {
    // Navigations-Intents behandeln (z.B. Benachrichtigungseinstellungen direkt öffnen)
    LaunchedEffect(intent) {
        if (intent.safeGetAndRemoveBooleanExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS)) {
            if (!backStack.contains(NotificationSettings)) {
                backStack.add(NotificationSettings)
            }
        }
    }

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
                    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
                    OnboardingScreen(
                        viewModel = onboardingViewModel,
                    ) {
                        backStack.clear()
                        backStack.add(Home)
                    }
                }

                is Home -> NavEntry(key) {
                    val homeViewModel: HomeViewModel = hiltViewModel()
                    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

                    // Inaktivitäts-Check: Filter nach 5 Minuten bei Wiederaufnahme zurücksetzen
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                homeViewModel.onIntent(HomeIntent.AppResumed)
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    // Live-Sync bei Änderungen im System-Adressbuch
                    ContactSyncEffect(onSyncNeeded = { homeViewModel.onIntent(HomeIntent.SyncContacts()) })

                    // Intent-Events für den Home-Screen verarbeiten (z.B. Widget / App Shortcuts)
                    LaunchedEffect(intent) {
                        if (intent.safeGetAndRemoveBooleanExtra(IntentExtras.SCROLL_TO_TOP)) {
                            homeViewModel.onIntent(HomeIntent.TriggerScrollToTop)
                        }
                        if (intent.safeGetAndRemoveBooleanExtra(IntentExtras.OPEN_SEARCH)) {
                            if (backStack.lastOrNull() != Home) {
                                backStack.clear()
                                backStack.add(Home)
                            }
                            homeViewModel.onIntent(HomeIntent.TriggerSearchFocus)
                        }
                        if (intent.safeGetAndRemoveBooleanExtra(IntentExtras.OPEN_ADD_CONTACT)) {
                            homeViewModel.onIntent(HomeIntent.SyncContacts())
                        }
                    }

                    HomeScreen(
                        uiState = uiState,
                        onIntent = homeViewModel::onIntent,
                        scrollToTopEvent = homeViewModel.scrollToTopEvent,
                        onNavigateToSettings = {
                            backStack.add(Settings)
                        }
                    )
                }

                is Settings -> NavEntry(key) {
                    SettingsScreen {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                }

                is NotificationSettings -> NavEntry(key) {
                    SettingsScreen(
                        initialTab = SettingsTab.NOTIFICATIONS,
                        onNavigateBack = {
                            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        }
                    )
                }

                else -> throw IllegalArgumentException("Unknown key: $key")
            }
        }
    )
}
