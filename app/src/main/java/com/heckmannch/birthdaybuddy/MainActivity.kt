package com.heckmannch.birthdaybuddy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowWidthSizeClass
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeScreen
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.OnboardingScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.about.AboutScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.about.PrivacyPolicyScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.backup.BackupScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.calendar.CalendarSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.labels.LabelSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.NotificationSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.sync.SyncSettingsScreen
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.viewmodel.BackupViewModel
import com.heckmannch.birthdaybuddy.viewmodel.CalendarViewModel
import com.heckmannch.birthdaybuddy.viewmodel.HomeViewModel
import com.heckmannch.birthdaybuddy.viewmodel.LabelViewModel
import com.heckmannch.birthdaybuddy.viewmodel.NotificationViewModel
import com.heckmannch.birthdaybuddy.viewmodel.OnboardingViewModel
import com.heckmannch.birthdaybuddy.widget.BirthdayWidgetWorker
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

/**
 * Navigation Routes
 */
private object Routes {
    const val HOME = "home"
    const val ONBOARDING = "onboarding"
    const val SETTINGS = "settings"
    const val LABEL_SETTINGS = "label_settings"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val CALENDAR_SETTINGS = "calendar_settings"
    const val BACKUP_SETTINGS = "backup_settings"
    const val SYNC_SETTINGS = "sync_settings"
    const val ABOUT = "about"
    const val PRIVACY_POLICY = "privacy_policy"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    private var lastInteractionTime: Long = System.currentTimeMillis()
    private val activityIntent = mutableStateOf<Intent?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        activityIntent.value = intent

        // Schedule next widget update
        BirthdayWidgetWorker.enqueueNextUpdate(this)

        lifecycleScope.launch {
            try {
                notificationRepository.syncScheduling()
            } catch (_: Exception) {
                // Safeguard
            }
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val homeViewModel: HomeViewModel = hiltViewModel()
            val onboardingViewModel: OnboardingViewModel = hiltViewModel()
            val onboardingCompleted by onboardingViewModel.onboardingCompleted.collectAsStateWithLifecycle()

            // Splash Screen so lange anzeigen, bis wir wissen, wo es hingeht
            splashScreen.setKeepOnScreenCondition {
                onboardingCompleted == null
            }

            BirthdayBuddyTheme {
                CompositionLocalProvider(LocalWindowWidthSizeClass provides windowSizeClass.widthSizeClass) {
                    val navController = rememberNavController()
                    val currentIntent by activityIntent

                    // Inaktivitäts-Check: Filter nach 5 Minuten bei Wiederaufnahme (ON_RESUME) zurücksetzen
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                if ((System.currentTimeMillis() - lastInteractionTime) > (5 * 60 * 1000)) {
                                    homeViewModel.resetFilters()
                                }
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    // React to intent changes (Initial start and onNewIntent)
                    HandleIntents(currentIntent, homeViewModel, navController)

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        AppNavigation(
                            navController,
                            homeViewModel,
                            onboardingViewModel,
                            windowSizeClass.widthSizeClass
                        )
                    }
                }
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        lastInteractionTime = System.currentTimeMillis()
    }

