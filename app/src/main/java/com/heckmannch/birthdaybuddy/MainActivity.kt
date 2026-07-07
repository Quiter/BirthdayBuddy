package com.heckmannch.birthdaybuddy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.rememberNavBackStack
import com.heckmannch.birthdaybuddy.ui.components.ContactSyncEffect
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowHeightSizeClass
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowWidthSizeClass
import com.heckmannch.birthdaybuddy.ui.navigation.AppNavHost
import com.heckmannch.birthdaybuddy.ui.navigation.Home
import com.heckmannch.birthdaybuddy.ui.navigation.NotificationSettings
import com.heckmannch.birthdaybuddy.ui.navigation.Onboarding
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeViewModel
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.OnboardingViewModel
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.util.IntentExtras
import com.heckmannch.birthdaybuddy.widget.BirthdayWidgetWorker
import dagger.hilt.android.AndroidEntryPoint

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
                themeAccent = appSettings.themeAccent
            ) {
                if (onboardingCompleted != null) {
                    CompositionLocalProvider(
                        LocalWindowWidthSizeClass provides windowSizeClass.widthSizeClass,
                        LocalWindowHeightSizeClass provides windowSizeClass.heightSizeClass
                    ) {
                        val backStack = rememberNavBackStack(
                            if (onboardingCompleted == true) Home else Onboarding
                        )
                        val currentIntent by activityIntent

                        // Inaktivitäts-Check: Filter nach 5 Minuten bei Wiederaufnahme zurücksetzen
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
                            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                        }

                        // Live-Sync bei Änderungen im System-Adressbuch
                        ContactSyncEffect(onSyncNeeded = homeViewModel::syncContacts)

                        // React to intent changes (Initial start and onNewIntent)
                        LaunchedEffect(currentIntent) {
                            if (currentIntent?.getBooleanExtra(IntentExtras.SCROLL_TO_TOP, false) == true) {
                                homeViewModel.triggerScrollToTop()
                                currentIntent?.removeExtra(IntentExtras.SCROLL_TO_TOP)
                            }
                            if (currentIntent?.getBooleanExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS, false) == true) {
                                if (!backStack.contains(NotificationSettings)) {
                                    backStack.add(NotificationSettings)
                                }
                                currentIntent?.removeExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS)
                            }
                            if (currentIntent?.getBooleanExtra(IntentExtras.OPEN_SEARCH, false) == true) {
                                backStack.clear()
                                backStack.add(Home)
                                homeViewModel.triggerSearchFocus()
                                currentIntent?.removeExtra(IntentExtras.OPEN_SEARCH)
                            }
                            if (currentIntent?.getBooleanExtra(IntentExtras.OPEN_ADD_CONTACT, false) == true) {
                                homeViewModel.syncContacts()
                                currentIntent?.removeExtra(IntentExtras.OPEN_ADD_CONTACT)
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            AppNavHost(
                                backStack = backStack,
                                homeViewModel = homeViewModel,
                                onboardingViewModel = onboardingViewModel,
                                windowWidthSizeClass = windowSizeClass.widthSizeClass
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Update activity intent so LaunchedEffect in setContent can react to it
        setIntent(intent)
        activityIntent.value = intent
    }
}
