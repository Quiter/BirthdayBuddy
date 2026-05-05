package com.heckmannch.birthdaybuddy2

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
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.heckmannch.birthdaybuddy2.ui.screens.home.HomeScreen
import com.heckmannch.birthdaybuddy2.ui.screens.settings.LabelSettingsScreen
import com.heckmannch.birthdaybuddy2.ui.screens.settings.SettingsScreen
import com.heckmannch.birthdaybuddy2.ui.theme.BirthdayBuddy2Theme
import com.heckmannch.birthdaybuddy2.viewmodel.BirthdayViewModel
import com.heckmannch.birthdaybuddy2.widget.BirthdayWidgetWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Tägliches Widget-Update planen
        BirthdayWidgetWorker.enqueueDailyUpdate(this)

        setContent {
            BirthdayBuddy2Theme {
                val viewModel: BirthdayViewModel = viewModel()
                val navController = rememberNavController()

                // Intent beim Start prüfen
                if (intent?.getBooleanExtra("SCROLL_TO_TOP", false) == true) {
                    viewModel.triggerScrollToTop()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        enterTransition = { materialSharedAxisZIn() },
                        exitTransition = { materialSharedAxisZOut() },
                        popEnterTransition = { materialSharedAxisZIn() },
                        popExitTransition = { materialSharedAxisZOut() },
                    ) {
                        composable("home") {
                            HomeScreen(viewModel = viewModel) {
                                navController.navigate("settings")
                            }
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateToLabels = {
                                    navController.navigate("label_settings")
                                },
                            ) {
                                navController.popBackStack()
                            }
                        }
                        composable("label_settings") {
                            LabelSettingsScreen(viewModel = viewModel) {
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

/**
 * Material 3 Shared Axis Z-Axis Transition (In)
 */
private fun materialSharedAxisZIn(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { (it * 0.1f).toInt() },
        animationSpec = tween(400, easing = FastOutSlowInEasing),
    ) + fadeIn(animationSpec = tween(400)) +
            scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
            )
}

/**
 * Material 3 Shared Axis Z-Axis Transition (Out)
 */
private fun materialSharedAxisZOut(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { (it * 0.1f).toInt() },
        animationSpec = tween(400, easing = FastOutSlowInEasing),
    ) + fadeOut(animationSpec = tween(400)) +
            scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
            )
}
