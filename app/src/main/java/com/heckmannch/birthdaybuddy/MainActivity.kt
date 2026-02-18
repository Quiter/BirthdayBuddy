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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        enableEdgeToEdge()

        setContent {
            BirthdayBuddyTheme {
                val navController = rememberNavController()
                val filterManager = remember { FilterManager(this@MainActivity) }

                NavHost(navController = navController, startDestination = "main") {

                    composable("main") { MainScreen(filterManager) { navController.navigate("settings") } }

                    composable("settings") { SettingsMenuScreen({ navController.navigate(it) }, { navController.popBackStack() }) }

                    composable("settings_block") {
                        var availableLabels by remember { mutableStateOf<Set<String>>(emptySet()) }
                        var isLoading by remember { mutableStateOf(true) }
                        LaunchedEffect(Unit) {
                            withContext(Dispatchers.IO) { availableLabels = fetchBirthdays(this@MainActivity).flatMap { it.labels }.toSortedSet() }
                            isLoading = false
                        }
                        BlockLabelsScreen(filterManager, availableLabels, isLoading) { navController.popBackStack() }
                    }

                    composable("settings_hide") {
                        var availableLabels by remember { mutableStateOf<Set<String>>(emptySet()) }
                        var isLoading by remember { mutableStateOf(true) }
                        LaunchedEffect(Unit) {
                            withContext(Dispatchers.IO) { availableLabels = fetchBirthdays(this@MainActivity).flatMap { it.labels }.toSortedSet() }
                            isLoading = false
                        }
                        HideLabelsScreen(filterManager, availableLabels, isLoading) { navController.popBackStack() }
                    }

                    composable("settings_alarms") { AlarmsScreen(filterManager) { navController.popBackStack() } }

                    // NEU: Route für Widget Include
                    composable("settings_widget_include") {
                        var availableLabels by remember { mutableStateOf<Set<String>>(emptySet()) }
                        var isLoading by remember { mutableStateOf(true) }
                        LaunchedEffect(Unit) {
                            withContext(Dispatchers.IO) { availableLabels = fetchBirthdays(this@MainActivity).flatMap { it.labels }.toSortedSet() }
                            isLoading = false
                        }
                        WidgetIncludeLabelsScreen(filterManager, availableLabels, isLoading) { navController.popBackStack() }
                    }

                    // NEU: Route für Widget Exclude
                    composable("settings_widget_exclude") {
                        var availableLabels by remember { mutableStateOf<Set<String>>(emptySet()) }
                        var isLoading by remember { mutableStateOf(true) }
                        LaunchedEffect(Unit) {
                            withContext(Dispatchers.IO) { availableLabels = fetchBirthdays(this@MainActivity).flatMap { it.labels }.toSortedSet() }
                            isLoading = false
                        }
                        WidgetExcludeLabelsScreen(filterManager, availableLabels, isLoading) { navController.popBackStack() }
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