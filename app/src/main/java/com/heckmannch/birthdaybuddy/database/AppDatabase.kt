package com.heckmannch.birthdaybuddy.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.AutoMigration

@Database(
    entities = [Contact::class, LabelConfig::class, NotificationRule::class, PendingNotification::class, AppSettings::class],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ],
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun labelConfigDao(): LabelConfigDao
    abstract fun notificationRuleDao(): NotificationRuleDao
    abstract fun pendingNotificationDao(): PendingNotificationDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "birthday_database",
                )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
