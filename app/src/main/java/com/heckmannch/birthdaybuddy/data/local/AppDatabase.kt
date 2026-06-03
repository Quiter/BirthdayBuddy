package com.heckmannch.birthdaybuddy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Contact::class, PendingNotification::class],
    version = 8,
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
         * Manueller Migrations-Pfad von 1 auf 2.
         * Hintergrund: Bei einigen Usern fehlte die Tabelle 'pending_notifications' in V1,
         * was die Auto-Migration beim Hinzufügen der 'dismissCount' Spalte zum Absturz brachte.
         * Robustheit: Wenn PRAGMA table_info fehlschlägt oder die Spaltenmanipulation abstürzt,
         * wird die Cache-Tabelle gedroppt und sauber neu angelegt (nicht-kritische Daten).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    val columnCursor = db.query("PRAGMA table_info(pending_notifications)")
                    var hasTable = false
                    var hasDismissCount = false
                    while (columnCursor.moveToNext()) {
                        hasTable = true
                        val nameIndex = columnCursor.getColumnIndex("name")
                        if ((nameIndex != -1) && (columnCursor.getString(nameIndex) == "dismissCount")) {
                            hasDismissCount = true
                        }
                    }
                    columnCursor.close()

                    if (!hasTable) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS `pending_notifications` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contactLookupKeys` TEXT NOT NULL, `daysBefore` INTEGER NOT NULL, `year` INTEGER NOT NULL, `isDone` INTEGER NOT NULL, `dismissCount` INTEGER NOT NULL DEFAULT 0)")
                    } else if (!hasDismissCount) {
                        db.execSQL("ALTER TABLE pending_notifications ADD COLUMN dismissCount INTEGER NOT NULL DEFAULT 0")
                    }
                } catch (_: Exception) {
                    db.execSQL("DROP TABLE IF EXISTS `pending_notifications`")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `pending_notifications` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contactLookupKeys` TEXT NOT NULL, `daysBefore` INTEGER NOT NULL, `year` INTEGER NOT NULL, `isDone` INTEGER NOT NULL, `dismissCount` INTEGER NOT NULL DEFAULT 0)")
                }
            }
        }

        /**
         * Migration von 2 auf 3.
         * Keine Änderungen an 'contacts' oder 'pending_notifications'.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Keine Änderungen an den Tabellen von AppDatabase erforderlich
            }
        }

        /**
         * Migration von 3 auf 4.
         * Fügt die Spalte 'phoneNumber' zur Tabelle 'contacts' hinzu, falls sie fehlt.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    val columnCursor = db.query("PRAGMA table_info(contacts)")
                    var columnExists = false
                    while (columnCursor.moveToNext()) {
                        val nameIndex = columnCursor.getColumnIndex("name")
                        if ((nameIndex != -1) && (columnCursor.getString(nameIndex) == "phoneNumber")) {
                            columnExists = true
                            break
                        }
                    }
                    columnCursor.close()
                    if (!columnExists) {
                        db.execSQL("ALTER TABLE contacts ADD COLUMN phoneNumber TEXT")
                    }
                } catch (_: Exception) {
                    // Safe Fallback
                }
            }
        }

        /**
         * Migration von 4 auf 5.
         * Fügt die Messenger-Spalten 'hasWhatsApp' und 'hasSignal' zur Tabelle 'contacts' hinzu.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    val columnCursor = db.query("PRAGMA table_info(contacts)")
                    var hasWhatsApp = false
                    var hasSignal = false
                    while (columnCursor.moveToNext()) {
                        val nameIndex = columnCursor.getColumnIndex("name")
                        if (nameIndex != -1) {
                            val columnName = columnCursor.getString(nameIndex)
                            if (columnName == "hasWhatsApp") hasWhatsApp = true
                            if (columnName == "hasSignal") hasSignal = true
                        }
                    }
                    columnCursor.close()

                    if (!hasWhatsApp) {
                        db.execSQL("ALTER TABLE contacts ADD COLUMN hasWhatsApp INTEGER NOT NULL DEFAULT 0")
                    }
                    if (!hasSignal) {
                        db.execSQL("ALTER TABLE contacts ADD COLUMN hasSignal INTEGER NOT NULL DEFAULT 0")
                    }
                } catch (_: Exception) {
                    // Safe Fallback
                }
            }
        }

        /**
         * Hilfsfunktion zum sauberen Neuaufbau der 'contacts'-Tabelle auf das korrekte V7-Schema.
         * Da SQLite ALTER TABLE COLUMN NULLABILITY nicht nativ unterstützt,
         * erstellen wir die Tabelle neu und übertragen die Daten.
         * Siehe https://www.sqlite.org/lang_altertable.html#otheralter
         */
        private fun recreateContactsTable(db: SupportSQLiteDatabase) {
            // 1. Bestehende Tabelle umbenennen
            db.execSQL("ALTER TABLE contacts RENAME TO contacts_old")

            // 2. Neue Tabelle mit korrektem V7-Schema (nullable birthday, not null giftIdeas) erstellen
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
         * Migration von 5 auf 6.
         * Ändert die Spalte 'birthday' in 'contacts' auf NULLABLE und fügt Indizes hinzu.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    recreateContactsTable(db)
                } catch (_: Exception) {
                    rollbackContactsTable(db)
                }
            }
        }

        /**
         * Migration von 6 auf 7.
         * Entfernt Tabellen, die nicht mehr zu AppDatabase gehören (jetzt in SettingsDatabase).
         * Zudem wird sichergestellt, dass das Schema der 'contacts'-Tabelle dem korrekten V7-Schema (not null giftIdeas) entspricht.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("DROP TABLE IF EXISTS `label_configs`")
                    db.execSQL("DROP TABLE IF EXISTS `notification_rules`")
                    db.execSQL("DROP TABLE IF EXISTS `app_settings`")
                } catch (_: Exception) {
                    // Ignorieren
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
                } catch (_: Exception) {
                    rollbackContactsTable(db)
                }
            }
        }

        /**
         * Migration von 7 auf 8.
         * Fügt die Spalten 'anniversary' und 'nameDay' zur Tabelle 'contacts' hinzu.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE contacts ADD COLUMN anniversary TEXT")
                db.execSQL("ALTER TABLE contacts ADD COLUMN nameDay TEXT")
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
                    MIGRATION_7_8
                )
                .fallbackToDestructiveMigration(true) // Letzter Rettungsanker bei komplett korruptem Zustand
                .build()
        }

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: try {
                    buildDatabase(context)
                } catch (_: Exception) {
                    try {
                        context.deleteDatabase("birthday_database")
                    } catch (_: Exception) {
                        // Ignorieren
                    }
                    buildDatabase(context)
                }.also { INSTANCE = it }
            }
    }
}
