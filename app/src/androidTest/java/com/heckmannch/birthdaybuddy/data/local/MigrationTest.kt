package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Testet die Datenbank-Migrationen für [AppDatabase].
 *
 * Beinhaltet modulare, isolierte Tests für jeden Migrationsschritt (5->6, 6->7, 7->8, 8->9, 9->10, 10->11)
 * sowie End-to-End Migrationstests über mehrere Versionen hinweg.
 *
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

    private data class ColumnInfo(
        val name: String,
        val isNotNull: Boolean,
        val defaultValue: String?
    )

    private fun getColumnInfo(
        db: SupportSQLiteDatabase,
        tableName: String
    ): Map<String, ColumnInfo> {
        val map = mutableMapOf<String, ColumnInfo>()
        val cursor = db.query("PRAGMA table_info('$tableName')")
        while (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            val notNull = cursor.getInt(cursor.getColumnIndexOrThrow("notnull")) == 1
            val defaultValue = cursor.getString(cursor.getColumnIndexOrThrow("dflt_value"))
            map[name] = ColumnInfo(name, notNull, defaultValue)
        }
        cursor.close()
        return map
    }

    private fun getIndices(db: SupportSQLiteDatabase, tableName: String): Set<String> {
        val set = mutableSetOf<String>()
        val cursor = db.query("PRAGMA index_list('$tableName')")
        while (cursor.moveToNext()) {
            val nameIdx = cursor.getColumnIndex("name")
            if (nameIdx != -1) {
                set.add(cursor.getString(nameIdx))
            }
        }
        cursor.close()
        return set
    }

    private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean {
        val cursor = db.query(
            "SELECT count(*) FROM sqlite_master WHERE type='table' AND name = ?",
            arrayOf(tableName)
        )
        val exists = cursor.moveToFirst() && cursor.getInt(0) > 0
        cursor.close()
        return exists
    }

    // ==========================================
    // Isolierte Tests für jeden Migrationsschritt
    // ==========================================

    @Test
    @Throws(IOException::class)
    fun testMigration5To6() {
        // 1. Create database in version 5 with test data
        helper.createDatabase(testDb, 5).apply {
            execSQL(
                "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, labels, giftIdeas, hasWhatsApp, hasSignal) " +
                        "VALUES ('5', 'key5_test', 'Max Mustermann', '1990-01-01', '[]', '[]', 0, 0)"
            )
            close()
        }

        // 2. Run migration 5 -> 6 (MIGRATION_5_6)
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            6,
            true,
            AppDatabase.MIGRATION_5_6
        )

        // 3. Verify data integrity
        val cursor = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key5_test'")
        assertTrue(cursor.moveToFirst())
        assertEquals("Max Mustermann", cursor.getString(cursor.getColumnIndexOrThrow("fullName")))
        assertEquals("1990-01-01", cursor.getString(cursor.getColumnIndexOrThrow("birthday")))
        cursor.close()

        // 4. Verify birthday and giftIdeas column nullability (notNull == false)
        val columns = getColumnInfo(migratedDb, "contacts")
        assertTrue(columns.containsKey("birthday"))
        assertFalse(columns["birthday"]!!.isNotNull)
        assertTrue(columns.containsKey("giftIdeas"))
        assertFalse(columns["giftIdeas"]!!.isNotNull)

        // 5. Verify index_contacts_birthday exists
        val indices = getIndices(migratedDb, "contacts")
        assertTrue(indices.contains("index_contacts_birthday"))
        assertTrue(indices.contains("index_contacts_lookupKey"))

        // 6. Verify that inserting a record with NULL birthday succeeds in version 6
        migratedDb.execSQL(
            "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, labels, giftIdeas, hasWhatsApp, hasSignal) " +
                    "VALUES ('6', 'key6_null', 'Null Birthday Contact', NULL, '[]', '[]', 0, 0)"
        )
        val nullCursor = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key6_null'")
        assertTrue(nullCursor.moveToFirst())
        assertTrue(nullCursor.isNull(nullCursor.getColumnIndexOrThrow("birthday")))
        nullCursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun testMigration6To7() {
        // 1. Create database in version 6 with legacy tables and contacts (NULL and non-NULL giftIdeas)
        helper.createDatabase(testDb, 6).apply {
            execSQL(
                "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, labels, giftIdeas, hasWhatsApp, hasSignal) " +
                        "VALUES ('6', 'key6_null_gift', 'Anna Schmidt', '1988-08-08', '[]', NULL, 1, 0)"
            )
            execSQL(
                "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, labels, giftIdeas, hasWhatsApp, hasSignal) " +
                        "VALUES ('7', 'key6_with_gift', 'Bernd Bauer', '1992-12-12', '[]', '[\"Buch\"]', 0, 1)"
            )
            execSQL("INSERT INTO label_configs (name, isHiddenFromFilter, isIgnored, isSystem) VALUES ('Familie', 0, 0, 1)")
            execSQL("INSERT INTO notification_rules (daysBefore, hour, minute) VALUES (1, 9, 0)")
            execSQL("INSERT INTO app_settings (id, notificationsEnabled, persistentNotifications, onboardingCompleted, lastSyncTimestamp) VALUES (1, 1, 0, 1, 1000)")
            execSQL("INSERT INTO pending_notifications (contactLookupKeys, daysBefore, year, isDone, dismissCount) VALUES ('[\"key6_null_gift\"]', 1, 2026, 0, 0)")
            close()
        }

        // 2. Run migration 6 -> 7 (MIGRATION_6_7)
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            7,
            true,
            AppDatabase.MIGRATION_6_7
        )

        // 3. Verify legacy tables are dropped
        assertFalse(tableExists(migratedDb, "label_configs"))
        assertFalse(tableExists(migratedDb, "notification_rules"))
        assertFalse(tableExists(migratedDb, "app_settings"))

        // 4. Verify contacts and pending_notifications still exist
        assertTrue(tableExists(migratedDb, "contacts"))
        assertTrue(tableExists(migratedDb, "pending_notifications"))

        // 5. Verify giftIdeas is NOT NULL
        val columns = getColumnInfo(migratedDb, "contacts")
        assertTrue(columns.containsKey("giftIdeas"))
        assertTrue(columns["giftIdeas"]!!.isNotNull)

        // 6. Verify data integrity (NULL giftIdeas coalesced to '[]' and non-NULL preserved)
        val cursor1 = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key6_null_gift'")
        assertTrue(cursor1.moveToFirst())
        assertEquals("Anna Schmidt", cursor1.getString(cursor1.getColumnIndexOrThrow("fullName")))
        assertEquals("[]", cursor1.getString(cursor1.getColumnIndexOrThrow("giftIdeas")))
        cursor1.close()

        val cursor2 = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key6_with_gift'")
        assertTrue(cursor2.moveToFirst())
        assertEquals("Bernd Bauer", cursor2.getString(cursor2.getColumnIndexOrThrow("fullName")))
        assertEquals("[\"Buch\"]", cursor2.getString(cursor2.getColumnIndexOrThrow("giftIdeas")))
        cursor2.close()

        val notifCursor =
            migratedDb.query("SELECT * FROM pending_notifications WHERE contactLookupKeys = '[\"key6_null_gift\"]'")
        assertTrue(notifCursor.moveToFirst())
        assertEquals(2026, notifCursor.getInt(notifCursor.getColumnIndexOrThrow("year")))
        notifCursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun testMigration7To8() {
        // 1. Create database in version 7 with test data
        helper.createDatabase(testDb, 7).apply {
            execSQL(
                "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, labels, giftIdeas, hasWhatsApp, hasSignal) " +
                        "VALUES ('7', 'key7_test', 'Clara Meier', '1995-05-15', '[]', '[]', 0, 0)"
            )
            close()
        }

        // 2. Run auto migration 7 -> 8
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            8,
            true
        )

        // 3. Verify data integrity and new columns are NULL for existing record
        val cursor = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key7_test'")
        assertTrue(cursor.moveToFirst())
        assertEquals("Clara Meier", cursor.getString(cursor.getColumnIndexOrThrow("fullName")))
        assertEquals("1995-05-15", cursor.getString(cursor.getColumnIndexOrThrow("birthday")))
        val anniversaryIdx = cursor.getColumnIndexOrThrow("anniversary")
        val nameDayIdx = cursor.getColumnIndexOrThrow("nameDay")
        assertTrue(cursor.isNull(anniversaryIdx))
        assertTrue(cursor.isNull(nameDayIdx))
        cursor.close()

        // 4. Verify columns anniversary and nameDay are nullable
        val columns = getColumnInfo(migratedDb, "contacts")
        assertTrue(columns.containsKey("anniversary"))
        assertFalse(columns["anniversary"]!!.isNotNull)
        assertTrue(columns.containsKey("nameDay"))
        assertFalse(columns["nameDay"]!!.isNotNull)

        // 5. Verify new record can be inserted with anniversary and nameDay
        migratedDb.execSQL(
            "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, anniversary, nameDay, labels, giftIdeas, hasWhatsApp, hasSignal) " +
                    "VALUES ('8', 'key8_new', 'David Koch', '1990-10-10', '2018-06-20', '2026-03-15', '[]', '[]', 0, 0)"
        )
        val newCursor = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key8_new'")
        assertTrue(newCursor.moveToFirst())
        assertEquals(
            "2018-06-20",
            newCursor.getString(newCursor.getColumnIndexOrThrow("anniversary"))
        )
        assertEquals("2026-03-15", newCursor.getString(newCursor.getColumnIndexOrThrow("nameDay")))
        newCursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun testMigration8To9() {
        // 1. Create database in version 8 with test data
        helper.createDatabase(testDb, 8).apply {
            execSQL(
                "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, anniversary, nameDay, labels, giftIdeas, hasWhatsApp, hasSignal) " +
                        "VALUES ('8', 'key8_test', 'Elena Weber', '1991-04-04', '2019-09-19', '2026-05-21', '[]', '[]', 0, 0)"
            )
            close()
        }

        // 2. Run auto migration 8 -> 9
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            9,
            true
        )

        // 3. Verify data integrity and spouseLookupKey is NULL on migrated record
        val cursor = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key8_test'")
        assertTrue(cursor.moveToFirst())
        assertEquals("Elena Weber", cursor.getString(cursor.getColumnIndexOrThrow("fullName")))
        assertEquals("1991-04-04", cursor.getString(cursor.getColumnIndexOrThrow("birthday")))
        assertEquals("2019-09-19", cursor.getString(cursor.getColumnIndexOrThrow("anniversary")))
        assertEquals("2026-05-21", cursor.getString(cursor.getColumnIndexOrThrow("nameDay")))
        val spouseIdx = cursor.getColumnIndexOrThrow("spouseLookupKey")
        assertTrue(cursor.isNull(spouseIdx))
        cursor.close()

        // 4. Verify spouseLookupKey is nullable
        val columns = getColumnInfo(migratedDb, "contacts")
        assertTrue(columns.containsKey("spouseLookupKey"))
        assertFalse(columns["spouseLookupKey"]!!.isNotNull)

        // 5. Verify new record can be inserted with spouseLookupKey
        migratedDb.execSQL(
            "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, anniversary, nameDay, spouseLookupKey, labels, giftIdeas, hasWhatsApp, hasSignal) " +
                    "VALUES ('9', 'key9_new', 'Felix Jung', '1987-07-07', NULL, NULL, 'keySpouse', '[]', '[]', 0, 0)"
        )
        val newCursor = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key9_new'")
        assertTrue(newCursor.moveToFirst())
        assertEquals(
            "keySpouse",
            newCursor.getString(newCursor.getColumnIndexOrThrow("spouseLookupKey"))
        )
        newCursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun testMigration9To10() {
        // 1. Create database in version 9 with test data
        helper.createDatabase(testDb, 9).apply {
            execSQL(
                "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, anniversary, nameDay, spouseLookupKey, labels, giftIdeas, hasWhatsApp, hasSignal) " +
                        "VALUES ('9', 'key9_test', 'Greta Fuchs', '1993-11-11', '2021-01-01', '2026-07-13', 'keySpouse9', '[]', '[]', 0, 0)"
            )
            close()
        }

        // 2. Run auto migration 9 -> 10
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            10,
            true
        )

        // 3. Verify data integrity and isFavorite has default value 0
        val cursor = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key9_test'")
        assertTrue(cursor.moveToFirst())
        assertEquals("Greta Fuchs", cursor.getString(cursor.getColumnIndexOrThrow("fullName")))
        assertEquals("1993-11-11", cursor.getString(cursor.getColumnIndexOrThrow("birthday")))
        assertEquals("2021-01-01", cursor.getString(cursor.getColumnIndexOrThrow("anniversary")))
        assertEquals("2026-07-13", cursor.getString(cursor.getColumnIndexOrThrow("nameDay")))
        assertEquals(
            "keySpouse9",
            cursor.getString(cursor.getColumnIndexOrThrow("spouseLookupKey"))
        )
        val isFavoriteIdx = cursor.getColumnIndexOrThrow("isFavorite")
        assertEquals(0, cursor.getInt(isFavoriteIdx))
        cursor.close()

        // 4. Verify isFavorite column is NOT NULL with default value '0'
        val columns = getColumnInfo(migratedDb, "contacts")
        assertTrue(columns.containsKey("isFavorite"))
        assertTrue(columns["isFavorite"]!!.isNotNull)
        assertEquals("0", columns["isFavorite"]!!.defaultValue)

        // 5. Verify new record can be inserted with isFavorite = 1
        migratedDb.execSQL(
            "INSERT INTO contacts (contactId, lookupKey, fullName, birthday, anniversary, nameDay, spouseLookupKey, isFavorite, labels, giftIdeas, hasWhatsApp, hasSignal) " +
                    "VALUES ('10', 'key10_fav', 'Hannah Berg', '1999-09-09', NULL, NULL, NULL, 1, '[]', '[]', 0, 0)"
        )
        val favCursor = migratedDb.query("SELECT * FROM contacts WHERE lookupKey = 'key10_fav'")
        assertTrue(favCursor.moveToFirst())
        assertEquals(1, favCursor.getInt(favCursor.getColumnIndexOrThrow("isFavorite")))
        favCursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun testMigration10To11() {
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
        assertTrue(contactCursor.moveToFirst())
        assertEquals(
            "Julia Test",
            contactCursor.getString(contactCursor.getColumnIndexOrThrow("fullName"))
        )
        assertEquals(
            "1995-03-20",
            contactCursor.getString(contactCursor.getColumnIndexOrThrow("birthday"))
        )
        assertEquals(
            "2020-08-15",
            contactCursor.getString(contactCursor.getColumnIndexOrThrow("anniversary"))
        )
        assertEquals(
            "2020-04-12",
            contactCursor.getString(contactCursor.getColumnIndexOrThrow("nameDay"))
        )
        assertEquals(1, contactCursor.getInt(contactCursor.getColumnIndexOrThrow("isFavorite")))
        assertEquals(
            "keySpouse",
            contactCursor.getString(contactCursor.getColumnIndexOrThrow("spouseLookupKey"))
        )
        contactCursor.close()

        // 4. Verify data integrity in pending_notifications table
        val notifCursor =
            migratedDb.query("SELECT * FROM pending_notifications WHERE year = 2026 AND daysBefore = 3")
        assertTrue(notifCursor.moveToFirst())
        assertEquals(
            "[\"key10\"]",
            notifCursor.getString(notifCursor.getColumnIndexOrThrow("contactLookupKeys"))
        )
        assertEquals(0, notifCursor.getInt(notifCursor.getColumnIndexOrThrow("isDone")))
        assertEquals(1, notifCursor.getInt(notifCursor.getColumnIndexOrThrow("dismissCount")))
        notifCursor.close()

        // 5. Verify index creation on contacts table
        val contactIndices = getIndices(migratedDb, "contacts")
        assertTrue(contactIndices.contains("index_contacts_anniversary"))

        // 6. Verify index creation on pending_notifications table
        val notifIndices = getIndices(migratedDb, "pending_notifications")
        assertTrue(notifIndices.contains("index_pending_notifications_isDone"))
        assertTrue(notifIndices.contains("index_pending_notifications_year_daysBefore"))
    }

    // ==========================================
    // End-to-End Tests über mehrere Versionen
    // ==========================================

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
        assertTrue(cursor.moveToFirst())
        assertEquals("Max Mustermann", cursor.getString(cursor.getColumnIndexOrThrow("fullName")))
        assertEquals("1990-01-01", cursor.getString(cursor.getColumnIndexOrThrow("birthday")))
        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("anniversary")))
        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("nameDay")))
        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("spouseLookupKey")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("isFavorite")))
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
        assertTrue(cursor.moveToFirst())
        assertEquals("Hans Muster", cursor.getString(cursor.getColumnIndexOrThrow("fullName")))
        assertEquals("1985-05-05", cursor.getString(cursor.getColumnIndexOrThrow("birthday")))
        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("anniversary")))
        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("nameDay")))
        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("spouseLookupKey")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("isFavorite")))
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
        assertTrue(cursor.moveToFirst())
        assertEquals("Erika Mustermann", cursor.getString(cursor.getColumnIndexOrThrow("fullName")))
        assertEquals("1992-02-02", cursor.getString(cursor.getColumnIndexOrThrow("birthday")))
        val spouseLookupKeyIndex = cursor.getColumnIndexOrThrow("spouseLookupKey")
        assertTrue(cursor.isNull(spouseLookupKeyIndex))
        val isFavoriteIndex = cursor.getColumnIndexOrThrow("isFavorite")
        assertEquals(0, cursor.getInt(isFavoriteIndex))
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate10To11() {
        testMigration10To11()
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
