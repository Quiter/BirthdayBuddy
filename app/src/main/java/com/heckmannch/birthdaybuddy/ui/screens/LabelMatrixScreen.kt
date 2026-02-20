package com.heckmannch.birthdaybuddy.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.data.FilterManager
import com.heckmannch.birthdaybuddy.utils.updateWidget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Zustände für ein Label in einem bestimmten Bereich.
 */
enum class MatrixState {
    SHOW,  // Sichtbar (Auge offen, farbig)
    HIDE,  // Nicht explizit eingeblendet (Auge offen, grau)
    BLOCK  // Ignoriert/Blockiert (Auge zu)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelMatrixScreen(
    filterManager: FilterManager,
    availableLabels: Set<String>,
    isLoading: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Alle relevanten Flows sammeln
    val hiddenDrawer by filterManager.hiddenDrawerLabelsFlow.collectAsState(initial = emptySet())
    val globalExcluded by filterManager.excludedLabelsFlow.collectAsState(initial = emptySet())
    
    val notifSelected by filterManager.notificationSelectedLabelsFlow.collectAsState(initial = emptySet())
    val notifExcluded by filterManager.notificationExcludedLabelsFlow.collectAsState(initial = emptySet())
    
    val widgetSelected by filterManager.widgetSelectedLabelsFlow.collectAsState(initial = emptySet())
    val widgetExcluded by filterManager.widgetExcludedLabelsFlow.collectAsState(initial = emptySet())

    var isLegendExpanded by remember { mutableStateOf(false) }

    // Dropdown-Menü Steuerung
    var showMenuForLabel by remember { mutableStateOf<Pair<String, String>?>(null) } // label to area

