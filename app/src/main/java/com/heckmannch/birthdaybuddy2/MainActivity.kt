package com.heckmannch.birthdaybuddy2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.heckmannch.birthdaybuddy2.ui.screens.home.HomeScreen
import com.heckmannch.birthdaybuddy2.ui.screens.settings.SettingsScreen
import com.heckmannch.birthdaybuddy2.ui.theme.BirthdayBuddy2Theme
import com.heckmannch.birthdaybuddy2.viewmodel.BirthdayViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BirthdayBuddy2Theme {
                val viewModel: BirthdayViewModel = viewModel()
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { (it * 0.1f).toInt() },
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(400)) +
                                    scaleIn(
                                        initialScale = 0.92f,
                                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                                    )
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(300)) +
                                    scaleOut(
                                        targetScale = 0.92f,
                                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                                    )
                        },
                        popEnterTransition = {
                            fadeIn(animationSpec = tween(400)) +
                                    scaleIn(
                                        initialScale = 0.92f,
                                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                                    )
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { (it * 0.1f).toInt() },
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(400)) +
                                    scaleOut(
                                        targetScale = 0.92f,
                                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                                    )
                        }
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
