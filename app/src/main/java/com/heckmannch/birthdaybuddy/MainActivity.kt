package com.heckmannch.birthdaybuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.screens.*
import com.heckmannch.birthdaybuddy.ui.Route
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        setContent {
            BirthdayBuddyTheme {
                val navController = rememberNavController()
                
                val mainViewModel: MainViewModel = 
                    viewModel(factory = MainViewModel.Factory)

                val uiState by mainViewModel.uiState.collectAsState()
                val container = (application as BirthdayApplication).container
                val filterManager = container.filterManager

                val onSafeBack = {
                    if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                        navController.popBackStack()
                    }
                }

                NavHost(navController = navController, startDestination = Route.Main) {
                    
                    composable<Route.Main> { 
                        MainScreen(mainViewModel, filterManager) { 
                            navController.navigate(Route.Settings) {
                                launchSingleTop = true
                            } 
                        } 
                    }

                    composable<Route.Settings> { 
                        SettingsMenuScreen(
                            mainViewModel = mainViewModel,
                            onNavigate = { routeName ->
                                val target = when(routeName) {
                                    "settings_alarms" -> Route.Alarms
                                    "settings_label_manager" -> Route.LabelManager
                                    else -> Route.Main
                                }
                                navController.navigate(target) {
                                    launchSingleTop = true
                                }
                            }, 
                            onBack = onSafeBack
                        ) 
                    }

                    composable<Route.LabelManager> {
                        LabelManagerScreen(
                            filterManager = filterManager,
                            availableLabels = uiState.availableLabels,
                            isLoading = uiState.isLoading,
                            onBack = onSafeBack
                        )
                    }

                    composable<Route.Alarms> {
                        SettingsAlarmsScreen(filterManager, onBack = onSafeBack)
                    }
                }
            }
        }
    }
}
