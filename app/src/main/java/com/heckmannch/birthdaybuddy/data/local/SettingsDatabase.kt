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
    version = 3,
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

        fun getDatabase(context: Context): SettingsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SettingsDatabase::class.java,
                    "settings_database",
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration(true) // Hier erlaubt, da wir V1 starten
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
