package com.heckmannch.birthdaybuddy.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.model.BirthdayContact

/**
 * Diese Datei enthält die zentralen UI-Komponenten des Hauptbildschirms.
 * Hier wird das Layout für die Suchleiste, das Seitenmenü (Drawer) und die Liste definiert.
 */

// Interne Konstanten für die Erkennung von System-Gruppen aus dem Android-Telefonbuch.
private const val SYSTEM_LABEL_ALL = "My Contacts"
private const val SYSTEM_LABEL_STARRED = "Starred in Android"

/**
 * Die Suchleiste am oberen Bildschirmrand.
 * Sie ermöglicht das Filtern der Kontaktliste in Echtzeit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSearchBar(
    query: String,                  // Der aktuelle Text in der Suche
    onQueryChange: (String) -> Unit, // Callback, wenn sich der Text ändert
    onMenuClick: () -> Unit,        // Öffnet das Seitenmenü
    modifier: Modifier = Modifier
) {
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { }, // Wir suchen live, daher ist kein "Enter"-Event nötig
                expanded = false,
                onExpandedChange = { },
                placeholder = { 
                    Text(
                        stringResource(R.string.main_search_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ) 
                },
                leadingIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            Icons.Default.Menu, 
                            contentDescription = stringResource(R.string.drawer_menu_open),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                trailingIcon = {
                    // Das "X" zum Löschen erscheint nur, wenn Text eingegeben wurde
                    AnimatedVisibility(
                        visible = query.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.drawer_search_clear))
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )
        },
        expanded = false,
        onExpandedChange = { },
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding() // Sorgt dafür, dass die Leiste unter der Statusleiste (Akku, Uhr) beginnt
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = SearchBarDefaults.inputFieldShape,
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) { }
}

/**
 * Der Inhalt des seitlichen Menüs (Navigation Drawer).
 * Erlaubt das Filtern nach Labels (Kategorien) und den schnellen Zugriff auf Apps.
 */
@Composable
fun MainDrawerContent(
    availableLabels: Set<String>,      // Alle gefundenen Labels aus den Kontakten
    selectedLabels: Set<String>,       // Die aktuell aktiven Filter
    hiddenDrawerLabels: Set<String>,   // Labels, die der Nutzer in den Einstellungen ausgeblendet hat
    onLabelToggle: (String, Boolean) -> Unit, // Aktion beim Klick auf ein Label
    onSettingsClick: () -> Unit        // Navigiert zu den Einstellungen
) {
    val context = LocalContext.current
    val allLabel = stringResource(R.string.label_all)
    val favoritesLabel = stringResource(R.string.label_favorites)
    
    // Merkt sich, ob die Label-Sektion ausgeklappt ist
    var labelsExpanded by rememberSaveable { mutableStateOf(true) }

    // Ein bunter Verlauf für den Header (wirkt freundlicher/moderner)
    val kidColors = listOf(Color(0xFF4285F4), Color(0xFFF06292), Color(0xFFFFB300), Color(0xFF4CAF50))
    val headerBrush = Brush.linearGradient(kidColors)

    // Sortiert die Labels: Erst "Alle", dann "Favoriten", dann den Rest alphabetisch
    val sortedLabels = remember(availableLabels, hiddenDrawerLabels) {
        availableLabels
            .filterNot { hiddenDrawerLabels.contains(it) }
            .sortedWith(compareBy<String> {
                when (it) {
                    SYSTEM_LABEL_ALL -> 0
                    SYSTEM_LABEL_STARRED -> 1
                    else -> 2
                }
            }.thenBy { it })
    }

    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
    ) {
        // HEADER BEREICH mit App-Titel und Einstellungen-Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBrush)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 8.dp, top = 48.dp, bottom = 16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.drawer_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 32.sp,
                    modifier = Modifier.weight(1f).padding(bottom = 4.dp)
                )
                
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings, 
                        contentDescription = stringResource(R.string.drawer_settings), 
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // SCROLLBARE LISTE im Menü
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Schnellzugriff auf externe Apps
            item {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_calendar)) },
                    selected = false,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).setData(Uri.parse("content://com.android.calendar/time/"))
                        context.startActivity(intent)
                    },
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.drawer_calendar)) },
                    modifier = Modifier.padding(NavigationDrawerItemPadding)
                )
                
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_contacts)) },
                    selected = false,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
                        context.startActivity(intent)
                    },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = stringResource(R.string.drawer_contacts)) },
                    modifier = Modifier.padding(NavigationDrawerItemPadding)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            }

            // LABEL-SEKTION (Überschrift mit Ausklapp-Funktion)
            item {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { 
                        Text(
                            stringResource(R.string.drawer_labels_header).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ) 
                    },
                    trailingContent = {
                        Icon(
                            imageVector = if (labelsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { labelsExpanded = !labelsExpanded }
                )
            }

            // Die einzelnen Label-Einträge
            if (labelsExpanded) {
                items(sortedLabels) { label ->
                    val isChecked = selectedLabels.contains(label)
                    // Mapping der technischen Label-Namen auf lesbare Texte und Icons
                    val (displayText, icon) = when(label) {
                        SYSTEM_LABEL_ALL -> allLabel to if (isChecked) Icons.Default.People else Icons.Default.PeopleOutline
                        SYSTEM_LABEL_STARRED -> favoritesLabel to if (isChecked) Icons.Default.Star else Icons.Default.StarOutline
                        else -> label to if (isChecked) Icons.AutoMirrored.Filled.Label else Icons.AutoMirrored.Outlined.Label
                    }

                    NavigationDrawerItem(
                        label = { 
                            Text(
                                text = displayText,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal 
                            ) 
                        },
                        selected = isChecked,
                        onClick = { onLabelToggle(label, isChecked) },
                        icon = { Icon(icon, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color.Transparent,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

private val NavigationDrawerItemPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)

/**
 * Die Hauptliste der Geburtstage.
 */
@Composable
fun BirthdayList(
    contacts: List<BirthdayContact>,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            // Extra Abstand unten, damit das FAB (Floating Action Button) nichts verdeckt
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp 
        )
    ) {
        items(
            items = contacts,
            key = { "${it.id}_${it.name}" } // Eindeutiger Schlüssel für flüssige Animationen
        ) { contact ->
            BirthdayItem(
                contact = contact,
                modifier = Modifier.animateItem() // Sorgt für sanftes Verschieben beim Filtern
            )
        }
    }
}

/**
 * Wird angezeigt, wenn keine Kontakte gefunden wurden (z.B. durch einen Filter).
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