    @Composable
    private fun HandleIntents(
        intent: Intent?,
        homeViewModel: HomeViewModel,
        navController: NavHostController
    ) {
        LaunchedEffect(intent) {
            if (intent?.getBooleanExtra("SCROLL_TO_TOP", false) == true) {
                homeViewModel.triggerScrollToTop()
                intent.removeExtra("SCROLL_TO_TOP")
            }
            if (intent?.getBooleanExtra("NAVIGATE_TO_NOTIFICATIONS", false) == true) {
                navController.navigate(Routes.NOTIFICATION_SETTINGS)
                intent.removeExtra("NAVIGATE_TO_NOTIFICATIONS")
            }
            if (intent?.getBooleanExtra("OPEN_SEARCH", false) == true) {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { inclusive = true }
                }
                homeViewModel.triggerSearchFocus()
                intent.removeExtra("OPEN_SEARCH")
            }
            if (intent?.getBooleanExtra("OPEN_ADD_CONTACT", false) == true) {
                // Sync triggern, falls ein neuer Kontakt hinzugefügt wurde
                homeViewModel.syncContacts()
                intent.removeExtra("OPEN_ADD_CONTACT")
            }
        }
    }

    @Composable
    private fun AppNavigation(
        navController: NavHostController,
        homeViewModel: HomeViewModel,
        onboardingViewModel: OnboardingViewModel,
        windowWidthSizeClass: WindowWidthSizeClass,
    ) {
        val onboardingCompleted by onboardingViewModel.onboardingCompleted.collectAsStateWithLifecycle()

        // Warten bis der Status geladen wurde, um Flackern zu vermeiden
        if (onboardingCompleted == null) return

        NavHost(
            navController = navController,
            startDestination = if (onboardingCompleted == true) Routes.HOME else Routes.ONBOARDING,
            enterTransition = { sharedAxisZIn() },
            exitTransition = { sharedAxisZOut() },
            popEnterTransition = { sharedAxisZIn() },
            popExitTransition = { sharedAxisZOut() },
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    windowWidthSizeClass = windowWidthSizeClass
                ) {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            }
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = homeViewModel,
                    windowWidthSizeClass = windowWidthSizeClass
                ) {
                    navController.navigate(Routes.SETTINGS)
                }
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
                    onNavigateToLabels = {
                        navController.navigate(Routes.LABEL_SETTINGS)
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Routes.NOTIFICATION_SETTINGS)
                    },
                    onNavigateToCalendar = {
                        navController.navigate(Routes.CALENDAR_SETTINGS)
                    },
                    onNavigateToBackup = {
                        navController.navigate(Routes.BACKUP_SETTINGS)
                    },
                    onNavigateToSync = {
                        navController.navigate(Routes.SYNC_SETTINGS)
                    },
                    onNavigateToAbout = {
                        navController.navigate(Routes.ABOUT)
                    },
                ) {
                    navController.popBackStack()
                }
            }
            composable(Routes.LABEL_SETTINGS) {
                val labelViewModel: LabelViewModel = hiltViewModel()
                LabelSettingsScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
                    viewModel = labelViewModel
                ) {
                    navController.popBackStack()
                }
            }
            composable(Routes.NOTIFICATION_SETTINGS) {
                val notificationViewModel: NotificationViewModel = hiltViewModel()
                NotificationSettingsScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
                    viewModel = notificationViewModel
                ) {
                    navController.popBackStack()
                }
            }
            composable(Routes.CALENDAR_SETTINGS) {
                val calendarViewModel: CalendarViewModel = hiltViewModel()
                CalendarSettingsScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
                    viewModel = calendarViewModel
                ) {
                    navController.popBackStack()
                }
            }
            composable(Routes.BACKUP_SETTINGS) {
                val backupViewModel: BackupViewModel = hiltViewModel()
                BackupScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
                    viewModel = backupViewModel
                ) {
                    navController.popBackStack()
                }
            }
            composable(Routes.SYNC_SETTINGS) {
                SyncSettingsScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
                    viewModel = homeViewModel
                ) {
                    navController.popBackStack()
                }
            }
            composable(Routes.ABOUT) {
                AboutScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPrivacyPolicy = {
                        navController.navigate(Routes.PRIVACY_POLICY)
                    },
                )
            }
            composable(Routes.PRIVACY_POLICY) {
                PrivacyPolicyScreen(windowWidthSizeClass = windowWidthSizeClass) {
                    navController.popBackStack()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Update activity intent so LaunchedEffect in setContent can react to it
        setIntent(intent)
        activityIntent.value = intent
    }
}

/**
 * Material 3 Shared Axis Z-Axis Transition (In)
 * Refined for a smoother feel.
 */
private fun sharedAxisZIn(): EnterTransition {
    return fadeIn(animationSpec = tween(300)) +
            scaleIn(
                initialScale = 0.94f,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
            )
}

/**
 * Material 3 Shared Axis Z-Axis Transition (Out)
 */
private fun sharedAxisZOut(): ExitTransition {
    return fadeOut(animationSpec = tween(300)) +
            scaleOut(
                targetScale = 0.94f,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
            )
}
