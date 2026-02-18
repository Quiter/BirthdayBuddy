package com.heckmannch.birthdaybuddy.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.model.BirthdayContact

/**
 * Die Haupt-Suchleiste der App.
 * Ermöglicht das Filtern der Kontaktliste nach Namen.
 * 
 * @param query Der aktuelle Suchtext.
 * @param onQueryChange Callback, wenn sich der Suchtext ändert.
 * @param onMenuClick Callback für das Hamburger-Menü-Icon (öffnet den Drawer).
 * @param modifier Zusätzliche Modifier für das Layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { }, // Suche wird live beim Tippen durchgeführt
                expanded = false,
                onExpandedChange = { },
                placeholder = { Text("In Kontakten suchen...") },
                leadingIcon = {
                    // Button zum Öffnen des seitlichen Menüs
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menü öffnen")
                    }
                },
                trailingIcon = {
                    // "X" Button zum Löschen der Suche, nur sichtbar wenn Text vorhanden
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Suche löschen")
                        }
                    }
                }
            )
        },
        expanded = false,
        onExpandedChange = { },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
    ) { }
}

/**
 * Inhalt des seitlichen Navigationsmenüs (Navigation Drawer).
 * Enthält Label-Filter, die Option zum Neuladen der Kontakte und den Link zu den Einstellungen.
 * 
 * @param availableLabels Alle in den Kontakten gefundenen Labels/Gruppen.
 * @param selectedLabels Die aktuell für die Anzeige ausgewählten Labels.
 * @param hiddenDrawerLabels Labels, die der Nutzer in den Einstellungen komplett ausgeblendet hat.
 * @param onLabelToggle Callback zum (De-)Aktivieren eines Filters.
 * @param onReloadContacts Funktion zum manuellen Synchronisieren der Kontakte.
 * @param onSettingsClick Navigations-Callback zu den Einstellungen.
 */
@Composable
fun MainDrawerContent(
    availableLabels: Set<String>,
    selectedLabels: Set<String>,
    hiddenDrawerLabels: Set<String>,
    onLabelToggle: (String, Boolean) -> Unit,
    onReloadContacts: () -> Unit,
    onSettingsClick: () -> Unit
) {
    ModalDrawerSheet {
        Spacer(Modifier.height(12.dp))
        Text(
            "Labels filtern",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
        )

        // Filtere Labels heraus, die in den Einstellungen als "versteckt" markiert wurden
        val labelsToShow = availableLabels.filterNot { hiddenDrawerLabels.contains(it) }

        // Liste der klickbaren Label-Einträge mit Checkbox
        labelsToShow.forEach { label ->
            val isChecked = selectedLabels.contains(label)
            NavigationDrawerItem(
                label = { Text(label) },
                selected = false,
                onClick = { onLabelToggle(label, isChecked) },
                icon = { Checkbox(checked = isChecked, onCheckedChange = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 28.dp))

        // Button zum manuellen Triggern des Kontakt-Imports
        NavigationDrawerItem(
            label = { Text("Kontakte neu laden") },
            selected = false,
            onClick = onReloadContacts,
            icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        // Button zur Einstellungsseite
        NavigationDrawerItem(
            label = { Text("Einstellungen") },
            selected = false,
            onClick = onSettingsClick,
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}

/**
 * Eine performante Liste zur Anzeige der Geburtstagskontakte.
 * 
 * @param contacts Die Liste der anzuzeigenden Kontakte.
 * @param listState Der Scroll-Status der Liste.
 * @param modifier Zusätzliche Layout-Optionen.
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
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Items mit stabilem Key für bessere Performance und Animationen
        items(
            items = contacts,
            key = { it.name + it.birthday }
        ) { contact ->
            BirthdayItem(
                contact = contact,
                modifier = Modifier.animateItem()
            )
        }
    }
}
