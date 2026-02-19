package com.heckmannch.birthdaybuddy.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.heckmannch.birthdaybuddy.ui.components.*
import com.heckmannch.birthdaybuddy.data.FilterManager
import com.heckmannch.birthdaybuddy.utils.getAppVersionName
import com.heckmannch.birthdaybuddy.utils.updateWidget
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMenuScreen(
    onNavigate: (String) -> Unit, 
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val filterManager = remember { FilterManager(context) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val widgetCount by filterManager.widgetItemCountFlow.collectAsState(initial = 3)
    var showCountDialog by remember { mutableStateOf(false) }
    val versionName = getAppVersionName()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SectionHeader("Anzeige & Filter")
            SettingsCard {
                SettingsBlockRow(
                    title = "Labels anzeigen", 
                    subtitle = "Sichtbarkeit im Seitenmenü", 
                    icon = Icons.Default.Visibility, 
                    iconColor = MaterialTheme.colorScheme.secondary
                ) { onNavigate("settings_mainscreen_include") }
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                
                SettingsBlockRow(
                    title = "Labels blockieren", 
                    subtitle = "Kontakte global ignorieren", 
                    icon = Icons.Default.VisibilityOff, 
                    iconColor = MaterialTheme.colorScheme.secondary
                ) { onNavigate("settings_mainscreen_exclude") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader("Benachrichtigungen")
            SettingsCard {
                SettingsBlockRow(
                    title = "Alarme", 
                    subtitle = "Erinnerungszeiten konfigurieren", 
                    icon = Icons.Default.Notifications, 
                    iconColor = MaterialTheme.colorScheme.primary
                ) { onNavigate("settings_alarms") }
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                
                SettingsBlockRow(
                    title = "Filter: Einschließen", 
                    subtitle = "Labels für Benachrichtigungen", 
                    icon = Icons.Default.FilterList, 
                    iconColor = MaterialTheme.colorScheme.primary
                ) { onNavigate("settings_notification_include") }
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                
                SettingsBlockRow(
                    title = "Filter: Ausschließen", 
                    subtitle = "Labels ignorieren", 
                    icon = Icons.Default.Block, 
                    iconColor = MaterialTheme.colorScheme.primary
                ) { onNavigate("settings_notification_exclude") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader("Widget")
            SettingsCard {
                SettingsBlockRow(
                    title = "Anzahl", 
                    subtitle = "Bis zu $widgetCount Personen anzeigen", 
                    icon = Icons.AutoMirrored.Filled.List, 
                    iconColor = MaterialTheme.colorScheme.tertiary
                ) { showCountDialog = true }
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                
                SettingsBlockRow(
                    title = "Filter: Einschließen", 
                    subtitle = "Bestimmte Labels im Widget", 
                    icon = Icons.Default.FilterList, 
                    iconColor = MaterialTheme.colorScheme.tertiary
                ) { onNavigate("settings_widget_include") }
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                
                SettingsBlockRow(
                    title = "Filter: Ausschließen", 
                    subtitle = "Labels im Widget ignorieren", 
                    icon = Icons.Default.Block, 
                    iconColor = MaterialTheme.colorScheme.tertiary
                ) { onNavigate("settings_widget_exclude") }
            }

            SettingsFooter(versionName) {
                context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/Quiter/BirthdayBuddy".toUri()))
            }
        }
    }

    if (showCountDialog) {
        WidgetCountDialog(widgetCount, onDismiss = { showCountDialog = false }) { count ->
            scope.launch {
                filterManager.saveWidgetItemCount(count)
                updateWidget(context)
                showCountDialog = false
            }
        }
    }
}
