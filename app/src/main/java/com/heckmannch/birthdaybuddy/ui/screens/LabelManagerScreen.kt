package com.heckmannch.birthdaybuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.data.FilterManager
import com.heckmannch.birthdaybuddy.utils.updateWidget
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelManagerScreen(
    filterManager: FilterManager,
    availableLabels: Set<String>,
    isLoading: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Aktuelle Filter-Sets sammeln
    val selectedLabels by filterManager.selectedLabelsFlow.collectAsState(initial = emptySet())
    val hiddenDrawer by filterManager.hiddenDrawerLabelsFlow.collectAsState(initial = emptySet())
    val widgetSelected by filterManager.widgetSelectedLabelsFlow.collectAsState(initial = emptySet())
    val notifSelected by filterManager.notificationSelectedLabelsFlow.collectAsState(initial = emptySet())
    val globalExcluded by filterManager.excludedLabelsFlow.collectAsState(initial = emptySet())

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        TabItem("App", Icons.Default.PhoneAndroid, "Sichtbarkeit im Hauptmenü"),
        TabItem("Widget", Icons.Default.Widgets, "Kontakte auf dem Homescreen"),
        TabItem("Alarme", Icons.Default.Notifications, "Benachrichtigungs-Filter")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Label Manager") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Tab-Navigation
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                divider = {}
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tab.title) },
                        icon = { Icon(tab.icon, null) }
                    )
                }
            }

            // Sub-Header mit Beschreibung
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = tabs[selectedTab].description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    val sortedLabels = availableLabels.toList().sortedBy { it }
                    
                    items(sortedLabels) { label ->
                        val isBlockedGlobal = globalExcluded.contains(label)
                        
                        val isVisible = when(selectedTab) {
                            0 -> !hiddenDrawer.contains(label)
                            1 -> widgetSelected.contains(label)
                            else -> notifSelected.contains(label)
                        }

                        LabelManagerRow(
                            label = label,
                            isVisible = isVisible,
                            isBlockedGlobal = isBlockedGlobal,
                            onToggleVisibility = {
                                scope.launch {
                                    when(selectedTab) {
                                        0 -> {
                                            val newHidden = hiddenDrawer.toMutableSet()
                                            if (isVisible) newHidden.add(label) else newHidden.remove(label)
                                            filterManager.saveHiddenDrawerLabels(newHidden)
                                        }
                                        1 -> {
                                            val newWidget = widgetSelected.toMutableSet()
                                            if (isVisible) newWidget.remove(label) else newWidget.add(label)
                                            filterManager.saveWidgetSelectedLabels(newWidget)
                                            updateWidget(context)
                                        }
                                        2 -> {
                                            val newNotif = notifSelected.toMutableSet()
                                            if (isVisible) newNotif.remove(label) else newNotif.add(label)
                                            filterManager.saveNotificationSelectedLabels(newNotif)
                                        }
                                    }
                                }
                            },
                            onToggleBlock = {
                                scope.launch {
                                    val newGlobal = globalExcluded.toMutableSet()
                                    if (isBlockedGlobal) newGlobal.remove(label) else newGlobal.add(label)
                                    filterManager.saveExcludedLabels(newGlobal)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

data class TabItem(val title: String, val icon: ImageVector, val description: String)

@Composable
fun LabelManagerRow(
    label: String,
    isVisible: Boolean,
    isBlockedGlobal: Boolean,
    onToggleVisibility: () -> Unit,
    onToggleBlock: () -> Unit
) {
    val alpha = if (isBlockedGlobal) 0.5f else 1.0f
    
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        headlineContent = {
            Text(
                text = label,
                color = if (isBlockedGlobal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isVisible && !isBlockedGlobal) FontWeight.Bold else FontWeight.Normal
            )
        },
        supportingContent = {
            if (isBlockedGlobal) {
                Text("Vollständig ignoriert", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Sichtbarkeit Switch
                IconButton(
                    onClick = onToggleVisibility,
                    enabled = !isBlockedGlobal
                ) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = if (isVisible && !isBlockedGlobal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
                
                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))
                
                // Global Block Button
                IconButton(onClick = onToggleBlock) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = if (isBlockedGlobal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    )
}
