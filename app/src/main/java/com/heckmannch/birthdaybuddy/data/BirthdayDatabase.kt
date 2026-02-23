package com.heckmannch.birthdaybuddy.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.heckmannch.birthdaybuddy.model.BirthdayContact

/**
 * Die Room-Datenbank-Klasse.
 * 
 * Diese abstrakte Klasse dient als Hauptzugriffspunkt für die persistente SQLite-Datenbank.
 * @Database: Definiert die enthaltenen Tabellen (Entities) und die Version.
 * @TypeConverters: Registriert die Konverter für komplexe Datentypen.
 */
@Database(entities = [BirthdayContact::class], version = 1, exportSchema = false)
@TypeConverters(DataConverters::class)
abstract class BirthdayDatabase : RoomDatabase() {
    
    // Abstrakte Methode, um Zugriff auf das DAO zu erhalten.
    abstract fun birthdayDao(): BirthdayDao

    companion object {
        // Volatile stellt sicher, dass Änderungen an dieser Variable sofort für alle Threads sichtbar sind.
        @Volatile
        private var INSTANCE: BirthdayDatabase? = null

        /**
         * Singleton-Pattern zur Erstellung der Datenbank-Instanz.
         * Verhindert, dass mehrere Instanzen gleichzeitig geöffnet werden, was zu Datenverlust führen könnte.
         */
        fun getDatabase(context: Context): BirthdayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BirthdayDatabase::class.java,
                    "birthday_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
