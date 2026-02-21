package com.heckmannch.birthdaybuddy.data

import android.content.Context
import android.util.Log
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Das Repository verwaltet den Datenfluss zwischen der lokalen Datenbank 
 * und dem Android-Kontaktsystem.
 */
@Singleton
class BirthdayRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val birthdayDao: BirthdayDao
) {

    /**
     * Ein reaktiver Stream aller Geburtstage aus der lokalen Datenbank.
     */
    val allBirthdays: Flow<List<BirthdayContact>> = birthdayDao.getAllBirthdays()

    /**
     * Synchronisiert die lokale Datenbank mit den Kontakten des Systems.
     */
    suspend fun refreshBirthdays() {
        withContext(Dispatchers.IO) {
            try {
                // 1. Neue Daten vom System laden
                val freshBirthdays = fetchBirthdays(context)
                
                // 2. In die Datenbank schreiben
                birthdayDao.syncBirthdays(freshBirthdays)
                
                if (freshBirthdays.isEmpty()) {
                    Log.i("BirthdayRepository", "Keine Geburtstage in den Kontakten gefunden.")
                }
            } catch (e: Exception) {
                Log.e("BirthdayRepository", "Fehler bei der Synchronisierung der Geburtstage", e)
            }
        }
    }
}
