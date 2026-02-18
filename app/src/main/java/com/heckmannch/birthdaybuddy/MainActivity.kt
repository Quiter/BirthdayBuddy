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

/**
 * Der Haupteinstiegspunkt der Anwendung.
 * Verantwortlich für das Setup der Notification-Channels, des Splash-Screens
 * und der zentralen Navigationsstruktur (Compose Navigation).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialisiert den Splash-Screen (Android 12+ API)
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Erstellt den Benachrichtigungskanal für die Geburtstags-Erinnerungen
        createNotificationChannel()
        
        // Aktiviert Edge-to-Edge Design (Inhalt fließt unter Status- und Navigationsleiste)
        enableEdgeToEdge()

        setContent {
            BirthdayBuddyTheme {
                val navController = rememberNavController()
                
                // Manager für alle persistenten Filter- und Einstellungswerte (SharedPreferences/DataStore)
                val filterManager = remember { FilterManager(this@MainActivity) }

                // Zentraler Navigations-Host für die App
                NavHost(navController = navController, startDestination = "main") {
                    
                    // Hauptbildschirm mit der Geburtstagsliste
                    composable("main") { 
                        MainScreen(filterManager) { navController.navigate("settings") } 
                    }

                    // Hauptmenü der Einstellungen
                    composable("settings") { 
                        SettingsMenuScreen({ navController.navigate(it) }, { navController.popBackStack() }) 
                    }

                    // Dynamische Routen für verschiedene Label-Auswahl-Screens in den Einstellungen.
                    // Diese nutzen eine gemeinsame Logik zum Laden der verfügbaren Kontakt-Labels.
                    listOf(
                        "settings_block", "settings_hide", 
                        "settings_widget_include", "settings_widget_exclude"
                    ).forEach { route ->
                        composable(route) {
                            var availableLabels by remember { mutableStateOf<Set<String>>(emptySet()) }
                            var isLoading by remember { mutableStateOf(true) }
                            
                            // Lädt alle Labels asynchron aus der Kontakt-Datenbank
                            LaunchedEffect(Unit) {
                                withContext(Dispatchers.IO) {
                                    availableLabels = fetchBirthdays(this@MainActivity)
                                        .flatMap { it.labels }
                                        .toSortedSet()
                                }
                                isLoading = false
                            }

                            // Wählt den entsprechenden Screen basierend auf der Route aus
                            when (route) {
                                "settings_block" -> BlockLabelsScreen(filterManager, availableLabels, isLoading) { navController.popBackStack() }
                                "settings_hide" -> HideLabelsScreen(filterManager, availableLabels, isLoading) { navController.popBackStack() }
                                "settings_widget_include" -> WidgetIncludeLabelsScreen(filterManager, availableLabels, isLoading) { navController.popBackStack() }
                                "settings_widget_exclude" -> WidgetExcludeLabelsScreen(filterManager, availableLabels, isLoading) { navController.popBackStack() }
                            }
                        }
                    }

                    // Screen für die Konfiguration der Weckzeiten / Benachrichtigungen
                    composable("settings_alarms") { 
                        AlarmsScreen(filterManager) { navController.popBackStack() }
                    }
                }
            }
        }
    }

    /**
     * Erstellt ab Android 8.0 (Oreo) einen Notification Channel, 
     * damit das System weiß, wie Benachrichtigungen priorisiert werden sollen.
     */
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
