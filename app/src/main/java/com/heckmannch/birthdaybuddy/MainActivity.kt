package com.heckmannch.birthdaybuddy

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.screens.*
import com.heckmannch.birthdaybuddy.ui.Route
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.heckmannch.birthdaybuddy.data.FilterManager
import com.heckmannch.birthdaybuddy.utils.ScreenOnReceiver
import com.heckmannch.birthdaybuddy.utils.updateWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var filterManager: FilterManager

    private val screenOnReceiver = ScreenOnReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        // Registriere den Receiver für das Einschalten des Displays
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        registerReceiver(screenOnReceiver, filter)

        setContent {
            val userPrefs by filterManager.preferencesFlow.collectAsState(initial = null)
            
            if (userPrefs != null) {
                BirthdayBuddyTheme(theme = userPrefs!!.theme) {
                    val navController = rememberNavController()
                    val scope = rememberCoroutineScope()
                    val context = LocalContext.current
                    
                    val mainViewModel: MainViewModel = viewModel()
                    val uiState by mainViewModel.uiState.collectAsState()

                    val onSafeBack = {
                        if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                            navController.popBackStack()
                        }
                    }

                    /**
                     * Lifecycle-Überwachung für:
                     * 1. App-Reset nach 30 Minuten Inaktivität.
                     * 2. Widget-Update beim App-Start (stellt sicher, dass die Daten aktuell sind).
                     */
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_STOP -> {
                                    scope.launch {
                                        filterManager.saveLastBackgroundTime(System.currentTimeMillis())
                                    }
                                }
                                Lifecycle.Event.ON_START -> {
                                    scope.launch {
                                        val prefs = filterManager.preferencesFlow.first()
                                        
                                        // 1. Widget nur aktualisieren, wenn heute noch nicht geschehen
                                        val today = LocalDate.now().toString()
                                        if (prefs.lastWidgetUpdateDate != today) {
                                            updateWidget(context)
                                        }

                                        // 2. Prüfen, ob wir zum MainScreen zurückkehren müssen
                                        val lastTime = prefs.lastBackgroundTime
                                        if (lastTime != 0L) {
                                            val diff = System.currentTimeMillis() - lastTime
                                            if (diff > 30 * 60 * 1000) {
                                                if (navController.currentDestination?.route != Route.Main::class.qualifiedName) {
                                                    navController.navigate(Route.Main) {
                                                        popUpTo(Route.Main) { inclusive = true }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
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
                                onBack = {
                                    // Beim Verlassen des Label-Managers das Widget aktualisieren
                                    updateWidget(context)
                                    onSafeBack()
                                }
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

    override fun onDestroy() {
        super.onDestroy()
        // Wichtig: Receiver wieder abmelden, um Memory Leaks zu vermeiden
        unregisterReceiver(screenOnReceiver)
    }
}
