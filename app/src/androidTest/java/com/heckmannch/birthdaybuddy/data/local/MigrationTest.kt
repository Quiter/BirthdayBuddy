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
    fun migrate10To11() {
        // 1. Create database in version 10 with test data in both tables
        helper.createDatabase(testDb, 10).apply {
            execSQL(
                "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, anniversary, nameDay, imageUri, phoneNumber, isFavorite, hasWhatsApp, hasSignal, labels, giftIdeas, spouseLookupKey) " +
                        "VALUES ('10', 'key10', 'Julia Test', '1995-03-20', '2020-08-15', '2020-04-12', 'content://test/img.jpg', '+4912345678', 1, 1, 0, '[]', '[]', 'keySpouse')"
            )
            execSQL(
                "INSERT INTO pending_notifications (contactLookupKeys, daysBefore, year, isDone, dismissCount) " +
                        "VALUES ('[\"key10\"]', 3, 2026, 0, 1)"
            )
            close()
        }

        // 2. Run migration to version 11 and validate schema & indices (AutoMigration 10->11)
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            11,
            true
        )

        // 3. Verify data integrity in contacts table
        val contactCursor = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key10'")
        assert(contactCursor.moveToFirst())
        assert(contactCursor.getString(contactCursor.getColumnIndex("fullName")) == "Julia Test")
        assert(contactCursor.getString(contactCursor.getColumnIndex("birthday")) == "1995-03-20")
        assert(contactCursor.getString(contactCursor.getColumnIndex("anniversary")) == "2020-08-15")
        assert(contactCursor.getString(contactCursor.getColumnIndex("nameDay")) == "2020-04-12")
        assert(contactCursor.getInt(contactCursor.getColumnIndex("isFavorite")) == 1)
        assert(contactCursor.getString(contactCursor.getColumnIndex("spouseLookupKey")) == "keySpouse")
        contactCursor.close()

        // 4. Verify data integrity in pending_notifications table
        val notifCursor = migratedDb.query("SELECT * FROM pending_notifications WHERE year = 2026 AND daysBefore = 3")
        assert(notifCursor.moveToFirst())
        assert(notifCursor.getString(notifCursor.getColumnIndex("contactLookupKeys")) == "[\"key10\"]")
        assert(notifCursor.getInt(notifCursor.getColumnIndex("isDone")) == 0)
        assert(notifCursor.getInt(notifCursor.getColumnIndex("dismissCount")) == 1)
        notifCursor.close()

        // 5. Verify index creation on contacts table
        val contactIndices = mutableListOf<String>()
        val contactIndexCursor = migratedDb.query("PRAGMA index_list('contacts')")
        while (contactIndexCursor.moveToNext()) {
            val nameIdx = contactIndexCursor.getColumnIndex("name")
            if (nameIdx != -1) {
                contactIndices.add(contactIndexCursor.getString(nameIdx))
            }
        }
        contactIndexCursor.close()
        assert(contactIndices.contains("index_contacts_anniversary"))

        // 6. Verify index creation on pending_notifications table
        val notifIndices = mutableListOf<String>()
        val notifIndexCursor = migratedDb.query("PRAGMA index_list('pending_notifications')")
        while (notifIndexCursor.moveToNext()) {
            val nameIdx = notifIndexCursor.getColumnIndex("name")
            if (nameIdx != -1) {
                notifIndices.add(notifIndexCursor.getString(nameIdx))
            }
        }
        notifIndexCursor.close()
        assert(notifIndices.contains("index_pending_notifications_isDone"))
        assert(notifIndices.contains("index_pending_notifications_year_daysBefore"))
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
