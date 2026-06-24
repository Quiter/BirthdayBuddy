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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
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
import com.heckmannch.birthdaybuddy.util.IntentExtras
import com.heckmannch.birthdaybuddy.viewmodel.AppViewModel
import com.heckmannch.birthdaybuddy.viewmodel.BackupViewModel
import com.heckmannch.birthdaybuddy.viewmodel.CalendarViewModel
import com.heckmannch.birthdaybuddy.viewmodel.HomeViewModel
import com.heckmannch.birthdaybuddy.viewmodel.LabelViewModel
import com.heckmannch.birthdaybuddy.viewmodel.NotificationViewModel
import com.heckmannch.birthdaybuddy.viewmodel.OnboardingViewModel
import com.heckmannch.birthdaybuddy.viewmodel.ThemeViewModel
import com.heckmannch.birthdaybuddy.widget.BirthdayWidgetWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

/**
 * Navigation Routes
 */
@Serializable
object Home : NavKey

@Serializable
object Onboarding : NavKey

@Serializable
object Settings : NavKey

@Serializable
object LabelSettings : NavKey

@Serializable
object NotificationSettings : NavKey

@Serializable
object OtherEventsSettings : NavKey

@Serializable
object CalendarSettings : NavKey

@Serializable
object BackupSettings : NavKey

@Serializable
object ThemeSettings : NavKey

@Serializable
object SyncSettings : NavKey

@Serializable
object About : NavKey

@Serializable
object PrivacyPolicy : NavKey

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
                if (onboardingCompleted != null) {
                    CompositionLocalProvider(
                        LocalWindowWidthSizeClass provides windowSizeClass.widthSizeClass,
                        LocalWindowHeightSizeClass provides windowSizeClass.heightSizeClass
                    ) {
                        val backStack = rememberNavBackStack(if (onboardingCompleted == true) Home else Onboarding)
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
                        HandleIntents(currentIntent, homeViewModel, backStack)

                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            AppNavigation(
                                backStack,
                                homeViewModel,
                                onboardingViewModel,
                                windowSizeClass.widthSizeClass
                            )
                        }
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
        backStack: MutableList<NavKey>
    ) {
        LaunchedEffect(intent) {
            if (intent?.getBooleanExtra(IntentExtras.SCROLL_TO_TOP, false) == true) {
                homeViewModel.triggerScrollToTop()
                intent.removeExtra(IntentExtras.SCROLL_TO_TOP)
            }
            if (intent?.getBooleanExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS, false) == true) {
                if (!backStack.contains(NotificationSettings)) {
                    backStack.add(NotificationSettings)
                }
                intent.removeExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS)
            }
            if (intent?.getBooleanExtra(IntentExtras.OPEN_SEARCH, false) == true) {
                backStack.clear()
                backStack.add(Home)
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
        backStack: MutableList<NavKey>,
        homeViewModel: HomeViewModel,
        onboardingViewModel: OnboardingViewModel,
        windowWidthSizeClass: WindowWidthSizeClass,
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
            entryProvider = { key ->
                when (key) {
                    is Onboarding -> NavEntry(key) {
                        OnboardingScreen(
                            viewModel = onboardingViewModel,
                            windowWidthSizeClass = windowWidthSizeClass
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
                            windowWidthSizeClass = windowWidthSizeClass,
                            onNavigateToSettings = {
                                backStack.add(Settings)
                            }
                        )
                    }
                    is Settings -> NavEntry(key) {
                        SettingsScreen(
                            windowWidthSizeClass = windowWidthSizeClass,
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
                            windowWidthSizeClass = windowWidthSizeClass,
                            viewModel = labelViewModel
                        ) {
                            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        }
                    }
                    is NotificationSettings -> NavEntry(key) {
                        val notificationViewModel: NotificationViewModel = hiltViewModel()
                        NotificationSettingsScreen(
                            windowWidthSizeClass = windowWidthSizeClass,
                            viewModel = notificationViewModel
                        ) {
                            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        }
                    }
                    is OtherEventsSettings -> NavEntry(key) {
                        val notificationViewModel: NotificationViewModel = hiltViewModel()
                        OtherEventsSettingsScreen(
                            windowWidthSizeClass = windowWidthSizeClass,
                            viewModel = notificationViewModel
                        ) {
                            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        }
                    }
                    is CalendarSettings -> NavEntry(key) {
                        val calendarViewModel: CalendarViewModel = hiltViewModel()
                        CalendarSettingsScreen(
                            windowWidthSizeClass = windowWidthSizeClass,
                            viewModel = calendarViewModel
                        ) {
                            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        }
                    }
                    is BackupSettings -> NavEntry(key) {
                        val backupViewModel: BackupViewModel = hiltViewModel()
                        BackupScreen(
                            windowWidthSizeClass = windowWidthSizeClass,
                            viewModel = backupViewModel
                        ) {
                            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        }
                    }
                    is ThemeSettings -> NavEntry(key) {
                        val themeViewModel: ThemeViewModel = hiltViewModel()
                        ThemeSettingsScreen(
                            windowWidthSizeClass = windowWidthSizeClass,
                            viewModel = themeViewModel
                        ) {
                            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        }
                    }
                    is SyncSettings -> NavEntry(key) {
                        SyncSettingsScreen(
                            windowWidthSizeClass = windowWidthSizeClass,
                            viewModel = homeViewModel
                        ) {
                            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        }
                    }
                    is About -> NavEntry(key) {
                        AboutScreen(
                            windowWidthSizeClass = windowWidthSizeClass,
                            onNavigateBack = {
                                if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                            },
                            onNavigateToPrivacyPolicy = {
                                backStack.add(PrivacyPolicy)
                            },
                        )
                    }
                    is PrivacyPolicy -> NavEntry(key) {
                        PrivacyPolicyScreen(windowWidthSizeClass = windowWidthSizeClass) {
                            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        }
                    }
                    else -> throw IllegalArgumentException("Unknown key: $key")
                }
            }
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Update activity intent so LaunchedEffect in setContent can react to it
        setIntent(intent)
        activityIntent.value = intent
    }
}

