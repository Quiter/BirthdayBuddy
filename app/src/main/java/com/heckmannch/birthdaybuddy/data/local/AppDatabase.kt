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
 * - Version 1 -> 2: [APP_MIGRATION_1_2] (Hinzufügen der Spalte `dismissCount` mit Default `0` in `pending_notifications`)
 * - Version 2 -> 3: [APP_MIGRATION_2_3] (Tabellenrekonstruktion von `app_settings` zur Entfernung von `swipeHintShown`)
 * - Version 3 -> 4: [APP_MIGRATION_3_4] (Hinzufügen der Spalte `phoneNumber` in `contacts`)
 * - Version 4 -> 5: [APP_MIGRATION_4_5] (Hinzufügen der Messenger-Spalten `hasWhatsApp` und `hasSignal` mit Default `0` in `contacts`)
 * - Version 5 -> 6: [APP_MIGRATION_5_6] (Manuell via Tabellenrekonstruktion zur Bereinigung von Nullabilities und Indizes)
 * - Version 6 -> 7: [APP_MIGRATION_6_7] (Manuell, Bereinigung von Legacy-Tabellen und NOT NULL Constraint für giftIdeas)
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
}

/**
 * Hilfsfunktion zum sauberen Neuaufbau der 'contacts'-Tabelle.
 *
 * @param db Die SQLite-Datenbankinstanz.
 * @param giftIdeasNotNull Gibt an, ob die Spalte `giftIdeas` als NOT NULL definiert und mit '[]' statt NULL befüllt werden soll (Schema V7+) oder als nullable TEXT (Schema V6).
 */
private fun recreateContactsTable(db: SupportSQLiteDatabase, giftIdeasNotNull: Boolean) {
    // 1. Bestehende Tabelle umbenennen
    db.execSQL("ALTER TABLE contacts RENAME TO contacts_old")

    // 2. Neue Tabelle erstellen (Schema abhängig von giftIdeasNotNull)
    val giftIdeasColumnDef = if (giftIdeasNotNull) "`giftIdeas` TEXT NOT NULL" else "`giftIdeas` TEXT"
    db.execSQL("CREATE TABLE IF NOT EXISTS `contacts` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contactId` TEXT NOT NULL, `lookupKey` TEXT NOT NULL, `fullName` TEXT NOT NULL, `birthday` TEXT, `imageUri` TEXT, `phoneNumber` TEXT, `hasWhatsApp` INTEGER NOT NULL DEFAULT 0, `hasSignal` INTEGER NOT NULL DEFAULT 0, `labels` TEXT NOT NULL, $giftIdeasColumnDef)")

    // 3. Vorhandene Spalten ermitteln, um fehlende Spalten robust abzufangen
    val columnsInOld = mutableSetOf<String>()
    db.query("PRAGMA table_info(contacts_old)").use { columnCursor ->
        while (columnCursor.moveToNext()) {
            val nameIndex = columnCursor.getColumnIndex("name")
            if (nameIndex != -1) {
                columnsInOld.add(columnCursor.getString(nameIndex))
            }
        }
    }

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

    if (giftIdeasNotNull) {
        if (columnsInOld.contains("giftIdeas")) {
            selectColumns.add("COALESCE(giftIdeas, '[]') AS giftIdeas")
        } else {
            selectColumns.add("'[]' AS giftIdeas")
        }
    } else {
        if (columnsInOld.contains("giftIdeas")) {
            selectColumns.add("giftIdeas")
        } else {
            selectColumns.add("NULL AS giftIdeas")
        }
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
 * Migration from database version 1 to 2.
 *
 * Adds the 'dismissCount' column to the 'pending_notifications' table with default value 0.
 * Includes defensive fallback in case the table is missing or already has the column.
 */
internal val APP_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            var hasTable = false
            var hasDismissCount = false
            db.query("PRAGMA table_info(pending_notifications)").use { columnCursor ->
                while (columnCursor.moveToNext()) {
                    hasTable = true
                    val nameIndex = columnCursor.getColumnIndex("name")
                    if (nameIndex != -1 && columnCursor.getString(nameIndex) == "dismissCount") {
                        hasDismissCount = true
                    }
                }
            }

            if (!hasTable) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `pending_notifications` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contactLookupKeys` TEXT NOT NULL, `daysBefore` INTEGER NOT NULL, `year` INTEGER NOT NULL, `isDone` INTEGER NOT NULL, `dismissCount` INTEGER NOT NULL DEFAULT 0)")
            } else if (!hasDismissCount) {
                db.execSQL("ALTER TABLE pending_notifications ADD COLUMN dismissCount INTEGER NOT NULL DEFAULT 0")
            }
        } catch (e: Exception) {
            throw RuntimeException(
                "Migration 1 to 2 failed: adding dismissCount to pending_notifications failed.",
                e
            )
        }
    }
}

/**
 * Migration from database version 2 to 3.
 *
 * Recreates the 'app_settings' table to remove the unused 'swipeHintShown' column,
 * ensuring backwards compatibility across SQLite versions (including minSdk 28 / SQLite < 3.35.0).
 */
internal val APP_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            var hasSwipeHintShown = false
            db.query("PRAGMA table_info(app_settings)").use { columnCursor ->
                while (columnCursor.moveToNext()) {
                    val nameIndex = columnCursor.getColumnIndex("name")
                    if (nameIndex != -1 && columnCursor.getString(nameIndex) == "swipeHintShown") {
                        hasSwipeHintShown = true
                        break
                    }
                }
            }

            if (hasSwipeHintShown) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `app_settings_new` (
                        `id` INTEGER NOT NULL,
                        `notificationsEnabled` INTEGER NOT NULL,
                        `persistentNotifications` INTEGER NOT NULL,
                        `onboardingCompleted` INTEGER NOT NULL,
                        `lastSyncTimestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO app_settings_new (id, notificationsEnabled, persistentNotifications, onboardingCompleted, lastSyncTimestamp)
                    SELECT id, notificationsEnabled, persistentNotifications, onboardingCompleted, lastSyncTimestamp
                    FROM app_settings
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE app_settings")
                db.execSQL("ALTER TABLE app_settings_new RENAME TO app_settings")
            }
        } catch (e: Exception) {
            throw RuntimeException(
                "Migration 2 to 3 failed: app_settings table recreation error.",
                e
            )
        }
    }
}

