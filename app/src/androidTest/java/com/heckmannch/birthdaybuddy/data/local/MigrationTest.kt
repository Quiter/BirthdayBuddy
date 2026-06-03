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
    private val testDb = "migration-test"

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
        helper.createDatabase(testDb, 1).apply {
            // Testdaten einfügen (V1-Schema)
            execSQL(
                "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, labels) " +
                        "VALUES ('1', 'key1', 'Max Mustermann', '1990-01-01', '[]')"
            )
            close()
        }

        // 2. Migration auf Version 2 ausführen und validieren
        // Wir übergeben die manuelle Migration, die wir testen wollen
        helper.runMigrationsAndValidate(testDb, 2, true, AppDatabase.MIGRATION_1_2)
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To9() {
        // 1. Datenbank in Version 1 erstellen mit Testdaten
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, labels) " +
                        "VALUES ('1', 'key1', 'Max Mustermann', '1990-01-01', '[]')"
            )
            close()
        }

        // 2. Migration über die gesamte Kette (1 bis 9) ausführen und validieren
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            9,
            true,
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9
        )

        // 3. Prüfen, ob die Daten intakt sind und neue Spalten default-Werte haben
        val cursor = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key1'")
        assert(cursor.moveToFirst())

        val nameIndex = cursor.getColumnIndex("fullName")
        val birthdayIndex = cursor.getColumnIndex("birthday")
        val phoneIndex = cursor.getColumnIndex("phoneNumber")
        val whatsappIndex = cursor.getColumnIndex("hasWhatsApp")
        val signalIndex = cursor.getColumnIndex("hasSignal")
        val anniversaryIndex = cursor.getColumnIndex("anniversary")
        val nameDayIndex = cursor.getColumnIndex("nameDay")
        val spouseLookupKeyIndex = cursor.getColumnIndex("spouseLookupKey")

        assert(cursor.getString(nameIndex) == "Max Mustermann")
        assert(cursor.getString(birthdayIndex) == "1990-01-01")
        assert(cursor.isNull(phoneIndex))
        assert(cursor.getInt(whatsappIndex) == 0)
        assert(cursor.getInt(signalIndex) == 0)
        assert(cursor.isNull(anniversaryIndex))
        assert(cursor.isNull(nameDayIndex))
        assert(cursor.isNull(spouseLookupKeyIndex))

        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To9() {
        // 1. Datenbank in Version 2 erstellen mit Testdaten
        helper.createDatabase(testDb, 2).apply {
            execSQL(
                "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, labels) " +
                        "VALUES ('2', 'key2', 'Erika Mustermann', '1992-02-02', '[]')"
            )
            close()
        }

        // 2. Migration über die Kette (2 bis 9) ausführen und validieren
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            9,
            true,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9
        )

        // 3. Prüfen, ob die Daten intakt sind
        val cursor = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key2'")
        assert(cursor.moveToFirst())
        assert(cursor.getString(cursor.getColumnIndex("fullName")) == "Erika Mustermann")
        assert(cursor.getString(cursor.getColumnIndex("birthday")) == "1992-02-02")
        assert(cursor.isNull(cursor.getColumnIndex("anniversary")))
        assert(cursor.isNull(cursor.getColumnIndex("nameDay")))
        assert(cursor.isNull(cursor.getColumnIndex("spouseLookupKey")))
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate8To9() {
        // 1. Create database in version 8 with test data
        helper.createDatabase(testDb, 8).apply {
            execSQL(
                "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, labels, giftIdeas, hasWhatsApp, hasSignal) " +
                        "VALUES ('3', 'key3', 'Erika Mustermann', '1992-02-02', '[]', '[]', 0, 0)"
            )
            close()
        }

        // 2. Run migration to version 9 and validate
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            9,
            true,
            AppDatabase.MIGRATION_8_9
        )

        // 3. Verify that the data is intact and spouseLookupKey is null by default
        val cursor = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key3'")
        assert(cursor.moveToFirst())
        assert(cursor.getString(cursor.getColumnIndex("fullName")) == "Erika Mustermann")
        assert(cursor.getString(cursor.getColumnIndex("birthday")) == "1992-02-02")
        val spouseLookupKeyIndex = cursor.getColumnIndex("spouseLookupKey")
        assert(cursor.isNull(spouseLookupKeyIndex))
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // Erstellt die DB in V1 und migriert schrittweise auf die aktuelle Version
        helper.createDatabase(testDb, 1).close()

        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            testDb
        ).addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9
        )
            .build().apply {
                openHelper.writableDatabase.close()
            }
    }
}
