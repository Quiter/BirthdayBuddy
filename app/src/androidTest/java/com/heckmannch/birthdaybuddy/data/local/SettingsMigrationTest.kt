package com.heckmannch.birthdaybuddy.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class SettingsMigrationTest {
    private val testDb = "settings-migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SettingsDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate2To3() {
        // 1. Create database in version 2
        helper.createDatabase(testDb, 2).apply {
            execSQL(
                "INSERT INTO app_settings (id, notificationsEnabled, persistentNotifications, onboardingCompleted, lastSyncTimestamp, calendarSyncEnabled, calendarId) " +
                        "VALUES (0, 1, 1, 1, 123456789, 0, NULL)"
            )
            close()
        }

        // 2. Run migration to version 3 and validate
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            3,
            true,
            SettingsDatabase.MIGRATION_2_3
        )

        // 3. Verify that data is intact and new column has the default value 0 (false)
        val cursor = migratedDb.query("SELECT * FROM app_settings WHERE id = 0")
        assert(cursor.moveToFirst())

        val notificationsIndex = cursor.getColumnIndex("notificationsEnabled")
        val otherEventsIndex = cursor.getColumnIndex("otherEventsEnabled")

        assert(cursor.getInt(notificationsIndex) == 1)
        assert(cursor.getInt(otherEventsIndex) == 0) // Should default to false/0

        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        // 1. Create database in version 3
        helper.createDatabase(testDb, 3).apply {
            execSQL(
                "INSERT INTO app_settings (id, notificationsEnabled, persistentNotifications, onboardingCompleted, lastSyncTimestamp, calendarSyncEnabled, calendarId, otherEventsEnabled) " +
                        "VALUES (0, 1, 1, 1, 123456789, 0, NULL, 0)"
            )
            execSQL(
                "INSERT INTO contact_user_data (lookupKey, giftIdeas) " +
                        "VALUES ('k1', '[]')"
            )
            close()
        }

        // 2. Run migration to version 4 and validate
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            4,
            true,
            SettingsDatabase.MIGRATION_3_4
        )

        // 3. Verify columns and default values
        val settingsCursor = migratedDb.query("SELECT * FROM app_settings WHERE id = 0")
        assert(settingsCursor.moveToFirst())
        val ignoredCouplePairsIndex = settingsCursor.getColumnIndex("ignoredCouplePairs")
        assert(settingsCursor.getString(ignoredCouplePairsIndex) == "[]")
        settingsCursor.close()

        val userDataCursor = migratedDb.query("SELECT * FROM contact_user_data WHERE lookupKey = 'k1'")
        assert(userDataCursor.moveToFirst())
        val spouseLookupKeyIndex = userDataCursor.getColumnIndex("spouseLookupKey")
        assert(userDataCursor.isNull(spouseLookupKeyIndex))
        userDataCursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate4To5() {
        // 1. Create database in version 4
        helper.createDatabase(testDb, 4).apply {
            execSQL(
                "INSERT INTO app_settings (id, notificationsEnabled, persistentNotifications, onboardingCompleted, lastSyncTimestamp, calendarSyncEnabled, calendarId, otherEventsEnabled, ignoredCouplePairs) " +
                        "VALUES (0, 1, 1, 1, 123456789, 0, NULL, 0, '[]')"
            )
            close()
        }

        // 2. Run migration to version 5 and validate
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            5,
            true,
            SettingsDatabase.MIGRATION_4_5
        )

        // 3. Verify columns and default values
        val settingsCursor = migratedDb.query("SELECT * FROM app_settings WHERE id = 0")
        assert(settingsCursor.moveToFirst())

        val birthdayColorIdx = settingsCursor.getColumnIndex("birthdayCalendarColor")
        val anniversaryColorIdx = settingsCursor.getColumnIndex("anniversaryCalendarColor")
        val nameDayColorIdx = settingsCursor.getColumnIndex("nameDayCalendarColor")

        assert(settingsCursor.getInt(birthdayColorIdx) == 0xFFE91E63.toInt())
        assert(settingsCursor.getInt(anniversaryColorIdx) == 0xFF9C27B0.toInt())
        assert(settingsCursor.getInt(nameDayColorIdx) == 0xFFFF9800.toInt())

        settingsCursor.close()
    }
}

