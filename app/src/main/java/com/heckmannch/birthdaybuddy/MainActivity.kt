package com.heckmannch.birthdaybuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.utils.FilterManager
import com.heckmannch.birthdaybuddy.utils.fetchBirthdays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
                val filterManager = remember { FilterManager(this@MainActivity) }

                NavHost(navController = navController, startDestination = "main") {
                    composable("main") { 
                        MainScreen(filterManager) { navController.navigate("settings") } 
                    }

                    composable("settings") { 
                        SettingsMenuScreen({ navController.navigate(it) }, { navController.popBackStack() }) 
                    }

                    // Optimierung: Gemeinsame Logik für Label-basierte Screens
                    listOf(
                        "settings_block", "settings_hide", 
                        "settings_widget_include", "settings_widget_exclude"
                    ).forEach { route ->
                        composable(route) {
                            var availableLabels by remember { mutableStateOf<Set<String>>(emptySet()) }
                            var isLoading by remember { mutableStateOf(true) }
                            
                            LaunchedEffect(Unit) {
                                withContext(Dispatchers.IO) {
                                    availableLabels = fetchBirthdays(this@MainActivity)
                                        .flatMap { it.labels }
                                        .toSortedSet()
                                }
                                isLoading = false
                            }

                            when (route) {
                                "settings_block" -> BlockLabelsScreen(filterManager, availableLabels, isLoading) { navController.popBackStack() }
                                "settings_hide" -> HideLabelsScreen(filterManager, availableLabels, isLoading) { navController.popBackStack() }
                                "settings_widget_include" -> WidgetIncludeLabelsScreen(filterManager, availableLabels, isLoading) { navController.popBackStack() }
                                "settings_widget_exclude" -> WidgetExcludeLabelsScreen(filterManager, availableLabels, isLoading) { navController.popBackStack() }
                            }
                        }
                    }

                    composable("settings_alarms") { 
                        AlarmsScreen(filterManager) { navController.popBackStack() }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Geburtstags-Erinnerungen"
            val channel = android.app.NotificationChannel("birthday_channel", name, android.app.NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Benachrichtigungen für anstehende Geburtstage"
            }
            getSystemService(android.app.NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
