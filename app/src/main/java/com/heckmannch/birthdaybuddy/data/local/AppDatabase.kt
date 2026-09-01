package com.heckmannch.birthdaybuddy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.heckmannch.birthdaybuddy.data.local.AppDatabase.Companion.rollbackContactsTable

@Database(
    entities = [ContactEntity::class, PendingNotificationEntity::class],
    version = 10,
    exportSchema = true
)
@TypeConverters(Converters::class, GiftIdeaConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun pendingNotificationDao(): PendingNotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null


        /**
         * Hilfsfunktion zum sauberen Neuaufbau der 'contacts'-Tabelle auf das korrekte Schema.
         * Da SQLite ALTER TABLE COLUMN NULLABILITY nicht nativ unterstützt,
         * erstellen wir die Tabelle neu und übertragen die Daten.
         * Siehe https://www.sqlite.org/lang_altertable.html#otheralter
         */
        private fun recreateContactsTable(db: SupportSQLiteDatabase) {
            // 1. Bestehende Tabelle umbenennen
            db.execSQL("ALTER TABLE contacts RENAME TO contacts_old")

            // 2. Neue Tabelle mit V7-Schema erstellen (MIGRATION_5_6 / MIGRATION_6_7 Zwischenzustand)
            db.execSQL("CREATE TABLE IF NOT EXISTS `contacts` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contactId` TEXT NOT NULL, `lookupKey` TEXT NOT NULL, `fullName` TEXT NOT NULL, `birthday` TEXT, `imageUri` TEXT, `phoneNumber` TEXT, `hasWhatsApp` INTEGER NOT NULL DEFAULT 0, `hasSignal` INTEGER NOT NULL DEFAULT 0, `labels` TEXT NOT NULL, `giftIdeas` TEXT NOT NULL)")

            // 3. Vorhandene Spalten ermitteln, um fehlende Spalten (falls vorherige Migrationen fehlschlugen) robust abzufangen
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
         * Rollback bei Fehlern während der Tabellenrekonstruktion.
         */
        private fun rollbackContactsTable(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE contacts_old RENAME TO contacts")
            } catch (_: Exception) {
                // Ignorieren
            }
            try {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_contacts_birthday` ON `contacts` (`birthday`)")
            } catch (_: Exception) {
                // Ignorieren
            }
        }

        /**
         * Migration from database version 5 to 6.
         *
         * Recreates the 'contacts' table to apply the V7-compatible schema (nullable birthday,
         * indexes, etc.) because SQLite does not support ALTER TABLE COLUMN NULLABILITY natively.
         *
         * If an error occurs, it executes a rollback to the previous state using [rollbackContactsTable]
         * and then rethrows the exception wrapped in a [RuntimeException]. Rethrowing is critical so that Room
         * aborts the migration transaction, crashes cleanly, and reports the error, preventing silent database corruption.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    recreateContactsTable(db)
                } catch (e: Exception) {
                    rollbackContactsTable(db)
                    throw RuntimeException("Migration 5 to 6 failed: contacts table recreation error. Rollback executed.", e)
                }
            }
        }

        /**
         * Migration from database version 6 to 7.
         *
         * Drops legacy tables (`label_configs`, `notification_rules`, `app_settings`) that were moved
         * to SettingsDatabase, and ensures the 'contacts' table conforms to the V7-compatible schema
         * where `giftIdeas` is marked NOT NULL.
         *
         * If dropping tables or contacts schema verification fails, the migration throws a [RuntimeException]
         * to prevent silent failures. If contacts table reconstruction fails, it attempts a rollback via
         * [rollbackContactsTable] before rethrowing, ensuring the database does not stay in an inconsistent state.
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
                        recreateContactsTable(db)
                    }
                } catch (e: Exception) {
                    rollbackContactsTable(db)
                    throw RuntimeException("Migration 6 to 7 failed: contacts table check or recreation error. Rollback executed.", e)
                }
            }
        }

        /**
         * Migration von 7 auf 8.
         * Fügt die Spalten 'anniversary' und 'nameDay' zur Tabelle 'contacts' hinzu.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columnCursor = db.query("PRAGMA table_info(contacts)")
                val columns = mutableSetOf<String>()
                while (columnCursor.moveToNext()) {
                    val nameIndex = columnCursor.getColumnIndex("name")
                    if (nameIndex != -1) {
                        columns.add(columnCursor.getString(nameIndex))
                    }
                }
                columnCursor.close()

                if (!columns.contains("anniversary")) {
                    db.execSQL("ALTER TABLE contacts ADD COLUMN anniversary TEXT")
                }
                if (!columns.contains("nameDay")) {
                    db.execSQL("ALTER TABLE contacts ADD COLUMN nameDay TEXT")
                }
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columnCursor = db.query("PRAGMA table_info(contacts)")
                val columns = mutableSetOf<String>()
                while (columnCursor.moveToNext()) {
                    val nameIndex = columnCursor.getColumnIndex("name")
                    if (nameIndex != -1) {
                        columns.add(columnCursor.getString(nameIndex))
                    }
                }
                columnCursor.close()

                if (!columns.contains("spouseLookupKey")) {
                    db.execSQL("ALTER TABLE contacts ADD COLUMN spouseLookupKey TEXT")
                }
            }
        }

        /**
         * Migration from database version 9 to 10.
         * Adds the 'isFavorite' column to the 'contacts' table.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columnCursor = db.query("PRAGMA table_info(contacts)")
                val columns = mutableSetOf<String>()
                while (columnCursor.moveToNext()) {
                    val nameIndex = columnCursor.getColumnIndex("name")
                    if (nameIndex != -1) {
                        columns.add(columnCursor.getString(nameIndex))
                    }
                }
                columnCursor.close()

                if (!columns.contains("isFavorite")) {
                    db.execSQL("ALTER TABLE contacts ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
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
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10
                )
                .build()
        }

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
    }
}
