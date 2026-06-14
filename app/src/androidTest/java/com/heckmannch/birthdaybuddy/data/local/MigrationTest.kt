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
    fun migrate5To9() {
        // 1. Create database in version 5 with test data
        helper.createDatabase(testDb, 5).apply {
            execSQL(
                "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, labels, giftIdeas, hasWhatsApp, hasSignal) " +
                        "VALUES ('5', 'key5', 'Max Mustermann', '1990-01-01', '[]', '[]', 0, 0)"
            )
            close()
        }

        // 2. Run migration through the chain (5 to 9) and validate
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            9,
            true,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9
        )

        // 3. Verify that the data is intact and new columns default to correct values
        val cursor = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key5'")
        assert(cursor.moveToFirst())
        assert(cursor.getString(cursor.getColumnIndex("fullName")) == "Max Mustermann")
        assert(cursor.getString(cursor.getColumnIndex("birthday")) == "1990-01-01")
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
        // Erstellt die DB in V5 und migriert schrittweise auf die aktuelle Version
        helper.createDatabase(testDb, 5).close()

        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            testDb
        ).addMigrations(
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
