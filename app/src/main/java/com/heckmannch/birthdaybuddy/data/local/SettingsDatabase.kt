package com.heckmannch.birthdaybuddy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase.Companion.MIGRATION_2_3
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase.Companion.MIGRATION_3_4
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase.Companion.MIGRATION_4_5
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase.Companion.MIGRATION_5_6
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase.Companion.MIGRATION_6_7
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase.Companion.MIGRATION_7_8
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase.Companion.MIGRATION_8_9
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase.Companion.MIGRATION_9_10

/**
 * Room-Datenbank für persistente Einstellungen und nutzerspezifische Konfigurationen.
 *
 * Beinhaltet:
 * - [AppSettingsEntity]: Allgemeine App-Einstellungen (Theme, Kalender-Farben, Features).
 * - [LabelConfigEntity]: Konfigurationen für benutzerdefinierte Labels (Farben, Benachrichtigungen, Widget-Sichtbarkeit).
 * - [NotificationRuleEntity]: Globale und label-spezifische Benachrichtigungsregeln.
 * - [ContactUserData]: Nutzerspezifische, in der Cloud gesicherte Kontaktdaten (z. B. Notizen, Geschenkideen, Beziehungsstatus).
 *
 * Migrationen:
 * - Version 2 -> 3: [MIGRATION_2_3] (Hinzufügen von `otherEventsEnabled` in `app_settings`)
 * - Version 3 -> 4: [MIGRATION_3_4] (Hinzufügen von `spouseLookupKey` in `contact_user_data` und `ignoredCouplePairs` in `app_settings`)
 * - Version 4 -> 5: [MIGRATION_4_5] (Hinzufügen der Kalender-Farben `birthdayCalendarColor`, `anniversaryCalendarColor`, `nameDayCalendarColor` in `app_settings`)
 * - Version 5 -> 6: [MIGRATION_5_6] (Hinzufügen von Theme-Einstellungen `themeMode`, `themeAmoled`, `themeAccent` in `app_settings`)
 * - Version 6 -> 7: [MIGRATION_6_7] (Hinzufügen von `themeContrast` in `app_settings`)
 * - Version 7 -> 8: [MIGRATION_7_8] (Hinzufügen von `labelsEnabled` in `app_settings`)
 * - Version 8 -> 9: [MIGRATION_8_9] (Tabellenrekonstruktion von `app_settings` zur Entfernung von `themeContrast`)
 * - Version 9 -> 10: [MIGRATION_9_10] (Hinzufügen von `notificationsEnabled` und `showInWidget` in `label_configs`)
 */
@Database(
    entities = [LabelConfigEntity::class, NotificationRuleEntity::class, AppSettingsEntity::class, ContactUserData::class],
    version = 10,
    exportSchema = true
)
@TypeConverters(Converters::class, GiftIdeaConverters::class)
abstract class SettingsDatabase : RoomDatabase() {
    abstract fun labelConfigDao(): LabelConfigDao
    abstract fun notificationRuleDao(): NotificationRuleDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun contactUserDataDao(): ContactUserDataDao

