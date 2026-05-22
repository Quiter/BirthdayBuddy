package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Testet die Datenbank-Migrationen.
 * WICHTIG: Erfordert die exportierten Schemas in app/schemas.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        // 1. Datenbank in Version 1 erstellen
        helper.createDatabase(TEST_DB, 1).apply {
            // Testdaten einfügen (V1-Schema)
            execSQL(
                "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, labels) " +
                        "VALUES ('1', 'key1', 'Max Mustermann', '1990-01-01', '[]')"
            )
            close()
        }

        // 2. Migration auf Version 2 ausführen und validieren
        // Wir übergeben die manuelle Migration, die wir testen wollen
        helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2)
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To7() {
        // 1. Datenbank in Version 1 erstellen mit Testdaten
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, labels) " +
                        "VALUES ('1', 'key1', 'Max Mustermann', '1990-01-01', '[]')"
            )
            close()
        }

        // 2. Migration über die gesamte Kette (1 bis 7) ausführen und validieren
        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB,
            7,
            true,
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7
        )

        // 3. Prüfen, ob die Daten intakt sind und neue Spalten default-Werte haben
        val cursor = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key1'")
        assert(cursor.moveToFirst())

        val nameIndex = cursor.getColumnIndex("fullName")
        val birthdayIndex = cursor.getColumnIndex("birthday")
        val phoneIndex = cursor.getColumnIndex("phoneNumber")
        val whatsappIndex = cursor.getColumnIndex("hasWhatsApp")
        val signalIndex = cursor.getColumnIndex("hasSignal")

        assert(cursor.getString(nameIndex) == "Max Mustermann")
        assert(cursor.getString(birthdayIndex) == "1990-01-01")
        assert(cursor.isNull(phoneIndex))
        assert(cursor.getInt(whatsappIndex) == 0)
        assert(cursor.getInt(signalIndex) == 0)

        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To7() {
        // 1. Datenbank in Version 2 erstellen mit Testdaten
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, labels) " +
                        "VALUES ('2', 'key2', 'Erika Mustermann', '1992-02-02', '[]')"
            )
            close()
        }

        // 2. Migration über die Kette (2 bis 7) ausführen und validieren
        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB,
            7,
            true,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7
        )

        // 3. Prüfen, ob die Daten intakt sind
        val cursor = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key2'")
        assert(cursor.moveToFirst())
        assert(cursor.getString(cursor.getColumnIndex("fullName")) == "Erika Mustermann")
        assert(cursor.getString(cursor.getColumnIndex("birthday")) == "1992-02-02")
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // Erstellt die DB in V1 und migriert schrittweise auf die aktuelle Version
        helper.createDatabase(TEST_DB, 1).close()

        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            TEST_DB
        ).addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7
        )
            .build().apply {
                openHelper.writableDatabase.close()
            }
    }
}
