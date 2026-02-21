package com.heckmannch.birthdaybuddy.data

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
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

/**
 * Modul zur Konfiguration des Hilt-Workers.
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {
    @Provides
    @Singleton
    fun provideWorkManagerConfiguration(workerFactory: HiltWorkerFactory): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}
