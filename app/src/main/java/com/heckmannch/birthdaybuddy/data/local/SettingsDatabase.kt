package com.heckmannch.birthdaybuddy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LabelConfigEntity::class, NotificationRuleEntity::class, AppSettingsEntity::class, ContactUserData::class],
    version = 10,
    exportSchema = true
)
@TypeConverters(Converters::class, GiftIdeaConverters::class)
abstract class SettingsDatabase : RoomDatabase() {
    abstract fun labelConfigDao(): LabelConfigDao
    abstract fun notificationRuleDao(): NotificationRuleDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun contactUserDataDao(): ContactUserDataDao

    companion object {
        @Volatile
        private var INSTANCE: SettingsDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN otherEventsEnabled INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE contact_user_data ADD COLUMN spouseLookupKey TEXT")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN ignoredCouplePairs TEXT NOT NULL DEFAULT '[]'")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN birthdayCalendarColor INTEGER NOT NULL DEFAULT ${0xFFE91E63.toInt()}")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN anniversaryCalendarColor INTEGER NOT NULL DEFAULT ${0xFF9C27B0.toInt()}")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN nameDayCalendarColor INTEGER NOT NULL DEFAULT ${0xFFFF9800.toInt()}")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'SYSTEM'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN themeAmoled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN themeAccent TEXT NOT NULL DEFAULT 'SYSTEM'")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN themeContrast REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN labelsEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create a new table without themeContrast
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `app_settings_new` (
                        `id` INTEGER NOT NULL, 
                        `notificationsEnabled` INTEGER NOT NULL, 
                        `persistentNotifications` INTEGER NOT NULL, 
                        `onboardingCompleted` INTEGER NOT NULL, 
                        `lastSyncTimestamp` INTEGER NOT NULL, 
                        `calendarSyncEnabled` INTEGER NOT NULL, 
                        `calendarId` INTEGER, 
                        `otherEventsEnabled` INTEGER NOT NULL, 
                        `ignoredCouplePairs` TEXT NOT NULL, 
                        `birthdayCalendarColor` INTEGER NOT NULL, 
                        `anniversaryCalendarColor` INTEGER NOT NULL, 
                        `nameDayCalendarColor` INTEGER NOT NULL, 
                        `themeMode` TEXT NOT NULL, 
                        `themeAmoled` INTEGER NOT NULL, 
                        `themeAccent` TEXT NOT NULL, 
                        `labelsEnabled` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent()
                )

                // 2. Copy the data
                db.execSQL(
                    """
                    INSERT INTO app_settings_new (
                        id, notificationsEnabled, persistentNotifications, onboardingCompleted, 
                        lastSyncTimestamp, calendarSyncEnabled, calendarId, otherEventsEnabled, 
                        ignoredCouplePairs, birthdayCalendarColor, anniversaryCalendarColor, 
                        nameDayCalendarColor, themeMode, themeAmoled, themeAccent, labelsEnabled
                    )
                    SELECT 
                        id, notificationsEnabled, persistentNotifications, onboardingCompleted, 
                        lastSyncTimestamp, calendarSyncEnabled, calendarId, otherEventsEnabled, 
                        ignoredCouplePairs, birthdayCalendarColor, anniversaryCalendarColor, 
                        nameDayCalendarColor, themeMode, themeAmoled, themeAccent, labelsEnabled
                    FROM app_settings
                """.trimIndent()
                )

                // 3. Drop the old table
                db.execSQL("DROP TABLE app_settings")

                // 4. Rename the new table
                db.execSQL("ALTER TABLE app_settings_new RENAME TO app_settings")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE label_configs ADD COLUMN notificationsEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE label_configs ADD COLUMN showInWidget INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context): SettingsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SettingsDatabase::class.java,
                    "settings_database",
                )
                    .addMigrations(
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10
                    )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
