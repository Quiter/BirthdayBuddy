package com.heckmannch.birthdaybuddy

import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BirthdayBuddyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
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
        // Wenn kein Datum da ist, geben wir 0 Jahre und 0 Tage zurück
        if (birthDateString.isEmpty()) return Pair(0, 0)

        return try {
            val today = java.time.LocalDate.now()

            // Fall 1: Kontakt hat KEIN Jahr hinterlegt (z.B. "--05-25")
            if (birthDateString.startsWith("--")) {
                val month = birthDateString.substring(2, 4).toInt()
                val day = birthDateString.substring(5, 7).toInt()

                var nextBday = java.time.LocalDate.of(today.year, month, day)
                if (nextBday.isBefore(today)) {
                    nextBday = nextBday.plusYears(1) // Geburtstag war schon, also nächstes Jahr
                }

                val days = java.time.temporal.ChronoUnit.DAYS.between(today, nextBday).toInt()
                return Pair(0, days) // Alter 0, da wir das Jahr nicht kennen
            }
            // Fall 2: Normales Datum mit Jahr (z.B. "1992-05-25")
            else {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val birthDate = java.time.LocalDate.parse(birthDateString, formatter)

                var age = today.year - birthDate.year
                var nextBday = birthDate.withYear(today.year)

                // Korrektur, falls der Geburtstag dieses Jahr schon war oder heute ist
                if (nextBday.isBefore(today) || nextBday.isEqual(today)) {
                    if (nextBday.isBefore(today)) {
                        nextBday = nextBday.plusYears(1)
                    }
                } else {
                    // Geburtstag kommt erst noch, also ist die Person noch ein Jahr jünger
                    age--
                }

                val days = java.time.temporal.ChronoUnit.DAYS.between(today, nextBday).toInt()
                return Pair(age, days)
            }
        } catch (e: Exception) {
            // Falls das Datum komplett unleserlich ist, stürzt die App nicht ab
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

data class BirthdayContact(
    val name: String,
    val birthday: String, // Später wandeln wir das in ein echtes Datum um
    val label: String,    // Für deine Kategorien (Privat, Arbeit, etc.)
    val remainingDays: Int,
    val age: Int
)