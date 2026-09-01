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
 * Testet die Datenbank-Migrationen für [AppDatabase].
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
    fun migrate5To10() {
        // 1. Create database in version 5 with test data
        helper.createDatabase(testDb, 5).apply {
            execSQL(
                "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, labels, giftIdeas, hasWhatsApp, hasSignal) " +
                        "VALUES ('5', 'key5', 'Max Mustermann', '1990-01-01', '[]', '[]', 0, 0)"
            )
            close()
        }

        // 2. Run migration through the chain (5 to 10) and validate.
        // MIGRATION_5_6 und MIGRATION_6_7 sind manuell, 7->8, 8->9 und 9->10 werden als AutoMigrations automatisch ausgeführt.
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            10,
            true,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7
        )

        // 3. Verify that the data is intact and new columns default to correct values
        val cursor = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key5'")
        assert(cursor.moveToFirst())
        assert(cursor.getString(cursor.getColumnIndex("fullName")) == "Max Mustermann")
        assert(cursor.getString(cursor.getColumnIndex("birthday")) == "1990-01-01")
        assert(cursor.isNull(cursor.getColumnIndex("anniversary")))
        assert(cursor.isNull(cursor.getColumnIndex("nameDay")))
        assert(cursor.isNull(cursor.getColumnIndex("spouseLookupKey")))
        assert(cursor.getInt(cursor.getColumnIndex("isFavorite")) == 0)
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate7To10() {
        // 1. Create database in version 7 with test data
        helper.createDatabase(testDb, 7).apply {
            execSQL(
                "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, labels, giftIdeas, hasWhatsApp, hasSignal) " +
                        "VALUES ('4', 'key4', 'Hans Muster', '1985-05-05', '[]', '[]', 0, 0)"
            )
            close()
        }

        // 2. Run migration to version 10 and validate (AutoMigrations 7->8, 8->9 und 9->10)
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            10,
            true
        )

        // 3. Verify that the data is intact and all auto-migrated columns have correct default values
        val cursor = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key4'")
        assert(cursor.moveToFirst())
        assert(cursor.getString(cursor.getColumnIndex("fullName")) == "Hans Muster")
        assert(cursor.getString(cursor.getColumnIndex("birthday")) == "1985-05-05")
        assert(cursor.isNull(cursor.getColumnIndex("anniversary")))
        assert(cursor.isNull(cursor.getColumnIndex("nameDay")))
        assert(cursor.isNull(cursor.getColumnIndex("spouseLookupKey")))
        assert(cursor.getInt(cursor.getColumnIndex("isFavorite")) == 0)
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate8To10() {
        // 1. Create database in version 8 with test data
        helper.createDatabase(testDb, 8).apply {
            execSQL(
                "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, labels, giftIdeas, hasWhatsApp, hasSignal) " +
                        "VALUES ('3', 'key3', 'Erika Mustermann', '1992-02-02', '[]', '[]', 0, 0)"
            )
            close()
        }

        // 2. Run migration to version 10 and validate (AutoMigrations 8->9 und 9->10)
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            10,
            true
        )

        // 3. Verify that the data is intact and spouseLookupKey is null and isFavorite is 0 by default
        val cursor = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key3'")
        assert(cursor.moveToFirst())
        assert(cursor.getString(cursor.getColumnIndex("fullName")) == "Erika Mustermann")
        assert(cursor.getString(cursor.getColumnIndex("birthday")) == "1992-02-02")
        val spouseLookupKeyIndex = cursor.getColumnIndex("spouseLookupKey")
        assert(cursor.isNull(spouseLookupKeyIndex))
        val isFavoriteIndex = cursor.getColumnIndex("isFavorite")
        assert(cursor.getInt(isFavoriteIndex) == 0)
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // Erstellt die DB in V5 und migriert schrittweise auf die aktuelle Version
        helper.createDatabase(testDb, 5).close()

        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            testDb
        ).addMigrations(
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7
        )
            .build().apply {
                openHelper.writableDatabase.close()
            }
    }
}
