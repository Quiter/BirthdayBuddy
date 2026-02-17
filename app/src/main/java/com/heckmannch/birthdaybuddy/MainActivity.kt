package com.heckmannch.birthdaybuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.utils.FilterManager
import com.heckmannch.birthdaybuddy.utils.fetchBirthdays

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Systemvorbereitungen
        createNotificationChannel()
        enableEdgeToEdge()

        setContent {
            BirthdayBuddyTheme {
                val navController = rememberNavController()
                val filterManager = remember { FilterManager(this@MainActivity) }

                NavHost(navController = navController, startDestination = "main") {

                    // 1. Der ausgelagerte Hauptbildschirm
                    composable("main") {
                        MainScreen(
                            filterManager = filterManager,
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }

                    // 2. Der Einstellungsbildschirm
                    composable("settings") {
                        var contacts by remember { mutableStateOf<List<BirthdayContact>>(emptyList()) }
                        LaunchedEffect(Unit) {
                            contacts = fetchBirthdays(this@MainActivity)
                        }
                        val availableLabels = contacts.flatMap { it.labels }.toSortedSet()

                        SettingsScreen(
                            filterManager = filterManager,
                            availableLabels = availableLabels,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    // Richtet den unsichtbaren Kanal für die Geburtstags-Erinnerungen ein
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