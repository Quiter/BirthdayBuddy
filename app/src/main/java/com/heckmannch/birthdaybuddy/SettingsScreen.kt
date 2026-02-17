package com.heckmannch.birthdaybuddy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.utils.FilterManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    filterManager: FilterManager,
    availableLabels: Set<String>,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // Beide Listen aus dem Speicher laden
    val excludedLabels by filterManager.excludedLabelsFlow.collectAsState(initial = emptySet())
    val hiddenDrawerLabels by filterManager.hiddenDrawerLabelsFlow.collectAsState(initial = emptySet())

    // Zustand für die Reiter (Tabs)
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Kontakte blockieren", "Menü aufräumen")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding()
                )
        ) {

            // Die Reiter (Tabs) zum Umschalten
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    // Die Überschrift ändert sich je nach Tab
                    val title = if (selectedTabIndex == 0) "Labels komplett ausschließen" else "Labels im Drawer verstecken"
                    val desc = if (selectedTabIndex == 0)
                        "Kontakte mit diesen Labels werden ignoriert und nirgendwo angezeigt."
                    else
                        "Diese Labels tauchen im linken Filter-Menü nicht mehr auf, die Kontakte bleiben aber erhalten."

                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))

                    // HIER WAR DER FEHLER: horizontal und bottom dürfen nicht gemischt werden!
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    )
                }

                items(availableLabels.toList()) { label ->
                    // Wir prüfen je nach Tab, welcher Schalter umgelegt sein soll
                    val isChecked = if (selectedTabIndex == 0) excludedLabels.contains(label) else hiddenDrawerLabels.contains(label)

                    ListItem(
                        headlineContent = { Text(label) },
                        trailingContent = {
                            Switch(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (selectedTabIndex == 0) {
                                        val newSet = excludedLabels.toMutableSet()
                                        if (checked) newSet.add(label) else newSet.remove(label)
                                        scope.launch { filterManager.saveExcludedLabels(newSet) }
                                    } else {
                                        val newSet = hiddenDrawerLabels.toMutableSet()
                                        if (checked) newSet.add(label) else newSet.remove(label)
                                        scope.launch { filterManager.saveHiddenDrawerLabels(newSet) }
                                    }
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}