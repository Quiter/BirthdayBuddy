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
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme


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
                        contacts = fetchBirthdays()
                    }
                }

                // 3. Start-Prüfung (Hintergrund-Task)
                LaunchedEffect(Unit) {
                    if (!hasPermission) {
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    } else {
                        contacts = fetchBirthdays()
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
                                                                val newSelection = selectedLabels.toMutableSet()
                                                                if (isChecked) newSelection.add(label) else newSelection.remove(label)
                                                                selectedLabels = newSelection
                                                            }
                                                        )
                                                        Text(text = label, modifier = Modifier.padding(start = 8.dp))
                                                    }
                                                },
                                                onClick = {
                                                    val newSelection = selectedLabels.toMutableSet()
                                                    if (selectedLabels.contains(label)) newSelection.remove(label) else newSelection.add(label)
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
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Wir brauchen die Erlaubnis, um Geburtstage anzuzeigen.")
                        }
                    }
                }
            }
        }
    }
    private fun fetchBirthdays(): List<BirthdayContact> {
        val contactList = mutableListOf<BirthdayContact>()

        // Die Projektion definiert, welche Spalten wir aus der Datenbank abfragen
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Event.CONTACT_ID,
            ContactsContract.CommonDataKinds.Event.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Event.START_DATE,
            ContactsContract.CommonDataKinds.Event.LABEL,
            ContactsContract.CommonDataKinds.Event.TYPE
        )

        // Filter: Wir wollen nur Daten vom Typ "Geburtstag"
        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ${ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY}"
        val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)

        // Die eigentliche Abfrage an das System
        val cursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        // Cursor durchlaufen und Daten in unsere Liste schreiben
        cursor?.use {
            // Wir brauchen jetzt auch die ID des Kontakts, um danach suchen zu können
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Event.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Event.DISPLAY_NAME)
            val birthdayIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE)

            while (it.moveToNext()) {
                val contactId = it.getString(idIndex) ?: ""
                val name = it.getString(nameIndex) ?: "Unbekannt"
                val bday = it.getString(birthdayIndex) ?: ""

                // HIER IST DIE MAGIE: Wir holen die echten Label!
                val label = getContactLabels(contactId)

                val (age, remainingDays) = calculateAgeAndDays(bday)
                contactList.add(BirthdayContact(name, bday, label, remainingDays, age))
            }
        }
        return contactList
    }


    // Hilfsfunktion zum Berechnen von Alter und Resttagen
    private fun calculateAgeAndDays(birthDateString: String): Pair<Int, Int> {
        if (birthDateString.isEmpty()) return Pair(0, 0)

        return try {
            val today = java.time.LocalDate.now()

            // Fall 1: Kontakt hat KEIN Jahr hinterlegt (z.B. "--05-25")
            if (birthDateString.startsWith("--")) {
                val month = birthDateString.substring(2, 4).toInt()
                val day = birthDateString.substring(5, 7).toInt()

                var nextBday = java.time.LocalDate.of(today.year, month, day)
                if (nextBday.isBefore(today)) {
                    nextBday = nextBday.plusYears(1)
                }

                val days = java.time.temporal.ChronoUnit.DAYS.between(today, nextBday).toInt()
                return Pair(0, days)
            }
            // Fall 2: Normales Datum mit Jahr (z.B. "1992-05-25")
            else {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val birthDate = java.time.LocalDate.parse(birthDateString, formatter)

                // Das Alter, das die Person in DIESEM Kalenderjahr erreicht
                var turnsAge = today.year - birthDate.year
                var nextBday = birthDate.withYear(today.year)

                // Wenn der Geburtstag dieses Jahr schon vorbei ist...
                if (nextBday.isBefore(today)) {
                    nextBday = nextBday.plusYears(1) // ...ist der nächste im nächsten Jahr
                    turnsAge += 1                    // ...und sie wird noch ein Jahr älter!
                }

                val days = java.time.temporal.ChronoUnit.DAYS.between(today, nextBday).toInt()
                return Pair(turnsAge, days)
            }
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }

    // Neue Hilfsfunktion: Holt die echten Kontakt-Gruppen (Label) aus der Datenbank
    private fun getContactLabels(contactId: String): String {
        val groupIds = mutableListOf<String>()

        // 1. Welche Gruppen-IDs hat dieser Kontakt?
        val groupCursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE),
            null
        )

        groupCursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)
            while (it.moveToNext()) {
                val groupId = it.getString(idIndex)
                if (groupId != null) groupIds.add(groupId)
            }
        }

        // Wenn er in keiner Gruppe ist
        if (groupIds.isEmpty()) return "Ohne Label"

        // 2. Wie heißen diese Gruppen im Klartext?
        val labels = mutableListOf<String>()
        val placeholders = groupIds.joinToString(",") { "?" }

        val titleCursor = contentResolver.query(
            ContactsContract.Groups.CONTENT_URI,
            arrayOf(ContactsContract.Groups.TITLE),
            "${ContactsContract.Groups._ID} IN ($placeholders)",
            groupIds.toTypedArray(),
            null
        )

        titleCursor?.use {
            val titleIndex = it.getColumnIndex(ContactsContract.Groups.TITLE)
            while (it.moveToNext()) {
                // Manchmal heißen die Gruppen im System "System Group: My Contacts"
                // Wir schneiden das "System Group: " einfach weg, damit es schöner aussieht
                val title = it.getString(titleIndex)?.replace("System Group: ", "")
                if (!title.isNullOrBlank()) {
                    labels.add(title)
                }
            }
        }

        return if (labels.isEmpty()) "Ohne Label" else labels.joinToString(", ")
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

@Composable
fun BirthdayItem(contact: BirthdayContact) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically // Zentriert beide Spalten schön mittig
        ) {
            // Linke Spalte (bekommt das "weight", damit sie sich anpasst)
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 2, // Erlaubt maximal 2 Zeilen für sehr lange Namen
                    overflow = TextOverflow.Ellipsis // Macht "..." am Ende, wenn es nicht passt
                )
                Text(text = "Datum: ${formatBirthdayGerman(contact.birthday)}", fontSize = 14.sp)
            }

            // Rechte Spalte (ohne weight, nimmt exakt den Platz, den sie braucht)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Wird ${contact.age}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "in ${contact.remainingDays} Tagen")
            }
        }
    }
}

data class BirthdayContact(
    val name: String,
    val birthday: String, // Später wandeln wir das in ein echtes Datum um
    val label: String,    // Für deine Kategorien (Privat, Arbeit, etc.)
    val remainingDays: Int,
    val age: Int
)

// Hilfsfunktion: Macht aus "1990-05-25" ein schönes "25.05.1990"
fun formatBirthdayGerman(dateString: String): String {
    if (dateString.isEmpty()) return "Unbekannt"

    return try {
        if (dateString.startsWith("--")) {
            // Fall 1: Kein Jahr bekannt (z.B. "--05-25")
            val month = dateString.substring(2, 4)
            val day = dateString.substring(5, 7)
            "$day.$month."
        } else {
            // Fall 2: Normales Datum (z.B. "1990-05-25")
            val parts = dateString.split("-")
            if (parts.size == 3) {
                "${parts[2]}.${parts[1]}.${parts[0]}" // Tag.Monat.Jahr
            } else {
                dateString
            }
        }
    } catch (e: Exception) {
        dateString // Falls das Format ganz komisch ist, geben wir es einfach unformatiert zurück
    }
}