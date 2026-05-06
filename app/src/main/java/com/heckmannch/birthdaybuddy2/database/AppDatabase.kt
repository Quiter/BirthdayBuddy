package com.heckmannch.birthdaybuddy2.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Contact::class, LabelConfig::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun labelConfigDao(): LabelConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "birthday_database",
                )
                .fallbackToDestructiveMigration(true) // Löscht die DB bei Versionsänderung (behebt den Absturz)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
