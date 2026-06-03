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
}
