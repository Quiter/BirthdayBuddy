package com.heckmannch.birthdaybuddy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy.components.BirthdayItem
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.utils.fetchBirthdays


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BirthdayBuddyTheme {
                // 1. Zustände (States) ganz oben
                var contacts by remember { mutableStateOf<List<BirthdayContact>>(emptyList()) }
                var hasPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.READ_CONTACTS
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                // 2. Erlaubnis-Abfrage
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    hasPermission = isGranted
                    if (isGranted) {
                        contacts = fetchBirthdays(context = this@MainActivity)
                    }
                }

                // 3. Start-Prüfung (Hintergrund-Task)
                LaunchedEffect(Unit) {
                    if (!hasPermission) {
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    } else {
                        contacts = fetchBirthdays(context = this@MainActivity)
                    }
                }

                // --- HIER BEGINNT DIE OBERFLÄCHE (UI) ---
                Surface(
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (hasPermission) {
                        // Daten für den Filter vorbereiten
                        val availableLabels = contacts.map { it.label }.toSortedSet()
                        var selectedLabels by remember { mutableStateOf(emptySet<String>()) }
                        var isMenuExpanded by remember { mutableStateOf(false) }

                        // Wenn Labels geladen wurden, beim Start alle anhaken
                        LaunchedEffect(availableLabels) {
                            if (selectedLabels.isEmpty() && availableLabels.isNotEmpty()) {
                                selectedLabels = availableLabels
                            }
                        }

                        // Liste filtern und sortieren
                        val filteredContacts = contacts.filter { selectedLabels.contains(it.label) }
                        val sortedContacts = filteredContacts.sortedBy { it.birthday.takeLast(5) }

                        // Das eigentliche Layout (Spalte mit Menü oben und Liste unten)
                        Column(modifier = Modifier.fillMaxSize()) {

                            // Filter-Menüleiste
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
                                        // NEU: Begrenzt die Höhe, damit das Menü scrollbar bleibt und nicht über den Rand schießt
                                        modifier = Modifier.heightIn(max = 350.dp)
                                    ) {
                                        availableLabels.forEach { label ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Checkbox(
                                                            checked = selectedLabels.contains(label),
                                                            onCheckedChange = { isChecked ->
                                                                val newSelection =
                                                                    selectedLabels.toMutableSet()
                                                                if (isChecked) newSelection.add(
                                                                    label
                                                                ) else newSelection.remove(label)
                                                                selectedLabels = newSelection
                                                            }
                                                        )
                                                        Text(
                                                            text = label,
                                                            modifier = Modifier.padding(start = 8.dp)
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    val newSelection = selectedLabels.toMutableSet()
                                                    if (selectedLabels.contains(label)) newSelection.remove(
                                                        label
                                                    ) else newSelection.add(label)
                                                    selectedLabels = newSelection
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Die Liste
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
                        // Ansicht, wenn keine Erlaubnis erteilt wurde
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Wir brauchen die Erlaubnis, um Geburtstage anzuzeigen.")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BirthdayBuddyTheme {
        Greeting("Android")
    }
}