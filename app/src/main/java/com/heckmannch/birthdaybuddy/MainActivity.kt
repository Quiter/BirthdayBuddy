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

                    composable("main") {
                        MainScreen(
                            filterManager = filterManager,
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }

                    composable("settings") {
                        SettingsMenuScreen(
                            onNavigate = { route -> navController.navigate(route) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // 3. Unterseite: Blockieren
                    composable("settings_block") {
                        var availableLabels by remember { mutableStateOf<Set<String>>(emptySet()) }
                        var isLoading by remember { mutableStateOf(true) } // NEU: Lade-Status

                        LaunchedEffect(Unit) {
                            withContext(Dispatchers.IO) {
                                val contacts = fetchBirthdays(this@MainActivity)
                                availableLabels = contacts.flatMap { it.labels }.toSortedSet()
                            }
                            isLoading = false // NEU: Laden beendet!
                        }

                        BlockLabelsScreen(filterManager, availableLabels, isLoading) { navController.popBackStack() }
                    }

                    // 4. Unterseite: Verstecken
                    composable("settings_hide") {
                        var availableLabels by remember { mutableStateOf<Set<String>>(emptySet()) }
                        var isLoading by remember { mutableStateOf(true) } // NEU: Lade-Status

                        LaunchedEffect(Unit) {
                            withContext(Dispatchers.IO) {
                                val contacts = fetchBirthdays(this@MainActivity)
                                availableLabels = contacts.flatMap { it.labels }.toSortedSet()
                            }
                            isLoading = false // NEU: Laden beendet!
                        }

                        HideLabelsScreen(filterManager, availableLabels, isLoading) { navController.popBackStack() }
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
            val descriptionText = "Benachrichtigungen für anstehende Geburtstage"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel("birthday_channel", name, importance).apply {
                description = descriptionText
            }

            val notificationManager: android.app.NotificationManager =
                getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}