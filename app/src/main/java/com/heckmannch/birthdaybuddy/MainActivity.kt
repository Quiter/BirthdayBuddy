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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BirthdayBuddyTheme {
                // 1. Wir merken uns die Kontakte in einem "Zustand" (State)
                var contacts by remember { mutableStateOf<List<BirthdayContact>>(emptyList()) }

                // 2. Wir prüfen, ob wir die Erlaubnis schon haben
                var hasPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.READ_CONTACTS
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                // 3. Das ist unser "Frage-Fenster" an den Nutzer
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    hasPermission = isGranted
                    if (isGranted) {
                        // Wenn der Nutzer "Ja" klickt, laden wir die Geburtstage
                        contacts = fetchBirthdays()
                    }
                }

                // 4. Direkt beim Start der App prüfen wir, was zu tun ist
                LaunchedEffect(Unit) {
                    if (!hasPermission) {
                        // Wenn keine Erlaubnis da ist, fragen wir danach
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    } else {
                        // Wenn sie schon da ist, laden wir sofort die Daten
                        contacts = fetchBirthdays()
                    }
                }

                // 5. Unsere neue, fertige Liste
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (hasPermission) {
                        // Wir sortieren die Liste von Januar bis Dezember.
                        // Da das Datum z.B. "1990-05-25" oder "--05-25" ist,
                        // sortieren wir einfach nach den letzten 5 Zeichen ("05-25").
                        val sortedContacts = contacts.sortedBy { it.birthday.takeLast(5) }

                        // LazyColumn ist unsere scrollbare Liste
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                        ) {
                            items(sortedContacts) { contact ->
                                // Hier rufen wir unser Design von unten auf
                                BirthdayItem(contact = contact)
                            }
                        }
                    } else {
                        // Wenn der Nutzer die Erlaubnis verweigert hat
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
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Event.DISPLAY_NAME)
            val birthdayIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE)
            val labelIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Event.LABEL)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex) ?: "Unbekannt"
                val bday = it.getString(birthdayIndex) ?: ""
                val label = it.getString(labelIndex) ?: "Privat"

                // Wir fügen das Objekt unserer Liste hinzu
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
    // Card ist eine schöne Box mit leichtem Schatten und abgerundeten Ecken
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        // Row ordnet die Elemente nebeneinander an (Links: Name/Datum, Rechts: Alter/Tage)
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Linke Spalte
            Column {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(text = "Datum: ${contact.birthday}", fontSize = 14.sp)
                Text(text = "Label: ${contact.label}", color = Color.Gray, fontSize = 12.sp)
            }

            // Rechte Spalte (rechtsbündig)
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