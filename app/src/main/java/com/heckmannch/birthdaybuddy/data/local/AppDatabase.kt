package com.heckmannch.birthdaybuddy.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room-Datenbank für kontobezogene Entitäten ([ContactEntity], [PendingNotificationEntity]).
 *
 * Migrationen:
 * - Version 5 -> 6: [MIGRATION_5_6] (Manuell via Tabellenrekonstruktion zur Bereinigung von Nullabilities und Indizes)
 * - Version 6 -> 7: [MIGRATION_6_7] (Manuell, Bereinigung von Legacy-Tabellen und NOT NULL Constraint für giftIdeas)
 * - Version 7 -> 8: [AutoMigration] (Hinzufügen der Spalten `anniversary` und `nameDay` in `contacts`)
 * - Version 8 -> 9: [AutoMigration] (Hinzufügen der Spalte `spouseLookupKey` in `contacts`)
 * - Version 9 -> 10: [AutoMigration] (Hinzufügen der Spalte `isFavorite` mit Default `0` in `contacts`)
 * - Version 10 -> 11: [AutoMigration] (Hinzufügen von Indizes für `PendingNotificationEntity` (`isDone`, `year`/`daysBefore`) und `ContactEntity` (`anniversary`))
 */
@Database(
    entities = [ContactEntity::class, PendingNotificationEntity::class],
    version = 11,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11)
    ]
)
@TypeConverters(Converters::class, GiftIdeaConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun pendingNotificationDao(): PendingNotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null


        /**
         * Hilfsfunktion zum sauberen Neuaufbau der 'contacts'-Tabelle auf das V6-Schema.
         * In V6 ist birthday nullable und giftIdeas ist weiterhin nullable (TEXT).
         */
        private fun recreateContactsTableV6(db: SupportSQLiteDatabase) {
            // 1. Bestehende Tabelle umbenennen
            db.execSQL("ALTER TABLE contacts RENAME TO contacts_old")

            // 2. Neue Tabelle mit V6-Schema erstellen
            db.execSQL("CREATE TABLE IF NOT EXISTS `contacts` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contactId` TEXT NOT NULL, `lookupKey` TEXT NOT NULL, `fullName` TEXT NOT NULL, `birthday` TEXT, `imageUri` TEXT, `phoneNumber` TEXT, `hasWhatsApp` INTEGER NOT NULL DEFAULT 0, `hasSignal` INTEGER NOT NULL DEFAULT 0, `labels` TEXT NOT NULL, `giftIdeas` TEXT)")

            // 3. Vorhandene Spalten ermitteln, um fehlende Spalten robust abzufangen
            val columnsInOld = mutableSetOf<String>()
            val columnCursor = db.query("PRAGMA table_info(contacts_old)")
            while (columnCursor.moveToNext()) {
                val nameIndex = columnCursor.getColumnIndex("name")
                if (nameIndex != -1) {
                    columnsInOld.add(columnCursor.getString(nameIndex))
                }
            }
            columnCursor.close()

            val selectColumns = mutableListOf<String>()
            selectColumns.add("localId")
            selectColumns.add("contactId")
            selectColumns.add("lookupKey")
            selectColumns.add("fullName")
            selectColumns.add("birthday")
            selectColumns.add("imageUri")

            if (columnsInOld.contains("phoneNumber")) {
                selectColumns.add("phoneNumber")
            } else {
                selectColumns.add("NULL AS phoneNumber")
            }

            if (columnsInOld.contains("hasWhatsApp")) {
                selectColumns.add("COALESCE(hasWhatsApp, 0) AS hasWhatsApp")
            } else {
                selectColumns.add("0 AS hasWhatsApp")
            }

            if (columnsInOld.contains("hasSignal")) {
                selectColumns.add("COALESCE(hasSignal, 0) AS hasSignal")
            } else {
                selectColumns.add("0 AS hasSignal")
            }

            if (columnsInOld.contains("labels")) {
                selectColumns.add("COALESCE(labels, '[]') AS labels")
            } else {
                selectColumns.add("'[]' AS labels")
            }

            if (columnsInOld.contains("giftIdeas")) {
                selectColumns.add("giftIdeas")
            } else {
                selectColumns.add("NULL AS giftIdeas")
            }

            val selectQuery = selectColumns.joinToString(", ")

            // 4. Daten kopieren mit dynamic fallback
            db.execSQL("INSERT INTO contacts (localId, contactId, lookupKey, fullName, birthday, imageUri, phoneNumber, hasWhatsApp, hasSignal, labels, giftIdeas) SELECT $selectQuery FROM contacts_old")

            // 5. Alte Tabelle löschen
            db.execSQL("DROP TABLE IF EXISTS contacts_old")

            // 6. Indizes neu anlegen
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_contacts_lookupKey` ON `contacts` (`lookupKey`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_contacts_birthday` ON `contacts` (`birthday`)")
        }

        /**
         * Hilfsfunktion zum sauberen Neuaufbau der 'contacts'-Tabelle auf das V7-Schema.
         * In V7 ist giftIdeas NOT NULL.
         */
        private fun recreateContactsTableV7(db: SupportSQLiteDatabase) {
            // 1. Bestehende Tabelle umbenennen
            db.execSQL("ALTER TABLE contacts RENAME TO contacts_old")

            // 2. Neue Tabelle mit V7-Schema erstellen
            db.execSQL("CREATE TABLE IF NOT EXISTS `contacts` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contactId` TEXT NOT NULL, `lookupKey` TEXT NOT NULL, `fullName` TEXT NOT NULL, `birthday` TEXT, `imageUri` TEXT, `phoneNumber` TEXT, `hasWhatsApp` INTEGER NOT NULL DEFAULT 0, `hasSignal` INTEGER NOT NULL DEFAULT 0, `labels` TEXT NOT NULL, `giftIdeas` TEXT NOT NULL)")

            // 3. Vorhandene Spalten ermitteln
            val columnsInOld = mutableSetOf<String>()
            val columnCursor = db.query("PRAGMA table_info(contacts_old)")
            while (columnCursor.moveToNext()) {
                val nameIndex = columnCursor.getColumnIndex("name")
                if (nameIndex != -1) {
                    columnsInOld.add(columnCursor.getString(nameIndex))
                }
            }
            columnCursor.close()

            val selectColumns = mutableListOf<String>()
            selectColumns.add("localId")
            selectColumns.add("contactId")
            selectColumns.add("lookupKey")
            selectColumns.add("fullName")
            selectColumns.add("birthday")
            selectColumns.add("imageUri")

            if (columnsInOld.contains("phoneNumber")) {
                selectColumns.add("phoneNumber")
            } else {
                selectColumns.add("NULL AS phoneNumber")
            }

            if (columnsInOld.contains("hasWhatsApp")) {
                selectColumns.add("COALESCE(hasWhatsApp, 0) AS hasWhatsApp")
            } else {
                selectColumns.add("0 AS hasWhatsApp")
            }

            if (columnsInOld.contains("hasSignal")) {
                selectColumns.add("COALESCE(hasSignal, 0) AS hasSignal")
            } else {
                selectColumns.add("0 AS hasSignal")
            }

            if (columnsInOld.contains("labels")) {
                selectColumns.add("COALESCE(labels, '[]') AS labels")
            } else {
                selectColumns.add("'[]' AS labels")
            }

            if (columnsInOld.contains("giftIdeas")) {
                selectColumns.add("COALESCE(giftIdeas, '[]') AS giftIdeas")
            } else {
                selectColumns.add("'[]' AS giftIdeas")
            }

            val selectQuery = selectColumns.joinToString(", ")

            // 4. Daten kopieren mit dynamic fallback
            db.execSQL("INSERT INTO contacts (localId, contactId, lookupKey, fullName, birthday, imageUri, phoneNumber, hasWhatsApp, hasSignal, labels, giftIdeas) SELECT $selectQuery FROM contacts_old")

            // 5. Alte Tabelle löschen
            db.execSQL("DROP TABLE IF EXISTS contacts_old")

            // 6. Indizes neu anlegen
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_contacts_lookupKey` ON `contacts` (`lookupKey`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_contacts_birthday` ON `contacts` (`birthday`)")
        }

        /**
         * Migration from database version 5 to 6.
         *
         * Recreates the 'contacts' table to apply the V6 schema (nullable birthday,
         * indexes, etc.) because SQLite does not support ALTER TABLE COLUMN NULLABILITY natively.
         *
         * Room executes migrations within an SQLite transaction by default. If an error occurs,
         * throwing a [RuntimeException] aborts the transaction, triggering a clean SQLite rollback
         * and preventing silent database corruption.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    recreateContactsTableV6(db)
                } catch (e: Exception) {
                    throw RuntimeException("Migration 5 to 6 failed: contacts table recreation error.", e)
                }
            }
        }

        /**
         * Migration from database version 6 to 7.
         *
         * Drops legacy tables (`label_configs`, `notification_rules`, `app_settings`) that were moved
         * to SettingsDatabase, and ensures the 'contacts' table conforms to the V7 schema
         * where `giftIdeas` is marked NOT NULL.
         *
         * Room executes migrations within an SQLite transaction by default. If an error occurs,
         * throwing a [RuntimeException] aborts the transaction, triggering a clean SQLite rollback
         * and preventing silent database corruption.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("DROP TABLE IF EXISTS `label_configs`")
                    db.execSQL("DROP TABLE IF EXISTS `notification_rules`")
                    db.execSQL("DROP TABLE IF EXISTS `app_settings`")
                } catch (e: Exception) {
                    throw RuntimeException("Migration 6 to 7 failed: dropping legacy tables failed.", e)
                }

                try {
                    val columnCursor = db.query("PRAGMA table_info(contacts)")
                    var isGiftIdeasNotNull = false
                    while (columnCursor.moveToNext()) {
                        val nameIndex = columnCursor.getColumnIndex("name")
                        val notNullIndex = columnCursor.getColumnIndex("notnull")
                        if (nameIndex != -1 && notNullIndex != -1) {
                            if (columnCursor.getString(nameIndex) == "giftIdeas") {
                                isGiftIdeasNotNull = columnCursor.getInt(notNullIndex) == 1
                                break
                            }
                        }
                    }
                    columnCursor.close()

                    if (!isGiftIdeasNotNull) {
                        recreateContactsTableV7(db)
                    }
                } catch (e: Exception) {
                    throw RuntimeException("Migration 6 to 7 failed: contacts table check or recreation error.", e)
                }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "birthday_database",
            )
                .addMigrations(
                    MIGRATION_5_6,
                    MIGRATION_6_7
                )
                .build()
        }

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
    }
}