/**
 * Migration from database version 3 to 4.
 *
 * Adds the nullable 'phoneNumber' column to the 'contacts' table.
 */
internal val APP_MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            var columnExists = false
            db.query("PRAGMA table_info(contacts)").use { columnCursor ->
                while (columnCursor.moveToNext()) {
                    val nameIndex = columnCursor.getColumnIndex("name")
                    if (nameIndex != -1 && columnCursor.getString(nameIndex) == "phoneNumber") {
                        columnExists = true
                        break
                    }
                }
            }

            if (!columnExists) {
                db.execSQL("ALTER TABLE contacts ADD COLUMN phoneNumber TEXT")
            }
        } catch (e: Exception) {
            throw RuntimeException(
                "Migration 3 to 4 failed: adding phoneNumber to contacts failed.",
                e
            )
        }
    }
}

/**
 * Migration from database version 4 to 5.
 *
 * Adds the messenger columns 'hasWhatsApp' and 'hasSignal' with default value 0 to the 'contacts' table.
 */
internal val APP_MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            var hasWhatsApp = false
            var hasSignal = false
            db.query("PRAGMA table_info(contacts)").use { columnCursor ->
                while (columnCursor.moveToNext()) {
                    val nameIndex = columnCursor.getColumnIndex("name")
                    if (nameIndex != -1) {
                        val columnName = columnCursor.getString(nameIndex)
                        if (columnName == "hasWhatsApp") hasWhatsApp = true
                        if (columnName == "hasSignal") hasSignal = true
                    }
                }
            }

            if (!hasWhatsApp) {
                db.execSQL("ALTER TABLE contacts ADD COLUMN hasWhatsApp INTEGER NOT NULL DEFAULT 0")
            }
            if (!hasSignal) {
                db.execSQL("ALTER TABLE contacts ADD COLUMN hasSignal INTEGER NOT NULL DEFAULT 0")
            }
        } catch (e: Exception) {
            throw RuntimeException(
                "Migration 4 to 5 failed: adding messenger columns to contacts failed.",
                e
            )
        }
    }
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
internal val APP_MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            recreateContactsTable(db, giftIdeasNotNull = false)
        } catch (e: Exception) {
            throw RuntimeException(
                "Migration 5 to 6 failed: contacts table recreation error.",
                e
            )
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
internal val APP_MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("DROP TABLE IF EXISTS `label_configs`")
            db.execSQL("DROP TABLE IF EXISTS `notification_rules`")
            db.execSQL("DROP TABLE IF EXISTS `app_settings`")
        } catch (e: Exception) {
            throw RuntimeException(
                "Migration 6 to 7 failed: dropping legacy tables failed.",
                e
            )
        }

        try {
            var isGiftIdeasNotNull = false
            db.query("PRAGMA table_info(contacts)").use { columnCursor ->
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
            }

            if (!isGiftIdeasNotNull) {
                recreateContactsTable(db, giftIdeasNotNull = true)
            }
        } catch (e: Exception) {
            throw RuntimeException(
                "Migration 6 to 7 failed: contacts table check or recreation error.",
                e
            )
        }
    }
}

/**
 * Erstellt und konfiguriert die [AppDatabase]-Instanz.
 *
 * @param context Der Anwendungskontext.
 * @return Die gebaute [AppDatabase]-Instanz.
 */
internal fun buildAppDatabase(context: Context): AppDatabase {
    return Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "birthday_database",
    )
        .addMigrations(
            APP_MIGRATION_1_2,
            APP_MIGRATION_2_3,
            APP_MIGRATION_3_4,
            APP_MIGRATION_4_5,
            APP_MIGRATION_5_6,
            APP_MIGRATION_6_7
        )
        .build()
}
