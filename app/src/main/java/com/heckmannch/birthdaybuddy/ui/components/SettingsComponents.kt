package com.heckmannch.birthdaybuddy.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        content = content
    )
}

/**
 * Eine komplette Einstellungs-Kachel bestehend aus einer Card und einer Zeile.
 */
@Composable
fun SettingsBlock(
    title: String, 
    subtitle: String, 
    icon: ImageVector, 
    iconColor: Color = Color.White,
    containerColor: Color = MaterialTheme.colorScheme.primary, 
    onClick: () -> Unit
) {
    SettingsCard {
        SettingsBlockRow(title, subtitle, icon, iconColor, containerColor, onClick = onClick)
    }
}

/**
 * Ein standardisierter Listeneintrag für die Einstellungen.
 */
@Composable
fun SettingsBlockRow(
    title: String, 
    subtitle: String, 
    icon: ImageVector, 
    iconColor: Color = Color.White,
    containerColor: Color = MaterialTheme.colorScheme.primary, 
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodyMedium) },
        leadingContent = {
            Box(
                modifier = Modifier.size(44.dp).background(containerColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(24.dp), tint = iconColor)
            }
        },
        trailingContent = { 
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline) 
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun SettingsFooter(versionName: String, onGithubClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Version $versionName", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onGithubClick) {
            Icon(painter = painterResource(id = R.drawable.ic_github), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Projekt auf GitHub")
        }
    }
}

@Composable
fun WidgetCountDialog(currentCount: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    var selectedValue by remember { mutableIntStateOf(currentCount) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Widget Kapazität", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Wie viele Personen sollen maximal angezeigt werden?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                WheelPicker(
                    range = (1..6).toList(),
                    initialValue = selectedValue,
                    onValueChange = { selectedValue = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selectedValue) }) {
                Text("Speichern", fontWeight = FontWeight.Bold)
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(items = availableLabels.toList()) { label ->
                        ListItem(
                            headlineContent = { Text(label) },
                            trailingContent = { Switch(checked = activeLabels.contains(label), onCheckedChange = { onToggle(label, it) }) }
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
    val itemHeight = 48.dp
    
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centeredIndex = listState.firstVisibleItemIndex
            if (centeredIndex in range.indices) onValueChange(range[centeredIndex])
        }
    }

    Box(modifier = Modifier.width(120.dp).height(itemHeight * 3), contentAlignment = Alignment.Center) {
        // Auswahl-Indikator
        Surface(
            modifier = Modifier.fillMaxWidth().height(itemHeight),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
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
                        style = MaterialTheme.typography.headlineMedium, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
