package com.heckmannch.birthdaybuddy.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import com.heckmannch.birthdaybuddy.ui.components.ContactSyncEffect
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeIntent
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeScreen
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeViewModel
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.OnboardingScreen
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.OnboardingViewModel
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsTab

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
 * @param modifier Der [Modifier], der auf das NavDisplay angewendet werden soll.
 * @param action Die aktuelle [AppAction] für Deep-Links und Steuerungs-Aktionen.
 * @param onActionHandled Callback zur Quittierung und Bereinigung verarbeiteter Aktionen.
 */
@Composable
fun AppNavHost(
    backStack: MutableList<NavKey>,
    modifier: Modifier = Modifier,
    action: AppAction? = null,
    onActionHandled: () -> Unit = {},
) {
    val onNavigateBack: () -> Unit = {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    // Globale Navigations-Aktionen behandeln (z.B. Benachrichtigungseinstellungen direkt öffnen oder zu Home wechseln)
    LaunchedEffect(action) {
        when (action) {
            null -> return@LaunchedEffect
            AppAction.NavigateToNotifications -> {
                val targetKey = Settings(initialTab = SettingsTab.NOTIFICATIONS)
                if (!backStack.contains(targetKey)) {
                    backStack.add(targetKey)
                }
                onActionHandled()
            }
            AppAction.OpenSearch,
            AppAction.ScrollToTop,
            AppAction.OpenAddContact,
            is AppAction.OpenBirthdayPicker -> {
                if (backStack.lastOrNull() != Home) {
                    backStack.clear()
                    backStack.add(Home)
                }
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = onNavigateBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        transitionSpec = forwardTransitionSpec(),
        popTransitionSpec = popTransitionSpec(),
        // Override default predictive back gesture scale/fade animation to use the same
        // horizontal slide transition with parallax as standard back (pop) navigation.
        predictivePopTransitionSpec = { popTransitionSpec().invoke(this) },
        entryProvider = entryProvider {
            entry<Onboarding> {
                val onboardingViewModel: OnboardingViewModel = hiltViewModel()
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                ) {
                    backStack.clear()
                    backStack.add(Home)
                }
            }

            entry<Home> {
                val homeViewModel: HomeViewModel = hiltViewModel()
                val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

                // Inaktivitäts-Check: Filter nach 5 Minuten bei Wiederaufnahme zurücksetzen
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    homeViewModel.onIntent(HomeIntent.AppResumed)
                }

                // Live-Sync bei Änderungen im System-Adressbuch
                ContactSyncEffect(onSyncNeeded = { homeViewModel.onIntent(HomeIntent.SyncContacts()) })

                // Aktionen für den Home-Screen verarbeiten (z.B. Widget / App Shortcuts / AppFunctions)
                LaunchedEffect(action) {
                    when (action) {
                        null -> return@LaunchedEffect
                        AppAction.ScrollToTop -> {
                            homeViewModel.onIntent(HomeIntent.TriggerScrollToTop)
                            onActionHandled()
                        }
                        AppAction.OpenSearch -> {
                            homeViewModel.onIntent(HomeIntent.TriggerSearchFocus)
                            onActionHandled()
                        }
                        AppAction.OpenAddContact -> {
                            homeViewModel.onIntent(HomeIntent.SyncContacts())
                            onActionHandled()
                        }
                        is AppAction.OpenBirthdayPicker -> {
                            homeViewModel.onIntent(
                                HomeIntent.OpenBirthdayPicker(
                                    contactLookupKey = action.contactLookupKey,
                                    year = action.year,
                                    month = action.month,
                                    day = action.day,
                                )
                            )
                            onActionHandled()
                        }
                        AppAction.NavigateToNotifications -> {
                            // Bereits auf globaler Navigationsebene behandelt
                        }
                    }
                }

                HomeScreen(
                    uiState = uiState,
                    onIntent = homeViewModel::onIntent,
                    scrollToTopEvent = homeViewModel.scrollToTopEvent,
                    onNavigateToSettings = {
                        backStack.add(Settings())
                    }
                )
            }

            entry<Settings> { key ->
                SettingsScreen(
                    initialTab = key.initialTab,
                    onNavigateBack = onNavigateBack
                )
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Screen Transitions & Animation Constants
// ---------------------------------------------------------------------------

private const val TRANSITION_DURATION_MS = 400
private const val FADE_DURATION_MS = 300
private const val PARALLAX_FACTOR = 4
private const val POP_TARGET_Z_INDEX = -1f

/**
 * Standard Vorwärts-Transition: Der neue Screen schiebt sich von rechts herein
 * und überdeckt den alten Screen, während dieser leicht nach links verschoben (Parallax)
 * und ausgeblendet wird.
 */
private fun forwardTransitionSpec(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform =
    {
        val enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(FADE_DURATION_MS))
        val exit = slideOutHorizontally(
            targetOffsetX = { -it / PARALLAX_FACTOR },
            animationSpec = tween(TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(FADE_DURATION_MS))
        enter togetherWith exit
    }

/**
 * Standard Pop- & Predictive-Back-Transition: Der aktuelle Screen gleitet nach rechts heraus,
 * während der darunterliegende Screen mit leichtem Parallax-Effekt von links hineingleitet
 * und eingeblendet wird.
 */
private fun popTransitionSpec(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform =
    {
        val enter = slideInHorizontally(
            initialOffsetX = { -it / PARALLAX_FACTOR },
            animationSpec = tween(TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(FADE_DURATION_MS))
        val exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(FADE_DURATION_MS))
        (enter togetherWith exit).apply {
            targetContentZIndex = POP_TARGET_Z_INDEX
        }
    }

