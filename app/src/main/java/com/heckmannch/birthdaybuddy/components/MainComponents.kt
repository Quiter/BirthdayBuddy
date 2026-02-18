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
                onSearch = { },
                expanded = false,
                onExpandedChange = { },
                placeholder = { Text("In Kontakten suchen...") },
                leadingIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menü öffnen")
                    }
                },
                trailingIcon = {
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
        // LazyColumn ist robuster für lange Listen im Drawer
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 32.dp) // Extra Platz unten
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Labels filtern",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                )
            }

            val labelsToShow = availableLabels.filterNot { hiddenDrawerLabels.contains(it) }

            items(labelsToShow) { label ->
                val isChecked = selectedLabels.contains(label)
                NavigationDrawerItem(
                    label = { Text(label) },
                    selected = false,
                    onClick = { onLabelToggle(label, isChecked) },
                    icon = { Checkbox(checked = isChecked, onCheckedChange = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 28.dp))
                
                NavigationDrawerItem(
                    label = { Text("Kontakte neu laden") },
                    selected = false,
                    onClick = onReloadContacts,
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Einstellungen") },
                    selected = false,
                    onClick = onSettingsClick,
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    }
}

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
        items(
            items = contacts,
            key = { "${it.id}_${it.name}" }
        ) { contact ->
            BirthdayItem(
                contact = contact,
                modifier = Modifier.animateItem()
            )
        }
    }
}
