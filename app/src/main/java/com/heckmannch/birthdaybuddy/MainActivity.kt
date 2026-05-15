package com.heckmannch.birthdaybuddy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.labels.LabelSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.NotificationSettingsScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.backup.BackupScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.about.AboutScreen
import com.heckmannch.birthdaybuddy.ui.screens.settings.about.PrivacyPolicyScreen
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.viewmodel.BirthdayViewModel
import com.heckmannch.birthdaybuddy.widget.BirthdayWidgetWorker

/**
 * Navigation Routes
 */
private object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val LABEL_SETTINGS = "label_settings"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val BACKUP_SETTINGS = "backup_settings"
    const val ABOUT = "about"
    const val PRIVACY_POLICY = "privacy_policy"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var lastInteractionTime: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Schedule next widget update
        BirthdayWidgetWorker.enqueueNextUpdate(this)

        setContent {
            BirthdayBuddyTheme {
                val viewModel: BirthdayViewModel = hiltViewModel()
                val navController = rememberNavController()

                // Inaktivitäts-Check: Filter nach 5 Minuten zurücksetzen
                LaunchedEffect(Unit) {
                    while (true) {
                        kotlinx.coroutines.delay(10000) // Alle 10 Sekunden prüfen
                        if (System.currentTimeMillis() - lastInteractionTime > 5 * 60 * 1000) {
                            viewModel.resetFilters()
                        }
                    }
                }

                // React to intent changes (Initial start and onNewIntent)
                HandleIntents(intent, viewModel, navController)

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavigation(navController, viewModel)
                }
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        lastInteractionTime = System.currentTimeMillis()
    }

    @Composable
    private fun HandleIntents(intent: Intent?, viewModel: BirthdayViewModel, navController: NavHostController) {
        LaunchedEffect(intent) {
            if (intent?.getBooleanExtra("SCROLL_TO_TOP", false) == true) {
                viewModel.triggerScrollToTop()
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
                viewModel.triggerSearchFocus()
                intent.removeExtra("OPEN_SEARCH")
            }
            if (intent?.getBooleanExtra("OPEN_ADD_CONTACT", false) == true) {
                // Sync triggern, falls ein neuer Kontakt hinzugefügt wurde
                viewModel.syncContacts()
                intent.removeExtra("OPEN_ADD_CONTACT")
            }
        }
    }

    @Composable
    private fun AppNavigation(navController: NavHostController, viewModel: BirthdayViewModel) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            enterTransition = { sharedAxisZIn() },
            exitTransition = { sharedAxisZOut() },
            popEnterTransition = { sharedAxisZIn() },
            popExitTransition = { sharedAxisZOut() },
        ) {
            composable(Routes.HOME) {
                HomeScreen(viewModel = viewModel) {
                    navController.navigate(Routes.SETTINGS)
                }
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToLabels = {
                        navController.navigate(Routes.LABEL_SETTINGS)
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Routes.NOTIFICATION_SETTINGS)
                    },
                    onNavigateToBackup = {
                        navController.navigate(Routes.BACKUP_SETTINGS)
                    },
                    onNavigateToAbout = {
                        navController.navigate(Routes.ABOUT)
                    },
                ) {
                    navController.popBackStack()
                }
            }
            composable(Routes.LABEL_SETTINGS) {
                LabelSettingsScreen(viewModel = viewModel) {
                    navController.popBackStack()
                }
            }
            composable(Routes.NOTIFICATION_SETTINGS) {
                NotificationSettingsScreen(viewModel = viewModel) {
                    navController.popBackStack()
                }
            }
            composable(Routes.BACKUP_SETTINGS) {
                BackupScreen(viewModel = viewModel) {
                    navController.popBackStack()
                }
            }
            composable(Routes.ABOUT) {
                AboutScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPrivacyPolicy = {
                        navController.navigate(Routes.PRIVACY_POLICY)
                    }
                )
            }
            composable(Routes.PRIVACY_POLICY) {
                PrivacyPolicyScreen {
                    navController.popBackStack()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Update activity intent so LaunchedEffect in setContent can react to it
        setIntent(intent)
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
