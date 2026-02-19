package com.heckmannch.birthdaybuddy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 16.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        content = content
    )
}

@Composable
fun SettingsBlock(
    title: String, 
    subtitle: String, 
    icon: ImageVector, 
    iconColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), 
    onClick: () -> Unit
) {
    SettingsCard {
        SettingsBlockRow(title, subtitle, icon, iconColor, containerColor, onClick = onClick)
    }
}

@Composable
fun SettingsBlockRow(
    title: String, 
    subtitle: String, 
    icon: ImageVector, 
    iconColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), 
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clip(RoundedCornerShape(24.dp)).clickable { onClick() },
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = containerColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(22.dp), tint = iconColor)
                }
            }
        },
        trailingContent = { 
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outlineVariant) 
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun SettingsFooter(versionName: String, onGithubClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Birthday Buddy", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text("Version $versionName", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(
            onClick = onGithubClick,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_github), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("GitHub Projekt", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun WidgetCountDialog(currentCount: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    var selectedValue by remember { mutableIntStateOf(currentCount) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Widget Kapazität", 
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Maximale Anzahl der Personen im Widget.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                WheelPicker(
                    range = (1..10).toList(),
                    initialValue = selectedValue,
                    onValueChange = { selectedValue = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSelect(selectedValue) }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelSelectionScreen(
    title: String, 
    description: String, 
    availableLabels: Set<String>, 
    activeLabels: Set<String>, 
    isLoading: Boolean, 
    onToggle: (String, Boolean) -> Unit, 
    onBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(title) },
                navigationIcon = { 
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") 
                    } 
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    description, 
                    style = MaterialTheme.typography.bodyMedium, 
                    modifier = Modifier.padding(16.dp), 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                    CircularProgressIndicator() 
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(items = availableLabels.toList()) { label ->
                        val isChecked = activeLabels.contains(label)
                        ListItem(
                            modifier = Modifier.clickable { onToggle(label, !isChecked) },
                            headlineContent = { Text(label, fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal) },
                            trailingContent = { 
                                Switch(
                                    checked = isChecked, 
                                    onCheckedChange = { onToggle(label, it) },
                                    thumbContent = if (isChecked) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        }
                                    } else null
                                ) 
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f) else Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelPicker(range: List<Int>, initialValue: Int, onValueChange: (Int) -> Unit) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = range.indexOf(initialValue).coerceAtLeast(0))
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val itemHeight = 56.dp
    
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centeredIndex = listState.firstVisibleItemIndex
            if (centeredIndex in range.indices) onValueChange(range[centeredIndex])
        }
    }

    Box(modifier = Modifier.width(140.dp).height(itemHeight * 3), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(itemHeight),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {}
        
        LazyColumn(
            state = listState, 
            flingBehavior = flingBehavior, 
            contentPadding = PaddingValues(vertical = itemHeight), 
            modifier = Modifier.fillMaxSize(), 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items = range) { value ->
                Box(modifier = Modifier.height(itemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = value.toString(), 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
