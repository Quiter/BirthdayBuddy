package com.heckmannch.birthdaybuddy

import android.content.Intent
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
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
import androidx.compose.ui.platform.LocalContext
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
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowHeightSizeClass
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
import com.heckmannch.birthdaybuddy.ui.screens.settings.otherevents.OtherEventsSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.sync.SyncSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.theme.ThemeSettingsScreen
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.viewmodel.AppViewModel
import com.heckmannch.birthdaybuddy.viewmodel.BackupViewModel
import com.heckmannch.birthdaybuddy.viewmodel.CalendarViewModel
import com.heckmannch.birthdaybuddy.viewmodel.HomeViewModel
import com.heckmannch.birthdaybuddy.viewmodel.LabelViewModel
import com.heckmannch.birthdaybuddy.viewmodel.NotificationViewModel
import com.heckmannch.birthdaybuddy.viewmodel.OnboardingViewModel
import com.heckmannch.birthdaybuddy.viewmodel.ThemeViewModel
import com.heckmannch.birthdaybuddy.util.IntentExtras
import com.heckmannch.birthdaybuddy.widget.BirthdayWidgetWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

/**
 * Navigation Routes
 */
@Serializable
object Home

@Serializable
object Onboarding

@Serializable
object Settings

@Serializable
object LabelSettings

@Serializable
object NotificationSettings

@Serializable
object OtherEventsSettings

@Serializable
object CalendarSettings

@Serializable
object BackupSettings

@Serializable
object ThemeSettings

@Serializable
object SyncSettings

@Serializable
object About

