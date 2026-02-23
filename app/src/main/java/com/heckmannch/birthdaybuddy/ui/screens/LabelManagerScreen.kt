package com.heckmannch.birthdaybuddy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.data.FilterManager
import com.heckmannch.birthdaybuddy.utils.updateWidget
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelManagerScreen(
    availableLabels: Set<String>,
    isLoading: Boolean,
    onBack: () -> Unit,
    viewModel: LabelViewModel = hiltViewModel()
) {
    val filterManager = viewModel.filterManager
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val hiddenDrawer by filterManager.hiddenDrawerLabelsFlow.collectAsState(initial = emptySet())
    val widgetSelected by filterManager.widgetSelectedLabelsFlow.collectAsState(initial = emptySet())
    val notifSelected by filterManager.notificationSelectedLabelsFlow.collectAsState(initial = emptySet())
    val globalExcluded by filterManager.excludedLabelsFlow.collectAsState(initial = emptySet())
    val showIntro by filterManager.showLabelManagerIntroFlow.collectAsState(initial = false)

    val tabs = listOf(
        TabItem(stringResource(R.string.label_manager_tab_app), Icons.Default.PhoneAndroid, stringResource(R.string.label_manager_desc_app)),
        TabItem(stringResource(R.string.label_manager_tab_widget), Icons.Default.Widgets, stringResource(R.string.label_manager_desc_widget)),
        TabItem(stringResource(R.string.label_manager_tab_alarms), Icons.Default.Notifications, stringResource(R.string.label_manager_desc_alarms))
    )

    val pagerState = rememberPagerState(pageCount = { tabs.size })

    val allLabel = stringResource(R.string.label_all)
    val favoritesLabel = stringResource(R.string.label_favorites)

    if (showIntro) {
        LabelManagerIntroDialog(
            onDismiss = { dontShowAgain ->
                scope.launch {
                    if (dontShowAgain) {
                        filterManager.setShowLabelManagerIntro(false)
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.label_manager_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.label_selection_back))
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                divider = {},
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(pagerState.currentPage),
                        width = 64.dp,
                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                    )
                }
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { 
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { 
                            Text(
                                tab.title,
                                style = if (pagerState.currentPage == index) 
                                    MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                else 
                                    MaterialTheme.typography.titleSmall
                            ) 
                        },
                        icon = { 
                            if (pagerState.currentPage == index) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = tab.icon, 
                                        contentDescription = null,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            } else {
                                Icon(tab.icon, null, modifier = Modifier.padding(bottom = 4.dp))
                            }
                        }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) { pageIndex ->
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (availableLabels.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.label_manager_empty))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                        )
                    ) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = tabs[pageIndex].description,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val sortedLabels = availableLabels.toList().sortedWith(compareBy<String> {
                            when (it) {
                                "My Contacts" -> 0
                                "Starred in Android" -> 1
                                else -> 2
                            }
                        }.thenBy { it })
                        
                        items(sortedLabels) { label ->
                            val isBlockedGlobal = globalExcluded.contains(label)
                            val isVisible = when(pageIndex) {
                                0 -> !hiddenDrawer.contains(label)
                                1 -> widgetSelected.contains(label)
                                else -> notifSelected.contains(label)
                            }

                            val displayText = when(label) {
                                "My Contacts" -> allLabel
                                "Starred in Android" -> favoritesLabel
                                else -> label
                            }

                            LabelManagerRow(
                                label = displayText,
                                isVisible = isVisible,
                                isBlockedGlobal = isBlockedGlobal,
                                onToggleVisibility = {
                                    scope.launch {
                                        when(pageIndex) {
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
                                onToggleBlock = if (label != "My Contacts") {
                                    {
                                        scope.launch {
                                            val newGlobal = globalExcluded.toMutableSet()
                                            if (isBlockedGlobal) newGlobal.remove(label) else newGlobal.add(label)
                                            filterManager.saveExcludedLabels(newGlobal)
                                        }
                                    }
                                } else null
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LabelManagerIntroDialog(onDismiss: (Boolean) -> Unit) {
    var checked by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDialog = false
                onDismiss(checked)
            },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(
                    text = stringResource(R.string.label_manager_intro_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.label_manager_intro_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .toggleable(
                                value = checked,
                                onValueChange = { checked = it },
                                role = Role.Checkbox
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Text(
                            text = stringResource(R.string.label_manager_intro_dont_show_again),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showDialog = false
                        onDismiss(checked)
                    }
                ) {
                    Text(stringResource(R.string.label_manager_intro_button))
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}

private data class TabItem(val title: String, val icon: ImageVector, val description: String)

@Composable
fun LabelManagerRow(
    label: String,
    isVisible: Boolean,
    isBlockedGlobal: Boolean,
    onToggleVisibility: () -> Unit,
    onToggleBlock: (() -> Unit)? = null
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
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
                Text(
                    stringResource(R.string.label_manager_status_blocked), 
                    color = MaterialTheme.colorScheme.error, 
                    style = MaterialTheme.typography.labelSmall
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleVisibility,
                    // Die Sichtbarkeit soll auch dann änderbar sein, wenn das Label geblockt ist.
                    // Nur die visuelle Darstellung (Icons/Farben) soll den Status widerspiegeln.
                    enabled = true 
                ) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = if (isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
                
                if (onToggleBlock != null) {
                    VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))
                    
                    IconButton(onClick = onToggleBlock) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = if (isBlockedGlobal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    )
}
