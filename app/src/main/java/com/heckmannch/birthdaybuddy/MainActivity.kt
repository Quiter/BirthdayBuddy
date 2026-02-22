package com.heckmannch.birthdaybuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.screens.*
import com.heckmannch.birthdaybuddy.ui.Route
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.heckmannch.birthdaybuddy.data.FilterManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var filterManager: FilterManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        setContent {
            BirthdayBuddyTheme {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()
                
                // Dank HiltViewModel Annotation im ViewModel wird hier automatisch die Hilt-Instanz genutzt
                val mainViewModel: MainViewModel = viewModel()

                val uiState by mainViewModel.uiState.collectAsState()

                val onSafeBack = {
                    if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                        navController.popBackStack()
                    }
                }

                // Logik zum Zurücksetzen auf den MainScreen nach längerer Inaktivität
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_STOP -> {
                                // App geht in den Hintergrund -> Zeit speichern
                                scope.launch {
                                    filterManager.saveLastBackgroundTime(System.currentTimeMillis())
                                }
                            }
                            Lifecycle.Event.ON_START -> {
                                // App kommt in den Vordergrund -> Zeit prüfen
                                scope.launch {
                                    val prefs = filterManager.preferencesFlow.first()
                                    val lastTime = prefs.lastBackgroundTime
                                    if (lastTime != 0L) {
                                        val diff = System.currentTimeMillis() - lastTime
                                        // Wenn mehr als 30 Minuten vergangen sind (1800000 ms)
                                        if (diff > 30 * 60 * 1000) {
                                            // Zurück zum MainScreen, falls wir nicht schon dort sind
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