    companion object {
        @Volatile
        private var INSTANCE: SettingsDatabase? = null

        private const val DATABASE_NAME = "settings_database"

        /**
         * Migration von Version 2 auf 3.
         *
         * Fügt die Spalte `otherEventsEnabled` zur Tabelle `app_settings` hinzu.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN otherEventsEnabled INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Migration von Version 3 auf 4.
         *
         * Fügt die Spalte `spouseLookupKey` zur Tabelle `contact_user_data` sowie
         * die Spalte `ignoredCouplePairs` zur Tabelle `app_settings` hinzu.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE contact_user_data ADD COLUMN spouseLookupKey TEXT")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN ignoredCouplePairs TEXT NOT NULL DEFAULT '[]'")
            }
        }

        /**
         * Migration von Version 4 auf 5.
         *
         * Fügt die Kalender-Farben (`birthdayCalendarColor`, `anniversaryCalendarColor`,
         * `nameDayCalendarColor`) zur Tabelle `app_settings` hinzu.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN birthdayCalendarColor INTEGER NOT NULL DEFAULT ${0xFFE91E63.toInt()}")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN anniversaryCalendarColor INTEGER NOT NULL DEFAULT ${0xFF9C27B0.toInt()}")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN nameDayCalendarColor INTEGER NOT NULL DEFAULT ${0xFFFF9800.toInt()}")
            }
        }

        /**
         * Migration von Version 5 auf 6.
         *
         * Fügt die Theme-Einstellungen (`themeMode`, `themeAmoled`, `themeAccent`)
         * zur Tabelle `app_settings` hinzu.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'SYSTEM'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN themeAmoled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN themeAccent TEXT NOT NULL DEFAULT 'SYSTEM'")
            }
        }

        /**
         * Migration von Version 6 auf 7.
         *
         * Fügt die Spalte `themeContrast` zur Tabelle `app_settings` hinzu.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN themeContrast REAL NOT NULL DEFAULT 0.0")
            }
        }

        /**
         * Migration von Version 7 auf 8.
         *
         * Fügt die Spalte `labelsEnabled` zur Tabelle `app_settings` hinzu.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN labelsEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }

        /**
         * Migration von Version 8 auf 9.
         *
         * Führt eine Tabellenrekonstruktion von `app_settings` durch, um die nicht mehr
         * benötigte Spalte `themeContrast` zu entfernen.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    // 1. Create a new table without themeContrast
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `app_settings_new` (
                            `id` INTEGER NOT NULL, 
                            `notificationsEnabled` INTEGER NOT NULL, 
                            `persistentNotifications` INTEGER NOT NULL, 
                            `onboardingCompleted` INTEGER NOT NULL, 
                            `lastSyncTimestamp` INTEGER NOT NULL, 
                            `calendarSyncEnabled` INTEGER NOT NULL, 
                            `calendarId` INTEGER, 
                            `otherEventsEnabled` INTEGER NOT NULL, 
                            `ignoredCouplePairs` TEXT NOT NULL, 
                            `birthdayCalendarColor` INTEGER NOT NULL, 
                            `anniversaryCalendarColor` INTEGER NOT NULL, 
                            `nameDayCalendarColor` INTEGER NOT NULL, 
                            `themeMode` TEXT NOT NULL, 
                            `themeAmoled` INTEGER NOT NULL, 
                            `themeAccent` TEXT NOT NULL, 
                            `labelsEnabled` INTEGER NOT NULL, 
                            PRIMARY KEY(`id`)
                        )
                    """.trimIndent()
                    )

                    // 2. Copy the data
                    db.execSQL(
                        """
                        INSERT INTO app_settings_new (
                            id, notificationsEnabled, persistentNotifications, onboardingCompleted, 
                            lastSyncTimestamp, calendarSyncEnabled, calendarId, otherEventsEnabled, 
                            ignoredCouplePairs, birthdayCalendarColor, anniversaryCalendarColor, 
                            nameDayCalendarColor, themeMode, themeAmoled, themeAccent, labelsEnabled
                        )
                        SELECT 
                            id, notificationsEnabled, persistentNotifications, onboardingCompleted, 
                            lastSyncTimestamp, calendarSyncEnabled, calendarId, otherEventsEnabled, 
                            ignoredCouplePairs, birthdayCalendarColor, anniversaryCalendarColor, 
                            nameDayCalendarColor, themeMode, themeAmoled, themeAccent, labelsEnabled
                        FROM app_settings
                    """.trimIndent()
                    )

                    // 3. Drop the old table
                    db.execSQL("DROP TABLE app_settings")

                    // 4. Rename the new table
                    db.execSQL("ALTER TABLE app_settings_new RENAME TO app_settings")
                } catch (e: Exception) {
                    throw RuntimeException(
                        "Migration 8 to 9 failed: app_settings table recreation error.",
                        e
                    )
                }
            }
        }

        /**
         * Migration von Version 9 auf 10.
         *
         * Fügt die Spalten `notificationsEnabled` und `showInWidget` zur Tabelle `label_configs` hinzu.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE label_configs ADD COLUMN notificationsEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE label_configs ADD COLUMN showInWidget INTEGER NOT NULL DEFAULT 1")
            }
        }

        private fun buildDatabase(context: Context): SettingsDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SettingsDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10
                )
                .build()
        }

        fun getDatabase(context: Context): SettingsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
    }
}
