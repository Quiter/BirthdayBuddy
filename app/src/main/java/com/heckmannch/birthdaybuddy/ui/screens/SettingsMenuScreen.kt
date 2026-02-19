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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.*
import com.heckmannch.birthdaybuddy.data.FilterManager
import com.heckmannch.birthdaybuddy.ui.theme.*
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
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.label_selection_back)) }
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
            // SEKTION: ANZEIGE & FILTER
            SectionHeader(stringResource(R.string.settings_section_display))
            SettingsGroup {
                SettingsBlockRow(
                    title = stringResource(R.string.settings_labels_show_title), 
                    subtitle = stringResource(R.string.settings_labels_show_desc), 
                    icon = Icons.Default.Visibility, 
                    iconContainerColor = SettingsColorDisplay,
                    isTop = true,
                    isBottom = false
                ) { onNavigate("settings_mainscreen_include") }
                
                SettingsBlockRow(
                    title = stringResource(R.string.settings_labels_block_title), 
                    subtitle = stringResource(R.string.settings_labels_block_desc), 
                    icon = Icons.Default.VisibilityOff, 
                    iconContainerColor = SettingsColorDisplay,
                    isTop = false,
                    isBottom = true
                ) { onNavigate("settings_mainscreen_exclude") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SEKTION: BENACHRICHTIGUNGEN
            SectionHeader(stringResource(R.string.settings_section_notifications))
            SettingsGroup {
                SettingsBlockRow(
                    title = stringResource(R.string.settings_alarms_title), 
                    subtitle = stringResource(R.string.settings_alarms_desc), 
                    icon = Icons.Default.Notifications, 
                    iconContainerColor = SettingsColorNotifications,
                    isTop = true,
                    isBottom = false
                ) { onNavigate("settings_alarms") }
                
                SettingsBlockRow(
                    title = stringResource(R.string.settings_notifications_include_title), 
                    subtitle = stringResource(R.string.settings_notifications_include_desc), 
                    icon = Icons.Default.Visibility,
                    iconContainerColor = SettingsColorNotifications,
                    isTop = false,
                    isBottom = false
                ) { onNavigate("settings_notification_include") }
                
                SettingsBlockRow(
                    title = stringResource(R.string.settings_notifications_exclude_title), 
                    subtitle = stringResource(R.string.settings_notifications_exclude_desc), 
                    icon = Icons.Default.VisibilityOff,
                    iconContainerColor = SettingsColorNotifications,
                    isTop = false,
                    isBottom = true
                ) { onNavigate("settings_notification_exclude") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SEKTION: WIDGET
            SectionHeader(stringResource(R.string.settings_section_widget))
            SettingsGroup {
                SettingsBlockRow(
                    title = stringResource(R.string.settings_widget_count_title), 
                    subtitle = stringResource(R.string.settings_widget_count_desc, widgetCount), 
                    icon = Icons.AutoMirrored.Filled.List, 
                    iconContainerColor = SettingsColorWidget,
                    isTop = true,
                    isBottom = false
                ) { showCountDialog = true }
                
                SettingsBlockRow(
                    title = stringResource(R.string.settings_widget_include_title), 
                    subtitle = stringResource(R.string.settings_widget_include_desc), 
                    icon = Icons.Default.Visibility,
                    iconContainerColor = SettingsColorWidget,
                    isTop = false,
                    isBottom = false
                ) { onNavigate("settings_widget_include") }
                
                SettingsBlockRow(
                    title = stringResource(R.string.settings_widget_exclude_title), 
                    subtitle = stringResource(R.string.settings_widget_exclude_desc),
                    icon = Icons.Default.VisibilityOff,
                    iconContainerColor = SettingsColorWidget,
                    isTop = false,
                    isBottom = true
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
