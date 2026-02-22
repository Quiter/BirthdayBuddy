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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        setContent {
            BirthdayBuddyTheme {
                val navController = rememberNavController()
                
                // Dank HiltViewModel Annotation im ViewModel wird hier automatisch die Hilt-Instanz genutzt
                val mainViewModel: MainViewModel = viewModel()

                val uiState by mainViewModel.uiState.collectAsState()

                val onSafeBack = {
                    if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                        navController.popBackStack()
                    }
                }

                NavHost(navController = navController, startDestination = Route.Main) {
                    
                    composable<Route.Main> { 
                        MainScreen(mainViewModel) { 
                            navController.navigate(Route.Settings) {
                                launchSingleTop = true
                            } 
                        } 
                    }

                    composable<Route.Settings> { 
                        SettingsMenuScreen(
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
                            availableLabels = uiState.availableLabels,
                            isLoading = uiState.isLoading,
                            onBack = onSafeBack
                        )
                    }

                    composable<Route.Alarms> {
                        SettingsAlarmsScreen(onBack = onSafeBack)
                    }
                }
            }
        }
    }
}
