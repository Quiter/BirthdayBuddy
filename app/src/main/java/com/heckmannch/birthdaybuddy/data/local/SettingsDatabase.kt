package com.heckmannch.birthdaybuddy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [LabelConfig::class, NotificationRule::class, AppSettings::class, ContactUserData::class],
    version = 2,
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

        fun getDatabase(context: Context): SettingsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SettingsDatabase::class.java,
                    "settings_database",
                )
                    .fallbackToDestructiveMigration() // Hier erlaubt, da wir V1 starten
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
