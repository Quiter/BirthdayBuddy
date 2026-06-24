package com.heckmannch.birthdaybuddy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LabelConfig::class, NotificationRule::class, AppSettings::class, ContactUserData::class],
    version = 8,
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
                        MIGRATION_7_8
                    )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