    // Beim ersten Start der Matrix (einmalig pro Installation) Legende aufklappen
    LaunchedEffect(Unit) {
        val alreadyShown = filterManager.matrixLegendShownFlow.first()
        if (!alreadyShown) {
            isLegendExpanded = true
            filterManager.setMatrixLegendShown(true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.label_matrix_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.label_selection_back))
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (availableLabels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.label_matrix_empty), textAlign = TextAlign.Center)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                
                // LEGENDE
                LegendSection(
                    isExpanded = isLegendExpanded,
                    onToggle = { isLegendExpanded = !isLegendExpanded }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Header Zeile
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Label",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    MatrixHeaderIcon(stringResource(R.string.label_matrix_column_drawer))
                    MatrixHeaderIcon(stringResource(R.string.label_matrix_column_notif))
                    MatrixHeaderIcon(stringResource(R.string.label_matrix_column_widget))
                }
                HorizontalDivider()

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(availableLabels.toList().sorted()) { label ->
                        // Zustände berechnen
                        val drawerState = when {
                            globalExcluded.contains(label) -> MatrixState.BLOCK
                            hiddenDrawer.contains(label) -> MatrixState.HIDE
                            else -> MatrixState.SHOW
                        }
                        
                        val notifState = when {
                            notifExcluded.contains(label) -> MatrixState.BLOCK
                            notifSelected.contains(label) -> MatrixState.SHOW
                            else -> MatrixState.HIDE
                        }
                        
                        val widgetState = when {
                            widgetExcluded.contains(label) -> MatrixState.BLOCK
                            widgetSelected.contains(label) -> MatrixState.SHOW
                            else -> MatrixState.HIDE
                        }

                        LabelMatrixRow(
                            label = label,
                            drawerState = drawerState,
                            notifState = notifState,
                            widgetState = widgetState,
                            onAreaClick = { area -> showMenuForLabel = label to area }
                        )

                        // Dropdown Menu für dieses Label
                        if (showMenuForLabel?.first == label) {
                            val area = showMenuForLabel!!.second
                            val currentState = when(area) {
                                "drawer" -> drawerState
                                "notif" -> notifState
                                else -> widgetState
                            }
                            
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                // Wir nutzen ein einfaches DropdownMenu an der Position der Icons
                                // Da wir 3 Icons haben, ist die Positionierung schwierig ohne "DropdownMenu" pro Icon.
                                // Hier implementieren wir eine elegantere Lösung: Ein modaler Dialog oder ein Popup.
                                // Für Android Studio Context nutzen wir hier ein DropdownMenu.
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    // Modal Dropdown/Picker wenn ein Label angeklickt wurde
    showMenuForLabel?.let { (label, area) ->
        val currentState = when(area) {
            "drawer" -> {
                val hidden = hiddenDrawer.contains(label)
                val excluded = globalExcluded.contains(label)
                if (excluded) MatrixState.BLOCK else if (hidden) MatrixState.HIDE else MatrixState.SHOW
            }
            "notif" -> {
                if (notifExcluded.contains(label)) MatrixState.BLOCK else if (notifSelected.contains(label)) MatrixState.SHOW else MatrixState.HIDE
            }
            else -> {
                if (widgetExcluded.contains(label)) MatrixState.BLOCK else if (widgetSelected.contains(label)) MatrixState.SHOW else MatrixState.HIDE
            }
        }

        AlertDialog(
            onDismissRequest = { showMenuForLabel = null },
            title = { Text(label, style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    MatrixOptionItem(MatrixState.SHOW, currentState == MatrixState.SHOW) {
                        scope.launch { updateState(label, area, MatrixState.SHOW, filterManager, hiddenDrawer, globalExcluded, notifSelected, notifExcluded, widgetSelected, widgetExcluded, context) }
                        showMenuForLabel = null
                    }
                    MatrixOptionItem(MatrixState.HIDE, currentState == MatrixState.HIDE) {
                        scope.launch { updateState(label, area, MatrixState.HIDE, filterManager, hiddenDrawer, globalExcluded, notifSelected, notifExcluded, widgetSelected, widgetExcluded, context) }
                        showMenuForLabel = null
                    }
                    MatrixOptionItem(MatrixState.BLOCK, currentState == MatrixState.BLOCK) {
                        scope.launch { updateState(label, area, MatrixState.BLOCK, filterManager, hiddenDrawer, globalExcluded, notifSelected, notifExcluded, widgetSelected, widgetExcluded, context) }
                        showMenuForLabel = null
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun MatrixOptionItem(state: MatrixState, isSelected: Boolean, onClick: () -> Unit) {
    val (icon, color) = when(state) {
        MatrixState.SHOW -> Icons.Default.Visibility to MaterialTheme.colorScheme.primary
        MatrixState.HIDE -> Icons.Default.Visibility to MaterialTheme.colorScheme.outline
        MatrixState.BLOCK -> Icons.Default.VisibilityOff to MaterialTheme.colorScheme.error
    }
    val text = when(state) {
        MatrixState.SHOW -> stringResource(R.string.label_matrix_legend_show)
        MatrixState.HIDE -> stringResource(R.string.label_matrix_legend_hide)
        MatrixState.BLOCK -> stringResource(R.string.label_matrix_legend_block)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text.split(":")[0], // Nur den Namen, nicht die ganze Beschreibung
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        if (isSelected) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.Visibility, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private suspend fun updateState(
    label: String,
    area: String,
    newState: MatrixState,
    filterManager: FilterManager,
    hiddenDrawer: Set<String>,
    globalExcluded: Set<String>,
    notifSelected: Set<String>,
    notifExcluded: Set<String>,
    widgetSelected: Set<String>,
    widgetExcluded: Set<String>,
    context: android.content.Context
) {
    when(area) {
        "drawer" -> {
            val newHidden = hiddenDrawer.toMutableSet()
            val newGlobal = globalExcluded.toMutableSet()
            when(newState) {
                MatrixState.SHOW -> { newHidden.remove(label); newGlobal.remove(label) }
                MatrixState.HIDE -> { newHidden.add(label); newGlobal.remove(label) }
                MatrixState.BLOCK -> { newHidden.add(label); newGlobal.add(label) }
            }
            filterManager.saveHiddenDrawerLabels(newHidden)
            filterManager.saveExcludedLabels(newGlobal)
        }
        "notif" -> {
            val newSelected = notifSelected.toMutableSet()
            val newExcluded = notifExcluded.toMutableSet()
            when(newState) {
                MatrixState.SHOW -> { newSelected.add(label); newExcluded.remove(label) }
                MatrixState.HIDE -> { newSelected.remove(label); newExcluded.remove(label) }
                MatrixState.BLOCK -> { newSelected.remove(label); newExcluded.add(label) }
            }
            filterManager.saveNotificationSelectedLabels(newSelected)
            filterManager.saveNotificationExcludedLabels(newExcluded)
        }
        "widget" -> {
            val newSelected = widgetSelected.toMutableSet()
            val newExcluded = widgetExcluded.toMutableSet()
            when(newState) {
                MatrixState.SHOW -> { newSelected.add(label); newExcluded.remove(label) }
                MatrixState.HIDE -> { newSelected.remove(label); newExcluded.remove(label) }
                MatrixState.BLOCK -> { newSelected.remove(label); newExcluded.add(label) }
            }
            filterManager.saveWidgetSelectedLabels(newSelected)
            filterManager.saveWidgetExcludedLabels(newExcluded)
            updateWidget(context)
        }
    }
}

@Composable
private fun LegendSection(isExpanded: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_matrix_legend_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    LegendItem(Icons.Default.Visibility, MaterialTheme.colorScheme.primary, stringResource(R.string.label_matrix_legend_show))
                    LegendItem(Icons.Default.Visibility, MaterialTheme.colorScheme.outline, stringResource(R.string.label_matrix_legend_hide))
                    LegendItem(Icons.Default.VisibilityOff, MaterialTheme.colorScheme.error, stringResource(R.string.label_matrix_legend_block))
                }
            }
        }
    }
}

@Composable
private fun LegendItem(icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun MatrixHeaderIcon(label: String) {
    Text(
        text = label,
        modifier = Modifier.width(64.dp),
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun LabelMatrixRow(
    label: String,
    drawerState: MatrixState,
    notifState: MatrixState,
    widgetState: MatrixState,
    onAreaClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        
        MatrixToggleButton(state = drawerState, onClick = { onAreaClick("drawer") })
        MatrixToggleButton(state = notifState, onClick = { onAreaClick("notif") })
        MatrixToggleButton(state = widgetState, onClick = { onAreaClick("widget") })
    }
}

@Composable
private fun MatrixToggleButton(state: MatrixState, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.width(64.dp)
    ) {
        val (icon, color) = when(state) {
            MatrixState.SHOW -> Icons.Default.Visibility to MaterialTheme.colorScheme.primary
            MatrixState.HIDE -> Icons.Default.Visibility to MaterialTheme.colorScheme.outline
            MatrixState.BLOCK -> Icons.Default.VisibilityOff to MaterialTheme.colorScheme.error
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
    }
}
