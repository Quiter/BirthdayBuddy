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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.*

/**
 * Diese Datei enthält wiederverwendbare UI-Elemente für die Einstellungs-Bildschirme.
 * Das Design orientiert sich an modernen "Pill"-Layouts (abgerundete Blöcke).
 */

/**
 * Eine einfache Überschrift für verschiedene Einstellungs-Sektionen.
 * Wird meist in Großbuchstaben und mit etwas Abstand angezeigt.
 */
@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp, start = 16.dp)
    )
}

/**
 * Ein Container, der mehrere Einstellungseinträge optisch gruppiert.
 * Er sorgt dafür, dass die Ecken der gesamten Gruppe abgerundet sind.
 */
@Composable
fun SettingsGroup(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
    ) {
        content()
    }
}

/**
 * Ein einzelner Eintrag (Zeile) in den Einstellungen.
 * 
 * @param title Haupttext (z.B. "Benachrichtigungen")
 * @param subtitle Kleinerer Text darunter (Beschreibung)
 * @param icon Das Icon auf der linken Seite
 * @param iconContainerColor Hintergrundfarbe des kleinen Icon-Kreises
 * @param isTop Wenn true, werden die oberen Ecken abgerundet (für den ersten Eintrag im Block)
 * @param isBottom Wenn true, werden die unteren Ecken abgerundet (für den letzten Eintrag im Block)
 * @param showArrow Zeigt den Pfeil nach rechts an (Standard: true)
 */
@Composable
fun SettingsBlockRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color = Color.White,
    isTop: Boolean = false,
    isBottom: Boolean = false,
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    // Erkennt automatisch, ob der DarkMode aktiv ist, um die Hintergrundfarbe anzupassen
    val isDark = LocalThemeIsDark.current
    val backgroundColor = if (isDark) SettingsPillBackgroundDark else SettingsPillBackgroundLight
    
    // Logik für die Abrundung: Nur oben, nur unten, beides (Pille) oder gar nicht (Mitte eines Blocks)
    val shape = when {
        isTop && isBottom -> RoundedCornerShape(28.dp)
        isTop -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        isBottom -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
        else -> RoundedCornerShape(4.dp)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp) // Kleiner Spalt zwischen den Zeilen
            .clip(shape)
            .clickable { onClick() },
        color = backgroundColor,
        shape = shape
    ) {
        ListItem(
            headlineContent = { 
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium) 
            },
            supportingContent = { 
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) 
            },
            leadingContent = {
                // Das Icon steckt in einem kleinen farbigen Kreis
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = iconContainerColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = iconTint
                        )
                    }
                }
            },
            trailingContent = if (showArrow) {
                {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            } else null,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

/**
 * Der Fußbereich der Einstellungen.
 * Zeigt den App-Namen, die Version und einen Button zum GitHub-Projekt.
 */
@Composable
fun SettingsFooter(versionName: String, onGithubClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name), 
            style = MaterialTheme.typography.titleSmall, 
            color = MaterialTheme.colorScheme.primary, 
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.version_label, versionName), 
            style = MaterialTheme.typography.labelMedium, 
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Button mit dem GitHub-Icon
        FilledTonalButton(
            onClick = onGithubClick,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_github), 
                contentDescription = null, 
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.settings_github_project), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Ein "Zahlenrad" (Wheel Picker), wie man es von iOS-Weckern kennt.
 * Wird in dieser App zur Auswahl der Uhrzeit für Benachrichtigungen genutzt.
 * 
 * @param range Der Zahlenbereich (z.B. 0..23 für Stunden)
 * @param initialValue Der Wert, der am Anfang in der Mitte stehen soll
 * @param onValueChange Callback, wenn ein neuer Wert in der Mitte einrastet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelPicker(range: List<Int>, initialValue: Int, onValueChange: (Int) -> Unit) {
    // Initialer Index in der Liste finden
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = range.indexOf(initialValue).coerceAtLeast(0))
    // Sorgt dafür, dass die Liste immer exakt auf einem Element "einrastet" (Snapping)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val itemHeight = 56.dp
    
    // Überwacht das Ende des Scroll-Vorgangs, um den neuen Wert zurückzugeben
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centeredIndex = listState.firstVisibleItemIndex
            if (centeredIndex in range.indices) onValueChange(range[centeredIndex])
        }
    }

    Box(
        modifier = Modifier.width(100.dp).height(itemHeight * 3), 
        contentAlignment = Alignment.Center
    ) {
        // Der optische Auswahlrahmen in der Mitte
        Surface(
            modifier = Modifier.fillMaxWidth().height(itemHeight),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {}
        
        // Die scrollbare Liste mit den Zahlen
        LazyColumn(
            state = listState, 
            flingBehavior = flingBehavior, 
            contentPadding = PaddingValues(vertical = itemHeight), // Puffer oben/unten für Zentrierung
            modifier = Modifier.fillMaxSize(), 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items = range) { value ->
                Box(
                    modifier = Modifier.height(itemHeight).fillMaxWidth(), 
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value.toString().padStart(2, '0'), // Füllt einstellige Zahlen mit einer Null auf (09 statt 9)
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
