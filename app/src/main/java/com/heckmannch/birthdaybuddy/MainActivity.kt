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
import com.heckmannch.birthdaybuddy.utils.FilterManager
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.heckmannch.birthdaybuddy.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        createNotificationChannel()
        enableEdgeToEdge()

        setContent {
            BirthdayBuddyTheme {
                val navController = rememberNavController()
                val mainViewModel: MainViewModel = viewModel()
                
                val availableLabels by mainViewModel.availableLabels.collectAsState()
                val isLoadingLabels by mainViewModel.isLoading.collectAsState()
                val filterManager = remember { FilterManager(this@MainActivity) }

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
                        BlockLabelsScreen(filterManager, availableLabels, isLoadingLabels) { navController.popBackStack() }
                    }
                    composable<Route.HideLabels> {
                        HideLabelsScreen(filterManager, availableLabels, isLoadingLabels) { navController.popBackStack() }
                    }
                    composable<Route.WidgetIncludeLabels> {
                        WidgetIncludeLabelsScreen(filterManager, availableLabels, isLoadingLabels) { navController.popBackStack() }
                    }
                    composable<Route.WidgetExcludeLabels> {
                        WidgetExcludeLabelsScreen(filterManager, availableLabels, isLoadingLabels) { navController.popBackStack() }
                    }

                    composable<Route.Alarms> {
                        AlarmsScreen(filterManager) { navController.popBackStack() }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val name = "Geburtstags-Erinnerungen"
        val channel = android.app.NotificationChannel("birthday_channel", name, android.app.NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Benachrichtigungen für anstehende Geburtstage"
        }
        getSystemService(android.app.NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}
