package com.heckmannch.birthdaybuddy.components

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

/**
 * Zeigt eine Überschrift für einen Einstellungsbereich an.
 * @param title Der Text der Überschrift.
 */
@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

/**
 * Ein Basis-Container (Card) für Einstellungseinträge.
 * @param content Die UI-Elemente, die innerhalb der Card angezeigt werden sollen.
 */
@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        content = content
    )
}

/**
 * Eine komplette Einstellungs-Kachel bestehend aus einer Card und einer Zeile.
 * Kombiniert [SettingsCard] und [SettingsBlockRow].
 */
@Composable
fun SettingsBlock(title: String, subtitle: String, icon: ImageVector, containerColor: Color, onClick: () -> Unit) {
    SettingsCard {
        SettingsBlockRow(title, subtitle, icon, containerColor, onClick = onClick)
    }
}

/**
 * Ein standardisierter Listeneintrag für die Einstellungen.
 * Enthält ein Icon links, Text in der Mitte und einen Pfeil rechts.
 * 
 * @param title Haupttitel der Einstellung.
 * @param subtitle Zusätzliche Informationen oder aktueller Wert.
 * @param icon Das anzuzeigende Icon.
 * @param containerColor Hintergrundfarbe des Icons.
 * @param onClick Aktion, die beim Klicken ausgeführt wird.
 */
@Composable
fun SettingsBlockRow(title: String, subtitle: String, icon: ImageVector, containerColor: Color, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            // Kreisrunder Hintergrund für das Icon
            Box(
                modifier = Modifier.size(40.dp).background(containerColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(20.dp), tint = Color.White)
            }
        },
        trailingContent = { 
            // Standard "Weiter"-Pfeil
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline) 
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

/**
 * Fußzeile der Einstellungsseite.
 * Zeigt die App-Version und einen Link zum GitHub-Repository.
 */
@Composable
fun SettingsFooter(versionName: String, onGithubClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
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

/**
 * Dialog zur Auswahl der Anzahl der Personen, die im Widget angezeigt werden sollen.
 */
@Composable
fun WidgetCountDialog(currentCount: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Widget Kapazität") },
        text = {
            Column {
                // Auswahlmöglichkeiten 1, 2 oder 3 Personen
                listOf(1, 2, 3).forEach { count ->
                    ListItem(
                        headlineContent = { Text("$count ${if (count == 1) "Person" else "Personen"}") },
                        leadingContent = { RadioButton(selected = currentCount == count, onClick = null) },
                        modifier = Modifier.clickable { onSelect(count) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fertig") } }
    )
}

/**
 * Ein separater Bildschirm zur Auswahl (Aktivierung/Deaktivierung) von Labels.
 * Wird z.B. genutzt, um zu filtern, welche Geburtsstage im Widget erscheinen.
 */
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
                            trailingContent = { 
                                // Switch zum An-/Ausschalten des Labels
                                Switch(checked = activeLabels.contains(label), onCheckedChange = { onToggle(label, it) }) 
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ein modernes "Drehrad" (Wheel Picker) zur Auswahl von Zahlenwerten.
 * Nutzt Snapping, damit das Rad immer auf einem Wert einrastet.
 * 
 * @param range Der Zahlenbereich (z.B. 0..23 für Stunden).
 * @param initialValue Der am Anfang ausgewählte Wert.
 * @param onValueChange Callback, wenn ein neuer Wert fixiert wird.
 */
@Composable
fun WheelPicker(
    range: List<Int>,
    initialValue: Int,
    onValueChange: (Int) -> Unit
) {
    // State für die Liste und das Snapping-Verhalten
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = range.indexOf(initialValue).coerceAtLeast(0))
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val itemHeight = 48.dp
    
    // Überwacht den Scroll-Status und meldet den Wert zurück, wenn das Scrollen stoppt
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centeredIndex = listState.firstVisibleItemIndex
            if (centeredIndex in range.indices) {
                onValueChange(range[centeredIndex])
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight * 3), // Zeigt 3 Zeilen an (eine oben, eine mitte, eine unten)
        contentAlignment = Alignment.Center
    ) {
        // Markierung des mittleren (ausgewählten) Bereichs
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight), // Padding, damit das erste/letzte Element mittig sein kann
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items = range) { value ->
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
