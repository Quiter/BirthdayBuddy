package com.heckmannch.birthdaybuddy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) für die Geburtstags-Datenbank.
 * 
 * Das DAO definiert die Methoden für den Zugriff auf die SQLite-Datenbank über Room.
 * Wir nutzen hier Coroutines (suspend) für schreibende Zugriffe und Flows für 
 * reaktive Lesezugriffe.
 */
@Dao
interface BirthdayDao {
    /**
     * Gibt einen Flow aller Geburtstage zurück.
     * Durch den Flow wird die UI automatisch aktualisiert, sobald sich Daten in 
     * der Tabelle ändern (Observer Pattern).
     */
    @Query("SELECT * FROM birthdays")
    fun getAllBirthdays(): Flow<List<BirthdayContact>>

    /**
     * Fügt eine Liste von Geburtstagen ein.
     * OnConflictStrategy.REPLACE sorgt dafür, dass bei gleicher ID der alte 
     * Eintrag überschrieben wird.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBirthdays(birthdays: List<BirthdayContact>)

    /**
     * Löscht alle Einträge aus der Tabelle.
     */
    @Query("DELETE FROM birthdays")
    suspend fun deleteAllBirthdays()

    /**
     * Führt eine vollständige Synchronisation in einer einzelnen Transaktion durch.
     * 
     * Warum @Transaction?
     * Eine Transaktion stellt sicher, dass die Datenbank entweder komplett aktualisiert
     * wird oder gar nicht (Atomarität). Zudem verhindert es Flackern in der UI, da 
     * der Flow erst nach Abschluss der gesamten Transaktion benachrichtigt wird.
     */
    @Transaction
    suspend fun syncBirthdays(birthdays: List<BirthdayContact>) {
        // Zuerst löschen wir alles, um sicherzustellen, dass Kontakte, die im 
        // Telefon gelöscht wurden, auch aus unserer Datenbank verschwinden.
        deleteAllBirthdays()
        // Dann fügen wir die aktuellen Daten vom System neu ein.
        insertBirthdays(birthdays)
    }
}
