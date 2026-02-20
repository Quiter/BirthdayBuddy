package com.heckmannch.birthdaybuddy.data

import android.content.Context
import android.util.Log
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Das Repository verwaltet den Datenfluss zwischen der lokalen Datenbank 
 * und dem Android-Kontaktsystem.
 */
class BirthdayRepository(private val context: Context) {

    private val database = BirthdayDatabase.getDatabase(context)
    private val birthdayDao = database.birthdayDao()

    /**
     * Ein reaktiver Stream aller Geburtstage aus der lokalen Datenbank.
     */
    val allBirthdays: Flow<List<BirthdayContact>> = birthdayDao.getAllBirthdays()

    /**
     * Synchronisiert die lokale Datenbank mit den Kontakten des Systems.
     * Das passiert asynchron im Hintergrund.
     */
    suspend fun refreshBirthdays() {
        withContext(Dispatchers.IO) {
            try {
                // 1. Neue Daten vom System laden
                val freshBirthdays = fetchBirthdays(context)
                
                // 2. In die Datenbank schreiben
                // Wir nutzen syncBirthdays, um auch gelöschte Kontakte zu entfernen
                birthdayDao.syncBirthdays(freshBirthdays)
                
                if (freshBirthdays.isEmpty()) {
                    Log.i("BirthdayRepository", "Keine Geburtstage in den Kontakten gefunden.")
                }
            } catch (e: Exception) {
                // Fehlerbehandlung mit Logging
                Log.e("BirthdayRepository", "Fehler bei der Synchronisierung der Geburtstage", e)
            }
        }
    }
}
