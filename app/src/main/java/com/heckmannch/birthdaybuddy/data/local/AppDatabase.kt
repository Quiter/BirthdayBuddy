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
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
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
                } catch (e: Exception) {
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
                } catch (e: Exception) {
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
                } catch (e: Exception) {
                    // Safe Fallback
                }
            }
        }

        /**
         * Migration von 5 auf 6.
         * Ändert die Spalte 'birthday' in 'contacts' auf NULLABLE und fügt Indizes hinzu.
         * Da SQLite ALTER TABLE COLUMN NULLABILITY nicht nativ unterstützt,
         * erstellen wir die Tabelle neu und übertragen die Daten.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    // 1. Bestehende Tabelle umbenennen
                    db.execSQL("ALTER TABLE contacts RENAME TO contacts_old")

                    // 2. Neue Tabelle mit nullable birthday (V6/V7 Schema) erstellen
                    db.execSQL("CREATE TABLE IF NOT EXISTS `contacts` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contactId` TEXT NOT NULL, `lookupKey` TEXT NOT NULL, `fullName` TEXT NOT NULL, `birthday` TEXT, `imageUri` TEXT, `phoneNumber` TEXT, `hasWhatsApp` INTEGER NOT NULL DEFAULT 0, `hasSignal` INTEGER NOT NULL DEFAULT 0, `labels` TEXT NOT NULL, `giftIdeas` TEXT)")

                    // 3. Daten aus der alten Tabelle kopieren
                    db.execSQL("INSERT INTO contacts (localId, contactId, lookupKey, fullName, birthday, imageUri, phoneNumber, hasWhatsApp, hasSignal, labels, giftIdeas) SELECT localId, contactId, lookupKey, fullName, birthday, imageUri, phoneNumber, hasWhatsApp, hasSignal, labels, giftIdeas FROM contacts_old")

                    // 4. Alte Tabelle löschen
                    db.execSQL("DROP TABLE IF EXISTS contacts_old")

                    // 5. Indizes neu anlegen
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_contacts_lookupKey` ON `contacts` (`lookupKey`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_contacts_birthday` ON `contacts` (`birthday`)")
                } catch (e: Exception) {
                    // Falls die Tabellenneuerstellung fehlschlägt, versuchen wir zumindest den Index anzulegen
                    try {
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_contacts_birthday` ON `contacts` (`birthday`)")
                    } catch (ex: Exception) {
                        // Ignorieren
                    }
                }
            }
        }

        /**
         * Migration von 6 auf 7.
         * Entfernt Tabellen, die nicht mehr zu AppDatabase gehören (jetzt in SettingsDatabase).
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("DROP TABLE IF EXISTS `label_configs`")
                    db.execSQL("DROP TABLE IF EXISTS `notification_rules`")
                    db.execSQL("DROP TABLE IF EXISTS `app_settings`")
                } catch (e: Exception) {
                    // Ignorieren
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "birthday_database",
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )
                    .fallbackToDestructiveMigration() // Letzter Rettungsanker bei komplett korruptem Zustand
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
