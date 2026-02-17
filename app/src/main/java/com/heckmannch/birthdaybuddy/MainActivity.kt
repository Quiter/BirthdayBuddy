package com.heckmannch.birthdaybuddy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy.components.BirthdayItem
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.utils.FilterManager
import com.heckmannch.birthdaybuddy.utils.fetchBirthdays
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BirthdayBuddyTheme {
                val scope = rememberCoroutineScope()
                val filterManager = remember { FilterManager(this@MainActivity) }

                var contacts by remember { mutableStateOf<List<BirthdayContact>>(emptyList()) }
                var hasPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.READ_CONTACTS
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    hasPermission = isGranted
                    if (isGranted) {
                        contacts = fetchBirthdays(context = this@MainActivity)
                    }
                }

                // Start-Prüfung für Kontakte
                LaunchedEffect(Unit) {
                    if (!hasPermission) {
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    } else {
                        contacts = fetchBirthdays(context = this@MainActivity)
                    }
                }

                // Lade die gespeicherten Labels aus dem DataStore
                val savedLabels by filterManager.selectedLabelsFlow.collectAsState(initial = null)

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {

                        if (hasPermission) {
                            // --- WICHTIG: Variablen zuerst definieren ---
                            val availableLabels = contacts.map { it.label }.toSortedSet()
                            var selectedLabels by remember { mutableStateOf(emptySet<String>()) }
                            var isMenuExpanded by remember { mutableStateOf(false) }
                            var isInitialized by remember { mutableStateOf(false) }

                            // --- JETZT folgt die Logik zum Wiederherstellen ---
                            LaunchedEffect(availableLabels, savedLabels) {
                                // Erst ausführen, wenn DataStore geantwortet hat (nicht mehr null)
                                if (savedLabels != null && !isInitialized) {
                                    if (savedLabels!!.isEmpty() && availableLabels.isNotEmpty()) {
                                        // Erster Start: Alles auswählen
                                        selectedLabels = availableLabels
                                        filterManager.saveSelectedLabels(availableLabels)
                                    } else {
                                        // Bestehende Auswahl laden (und filtern, falls Labels gelöscht wurden)
                                        selectedLabels = savedLabels!!.filter { availableLabels.contains(it) }.toSet()
                                    }
                                    isInitialized = true
                                }
                            }

                            val filteredContacts = contacts.filter { selectedLabels.contains(it.label) }
                            val sortedContacts = filteredContacts.sortedBy { it.birthday.takeLast(5) }

                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Box {
                                        Button(onClick = { isMenuExpanded = true }) {
                                            Text("Filter")
                                        }

                                        DropdownMenu(
                                            expanded = isMenuExpanded,
                                            onDismissRequest = { isMenuExpanded = false },
                                            modifier = Modifier.heightIn(max = 350.dp)
                                        ) {
                                            availableLabels.forEach { label ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Checkbox(
                                                                checked = selectedLabels.contains(label),
                                                                onCheckedChange = { isChecked ->
                                                                    val newSelection = selectedLabels.toMutableSet()
                                                                    if (isChecked) newSelection.add(label) else newSelection.remove(label)
                                                                    selectedLabels = newSelection
                                                                    scope.launch { filterManager.saveSelectedLabels(newSelection) }
                                                                }
                                                            )
                                                            Text(text = label, modifier = Modifier.padding(start = 8.dp))
                                                        }
                                                    },
                                                    onClick = {
                                                        val newSelection = selectedLabels.toMutableSet()
                                                        if (selectedLabels.contains(label)) newSelection.remove(label) else newSelection.add(label)
                                                        selectedLabels = newSelection
                                                        scope.launch { filterManager.saveSelectedLabels(newSelection) }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                                ) {
                                    items(sortedContacts) { contact ->
                                        BirthdayItem(contact = contact)
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "Wir brauchen die Erlaubnis, um Geburtstage anzuzeigen.")
                            }
                        }
                    }
                }
            }
        }
    }
}