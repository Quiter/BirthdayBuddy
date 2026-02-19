package com.heckmannch.birthdaybuddy.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.heckmannch.birthdaybuddy.model.BirthdayContact

@Database(entities = [BirthdayContact::class], version = 1, exportSchema = false)
@TypeConverters(DataConverters::class)
abstract class BirthdayDatabase : RoomDatabase() {
    abstract fun birthdayDao(): BirthdayDao

    companion object {
        @Volatile
        private var INSTANCE: BirthdayDatabase? = null

        fun getDatabase(context: Context): BirthdayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BirthdayDatabase::class.java,
                    "birthday_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
