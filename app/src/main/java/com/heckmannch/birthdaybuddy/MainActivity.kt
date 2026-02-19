package com.heckmannch.birthdaybuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
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
        
        createNotificationChannel()
        enableEdgeToEdge()

        setContent {
            BirthdayBuddyTheme {
                val navController = rememberNavController()
                
                // Explizite Angabe des korrekten ViewModels aus dem screens-Paket
                val mainViewModel: com.heckmannch.birthdaybuddy.ui.screens.MainViewModel = 
                    viewModel(factory = com.heckmannch.birthdaybuddy.ui.screens.MainViewModel.Factory)

                val uiState by mainViewModel.uiState.collectAsState()
                val container = (application as BirthdayApplication).container
                val filterManager = container.filterManager

                NavHost(navController = navController, startDestination = Route.Main) {
                    
                    composable<Route.Main> { 
                        MainScreen(mainViewModel, filterManager) { navController.navigate(Route.Settings) } 
                    }

                    composable<Route.Settings> { 
                        SettingsMenuScreen(
                            onNavigate = { routeName ->
                                val target = when(routeName) {
                                    "settings_alarms" -> Route.Alarms
                                    "settings_hide" -> Route.HideLabels
                                    "settings_block" -> Route.BlockLabels
                                    "settings_widget_include" -> Route.WidgetIncludeLabels
                                    "settings_widget_exclude" -> Route.WidgetExcludeLabels
                                    else -> Route.Main
                                }
                                navController.navigate(target)
                            }, 
                            onBack = { navController.popBackStack() }
                        ) 
                    }

                    composable<Route.BlockLabels> {
                        SettingsBlockLabelsScreen(
                            filterManager, 
                            uiState.availableLabels, 
                            uiState.isLoading
                        ) { navController.popBackStack() }
                    }
                    composable<Route.HideLabels> {
                        SettingsHideLabelsScreen(
                            filterManager, 
                            uiState.availableLabels, 
                            uiState.isLoading
                        ) { navController.popBackStack() }
                    }
                    composable<Route.WidgetIncludeLabels> {
                        SettingsWidgetIncludeLabelsScreen(
                            filterManager, 
                            uiState.availableLabels, 
                            uiState.isLoading
                        ) { navController.popBackStack() }
                    }
                    composable<Route.WidgetExcludeLabels> {
                        SettingsWidgetExcludeLabelsScreen(
                            filterManager, 
                            uiState.availableLabels, 
                            uiState.isLoading
                        ) { navController.popBackStack() }
                    }

                    composable<Route.Alarms> {
                        SettingsAlarmsScreen(filterManager) { navController.popBackStack() }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.notification_channel_name)
        val descriptionText = getString(R.string.notification_channel_desc)
        val channel = android.app.NotificationChannel("birthday_channel", name, android.app.NotificationManager.IMPORTANCE_HIGH).apply {
            description = descriptionText
        }
        getSystemService(android.app.NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}