@Serializable
object PrivacyPolicy

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val appViewModel: AppViewModel = hiltViewModel()
            val homeViewModel: HomeViewModel = hiltViewModel()
            val onboardingViewModel: OnboardingViewModel = hiltViewModel()
            val onboardingCompleted by onboardingViewModel.onboardingCompleted.collectAsStateWithLifecycle()
            val appSettings by appViewModel.appSettings.collectAsStateWithLifecycle()

            // Splash Screen so lange anzeigen, bis wir wissen, wo es hingeht
            splashScreen.setKeepOnScreenCondition {
                onboardingCompleted == null
            }

            BirthdayBuddyTheme(
                themeMode = appSettings.themeMode,
                themeAmoled = appSettings.themeAmoled,
                themeAccent = appSettings.themeAccent,
                themeContrast = appSettings.themeContrast
            ) {
                CompositionLocalProvider(
                    LocalWindowWidthSizeClass provides windowSizeClass.widthSizeClass,
                    LocalWindowHeightSizeClass provides windowSizeClass.heightSizeClass
                ) {
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

                    // Live-Sync bei Änderungen im System-Adressbuch (ContentObserver mit 1s Debounce)
                    val context = LocalContext.current
                    DisposableEffect(context) {
                        val contentResolver = context.contentResolver
                        val mainHandler = Handler(Looper.getMainLooper())
                        val observer = object : ContentObserver(mainHandler) {
                            private val syncRunnable = Runnable { homeViewModel.syncContacts() }

                            override fun onChange(selfChange: Boolean) {
                                super.onChange(selfChange)
                                mainHandler.removeCallbacks(syncRunnable)
                                mainHandler.postDelayed(syncRunnable, 1000) // 1 Sekunde Debounce
                            }
                        }
                        try {
                            contentResolver.registerContentObserver(
                                ContactsContract.Contacts.CONTENT_URI,
                                true,
                                observer
                            )
                        } catch (_: SecurityException) {
                            // Keine Berechtigung vorhanden
                        }
                        onDispose {
                            contentResolver.unregisterContentObserver(observer)
                            mainHandler.removeCallbacksAndMessages(null)
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
            if (intent?.getBooleanExtra(IntentExtras.SCROLL_TO_TOP, false) == true) {
                homeViewModel.triggerScrollToTop()
                intent.removeExtra(IntentExtras.SCROLL_TO_TOP)
            }
            if (intent?.getBooleanExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS, false) == true) {
                navController.navigate(NotificationSettings)
                intent.removeExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS)
            }
            if (intent?.getBooleanExtra(IntentExtras.OPEN_SEARCH, false) == true) {
                navController.navigate(Home) {
                    popUpTo(Home) { inclusive = true }
                }
                homeViewModel.triggerSearchFocus()
                intent.removeExtra(IntentExtras.OPEN_SEARCH)
            }
            if (intent?.getBooleanExtra(IntentExtras.OPEN_ADD_CONTACT, false) == true) {
                // Sync triggern, falls ein neuer Kontakt hinzugefügt wurde
                homeViewModel.syncContacts()
                intent.removeExtra(IntentExtras.OPEN_ADD_CONTACT)
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
            startDestination = if (onboardingCompleted == true) Home else Onboarding,
            enterTransition = { sharedAxisZIn() },
            exitTransition = { sharedAxisZOut() },
            popEnterTransition = { sharedAxisZIn() },
            popExitTransition = { sharedAxisZOut() },
        ) {
            composable<Onboarding> {
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    windowWidthSizeClass = windowWidthSizeClass
                ) {
                    navController.navigate(Home) {
                        popUpTo(Onboarding) { inclusive = true }
                    }
                }
            }
            composable<Home> {
                HomeScreen(
                    viewModel = homeViewModel,
                    windowWidthSizeClass = windowWidthSizeClass
                ) {
                    navController.navigate(Settings)
                }
            }
            composable<Settings> {
                SettingsScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
                    homeViewModel = homeViewModel,
                    onNavigateToLabels = {
                        navController.navigate(LabelSettings)
                    },
                    onNavigateToNotifications = {
                        navController.navigate(NotificationSettings)
                    },
                    onNavigateToCalendar = {
                        navController.navigate(CalendarSettings)
                    },
                    onNavigateToBackup = {
                        navController.navigate(BackupSettings)
                    },
                    onNavigateToTheme = {
                        navController.navigate(ThemeSettings)
                    },
                    onNavigateToSync = {
                        navController.navigate(SyncSettings)
                    },
                    onNavigateToAbout = {
                        navController.navigate(About)
                    },
                    onNavigateToOtherEvents = {
                        navController.navigate(OtherEventsSettings)
                    },
                ) {
                    navController.popBackStack()
                }
            }
            composable<LabelSettings> {
                val labelViewModel: LabelViewModel = hiltViewModel()
                LabelSettingsScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
                    viewModel = labelViewModel
                ) {
                    navController.popBackStack()
                }
            }
            composable<NotificationSettings> {
                val notificationViewModel: NotificationViewModel = hiltViewModel()
                NotificationSettingsScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
                    viewModel = notificationViewModel
                ) {
                    navController.popBackStack()
                }
            }
            composable<OtherEventsSettings> {
                val notificationViewModel: NotificationViewModel = hiltViewModel()
                OtherEventsSettingsScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
                    viewModel = notificationViewModel
                ) {
                    navController.popBackStack()
                }
            }
            composable<CalendarSettings> {
                val calendarViewModel: CalendarViewModel = hiltViewModel()
                CalendarSettingsScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
                    viewModel = calendarViewModel
                ) {
                    navController.popBackStack()
                }
            }
            composable<BackupSettings> {
                val backupViewModel: BackupViewModel = hiltViewModel()
                BackupScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
                    viewModel = backupViewModel
                ) {
                    navController.popBackStack()
                }
            }
            composable<ThemeSettings> {
                val themeViewModel: ThemeViewModel = hiltViewModel()
                ThemeSettingsScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
                    viewModel = themeViewModel
                ) {
                    navController.popBackStack()
                }
            }
            composable<SyncSettings> {
                SyncSettingsScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
                    viewModel = homeViewModel
                ) {
                    navController.popBackStack()
                }
            }
            composable<About> {
                AboutScreen(
                    windowWidthSizeClass = windowWidthSizeClass,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPrivacyPolicy = {
                        navController.navigate(PrivacyPolicy)
                    },
                )
            }
            composable<PrivacyPolicy> {
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
