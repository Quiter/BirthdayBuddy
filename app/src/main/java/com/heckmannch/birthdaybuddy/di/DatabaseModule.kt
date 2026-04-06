package com.heckmannch.birthdaybuddy.di

import android.content.Context
import com.heckmannch.birthdaybuddy.data.local.BirthdayDao
import com.heckmannch.birthdaybuddy.data.local.BirthdayDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt-Modul zur Bereitstellung von Datenbank-bezogenen Abhängigkeiten.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BirthdayDatabase {
        return BirthdayDatabase.getDatabase(context)
    }

    @Provides
    fun provideBirthdayDao(database: BirthdayDatabase): BirthdayDao {
        return database.birthdayDao()
    }
}
