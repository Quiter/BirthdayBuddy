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

        val userDataCursor =
            migratedDb.query("SELECT * FROM contact_user_data WHERE lookupKey = 'k1'")
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

    @Test
    @Throws(IOException::class)
    fun migrate5To6() {
        // 1. Create database in version 5
        helper.createDatabase(testDb, 5).apply {
            execSQL(
                "INSERT INTO app_settings (id, notificationsEnabled, persistentNotifications, onboardingCompleted, lastSyncTimestamp, calendarSyncEnabled, calendarId, otherEventsEnabled, ignoredCouplePairs, birthdayCalendarColor, anniversaryCalendarColor, nameDayCalendarColor) " +
                        "VALUES (0, 1, 1, 1, 123456789, 0, NULL, 0, '[]', -1564957, -6543440, -26624)"
            )
            close()
        }

        // 2. Run migration to version 6 and validate
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            6,
            true,
            SettingsDatabase.MIGRATION_5_6
        )

        // 3. Verify columns and default values
        val settingsCursor = migratedDb.query("SELECT * FROM app_settings WHERE id = 0")
        assert(settingsCursor.moveToFirst())

        val themeModeIdx = settingsCursor.getColumnIndex("themeMode")
        val themeAmoledIdx = settingsCursor.getColumnIndex("themeAmoled")
        val themeAccentIdx = settingsCursor.getColumnIndex("themeAccent")

        assert(settingsCursor.getString(themeModeIdx) == "SYSTEM")
        assert(settingsCursor.getInt(themeAmoledIdx) == 0) // Boolean represented as 0/1 in SQLite
        assert(settingsCursor.getString(themeAccentIdx) == "SYSTEM")

        settingsCursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate6To7() {
        // 1. Create database in version 6
        helper.createDatabase(testDb, 6).apply {
            execSQL(
                "INSERT INTO app_settings (id, notificationsEnabled, persistentNotifications, onboardingCompleted, lastSyncTimestamp, calendarSyncEnabled, calendarId, otherEventsEnabled, ignoredCouplePairs, birthdayCalendarColor, anniversaryCalendarColor, nameDayCalendarColor, themeMode, themeAmoled, themeAccent) " +
                        "VALUES (0, 1, 1, 1, 123456789, 0, NULL, 0, '[]', -1564957, -6543440, -26624, 'SYSTEM', 0, 'SYSTEM')"
            )
            close()
        }

        // 2. Run migration to version 7 and validate
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            7,
            true,
            SettingsDatabase.MIGRATION_6_7
        )

        // 3. Verify columns and default values
        val settingsCursor = migratedDb.query("SELECT * FROM app_settings WHERE id = 0")
        assert(settingsCursor.moveToFirst())

        val themeContrastIdx = settingsCursor.getColumnIndex("themeContrast")
        assert(settingsCursor.getDouble(themeContrastIdx) == 0.0)

        settingsCursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate7To8() {
        // 1. Create database in version 7
        helper.createDatabase(testDb, 7).apply {
            execSQL(
                "INSERT INTO app_settings (id, notificationsEnabled, persistentNotifications, onboardingCompleted, lastSyncTimestamp, calendarSyncEnabled, calendarId, otherEventsEnabled, ignoredCouplePairs, birthdayCalendarColor, anniversaryCalendarColor, nameDayCalendarColor, themeMode, themeAmoled, themeAccent, themeContrast) " +
                        "VALUES (0, 1, 1, 1, 123456789, 0, NULL, 0, '[]', -1564957, -6543440, -26624, 'SYSTEM', 0, 'SYSTEM', 0.0)"
            )
            close()
        }

        // 2. Run migration to version 8 and validate
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            8,
            true,
            SettingsDatabase.MIGRATION_7_8
        )

        // 3. Verify columns and default values
        val settingsCursor = migratedDb.query("SELECT * FROM app_settings WHERE id = 0")
        assert(settingsCursor.moveToFirst())

        val labelsEnabledIdx = settingsCursor.getColumnIndex("labelsEnabled")
        assert(settingsCursor.getInt(labelsEnabledIdx) == 1) // represented as 1 (true) in SQLite

        settingsCursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate8To9() {
        // 1. Create database in version 8
        helper.createDatabase(testDb, 8).apply {
            execSQL(
                "INSERT INTO app_settings (id, notificationsEnabled, persistentNotifications, onboardingCompleted, lastSyncTimestamp, calendarSyncEnabled, calendarId, otherEventsEnabled, ignoredCouplePairs, birthdayCalendarColor, anniversaryCalendarColor, nameDayCalendarColor, themeMode, themeAmoled, themeAccent, themeContrast, labelsEnabled) " +
                        "VALUES (0, 1, 1, 1, 123456789, 0, NULL, 0, '[]', -1564957, -6543440, -26624, 'SYSTEM', 0, 'SYSTEM', 0.0, 1)"
            )
            close()
        }

        // 2. Run migration to version 9 and validate
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            9,
            true,
            SettingsDatabase.MIGRATION_8_9
        )

        // 3. Verify columns and default values
        val settingsCursor = migratedDb.query("SELECT * FROM app_settings WHERE id = 0")
        assert(settingsCursor.moveToFirst())

        // Verify that themeContrast column does not exist anymore (-1)
        val themeContrastIdx = settingsCursor.getColumnIndex("themeContrast")
        assert(themeContrastIdx == -1)

        // Verify that labelsEnabled still exists and is 1
        val labelsEnabledIdx = settingsCursor.getColumnIndex("labelsEnabled")
        assert(settingsCursor.getInt(labelsEnabledIdx) == 1)

        settingsCursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate9To10() {
        // 1. Create database in version 9
        helper.createDatabase(testDb, 9).apply {
            execSQL(
                "INSERT INTO label_configs (name, isHiddenFromFilter, isIgnored, isSystem) " +
                        "VALUES ('Family', 0, 0, 0)"
            )
            close()
        }

        // 2. Run migration to version 10 and validate
        val migratedDb = helper.runMigrationsAndValidate(
            testDb,
            10,
            true,
            SettingsDatabase.MIGRATION_9_10
        )

        // 3. Verify columns and default values
        val cursor = migratedDb.query("SELECT * FROM label_configs WHERE name = 'Family'")
        assert(cursor.moveToFirst())

        val notificationsEnabledIdx = cursor.getColumnIndex("notificationsEnabled")
        val showInWidgetIdx = cursor.getColumnIndex("showInWidget")

        assert(cursor.getInt(notificationsEnabledIdx) == 1)
        assert(cursor.getInt(showInWidgetIdx) == 1)

        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // 1. Create database in version 2
        helper.createDatabase(testDb, 2).close()

        // 2. Run all migrations to version 10 and validate
        helper.runMigrationsAndValidate(
            testDb,
            10,
            true,
            SettingsDatabase.MIGRATION_2_3,
            SettingsDatabase.MIGRATION_3_4,
            SettingsDatabase.MIGRATION_4_5,
            SettingsDatabase.MIGRATION_5_6,
            SettingsDatabase.MIGRATION_6_7,
            SettingsDatabase.MIGRATION_7_8,
            SettingsDatabase.MIGRATION_8_9,
            SettingsDatabase.MIGRATION_9_10
        )
    }
}

