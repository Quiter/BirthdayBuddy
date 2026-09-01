package com.heckmannch.birthdaybuddy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.rememberNavBackStack
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowAdaptiveInfo
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowSizeClass
import com.heckmannch.birthdaybuddy.ui.navigation.AppNavHost
import com.heckmannch.birthdaybuddy.ui.navigation.Home
import com.heckmannch.birthdaybuddy.ui.navigation.Onboarding
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Haupt-Activity von BirthdayBuddy.
 *
 * Reagiert auf eingehende Intents (z.B. App-Shortcuts, Widget-Klicks, Benachrichtigungen)
 * und reicht diese reaktiv über [AppViewModel] an [AppNavHost] zur sicheren, typen-geprüften Verarbeitung weiter.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Splash Screen so lange anzeigen, bis der Initialzustand (Onboarding-Status) geladen ist
        splashScreen.setKeepOnScreenCondition {
            appViewModel.onboardingCompleted.value == null
        }

        enableEdgeToEdge()

        // Nur beim Kaltstart / Initialaufruf an das ViewModel übergeben, um Re-Execution bei Recreations zu verhindern
        if (savedInstanceState == null) {
            appViewModel.handleIntent(intent)
        }

        setContent {
            val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
            val windowSizeClass = windowAdaptiveInfo.windowSizeClass
            val onboardingCompleted by appViewModel.onboardingCompleted.collectAsStateWithLifecycle()
            val appSettings by appViewModel.appSettings.collectAsStateWithLifecycle()
            val pendingIntent by appViewModel.pendingIntent.collectAsStateWithLifecycle()

            BirthdayBuddyTheme(
                themeMode = appSettings.themeMode,
                themeAmoled = appSettings.themeAmoled,
                themeAccent = appSettings.themeAccent,
                customAccentColorHex = appSettings.customAccentColor
            ) {
                if (onboardingCompleted != null) {
                    CompositionLocalProvider(
                        LocalWindowSizeClass provides windowSizeClass,
                        LocalWindowAdaptiveInfo provides windowAdaptiveInfo
                    ) {
                        val backStack = rememberNavBackStack(
                            if (onboardingCompleted == true) Home else Onboarding
                        )

                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            AppNavHost(
                                backStack = backStack,
                                intent = pendingIntent,
                                onIntentHandled = appViewModel::consumeIntent
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Gemäß Android Intent Security Best Practices:
        // setIntent(intent) muss aufgerufen werden, um die Intent-Referenz der Activity zu aktualisieren.
        setIntent(intent)
        appViewModel.handleIntent(intent)
    }
}
