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
    mainViewModel: MainViewModel,
    onNavigate: (String) -> Unit, 
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val filterManager = remember { FilterManager(context) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val widgetCount by filterManager.widgetItemCountFlow.collectAsState(initial = 3)
    val syncStatus by mainViewModel.syncStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showCountDialog by remember { mutableStateOf(false) }
    val versionName = getAppVersionName()

    val successMsg = stringResource(R.string.sync_success)

    // Status-Meldungen verarbeiten
    LaunchedEffect(syncStatus) {
        when (syncStatus) {
            is SyncStatus.Success -> {
                snackbarHostState.showSnackbar(successMsg)
                mainViewModel.resetSyncStatus()
            }
            is SyncStatus.Error -> {
                snackbarHostState.showSnackbar((syncStatus as SyncStatus.Error).message)
                mainViewModel.resetSyncStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            // SEKTION: ORGANISATION
            SectionHeader(stringResource(R.string.label_manager_section_org))
            SettingsGroup {
                SettingsBlockRow(
                    title = stringResource(R.string.label_manager_title), 
                    subtitle = stringResource(R.string.label_manager_subtitle), 
                    icon = Icons.Default.Style, 
                    iconContainerColor = SettingsColorOrganisation,
                    isTop = true,
                    isBottom = false
                ) { onNavigate("settings_label_manager") }
                
                SettingsBlockRow(
                    title = stringResource(R.string.drawer_reload_contacts), 
                    subtitle = stringResource(R.string.sync_desc), 
                    icon = Icons.Default.Refresh, 
                    iconContainerColor = SettingsColorOrganisation,
                    isTop = false,
                    isBottom = true,
                    showArrow = false
                ) { mainViewModel.loadContacts() }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SEKTION: BENACHRICHTIGUNGEN
            SectionHeader(stringResource(R.string.settings_section_notifications))
            SettingsGroup {
                SettingsBlockRow(
                    title = stringResource(R.string.settings_alarms_title), 
                    subtitle = stringResource(R.string.settings_alarms_desc), 
                    icon = Icons.Default.Notifications, 
                    iconContainerColor = SettingsColorNotifications,
                    isTop = true,
                    isBottom = true
                ) { onNavigate("settings_alarms") }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SEKTION: WIDGET
            SectionHeader(stringResource(R.string.settings_section_widget))
            SettingsGroup {
                SettingsBlockRow(
                    title = stringResource(R.string.settings_widget_count_title), 
                    subtitle = stringResource(R.string.settings_widget_count_desc, widgetCount), 
                    icon = Icons.AutoMirrored.Filled.List, 
                    iconContainerColor = SettingsColorWidget,
                    isTop = true,
                    isBottom = true
                ) { showCountDialog = true }
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
