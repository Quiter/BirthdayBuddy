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
 * 
 * Hilt-Module sind "Fabriken", die definieren, wie Instanzen von Klassen erstellt werden,
 * die nicht direkt annotiert werden können (wie z.B. Bibliotheksklassen wie Room).
 */
@Module
@InstallIn(SingletonComponent::class) // SingletonComponent bedeutet: Die Instanzen leben so lange wie die App.
object DatabaseModule {

    /**
     * Stellt die Singleton-Instanz der Datenbank bereit.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BirthdayDatabase {
        return BirthdayDatabase.getDatabase(context)
    }

    /**
     * Stellt das DAO bereit. 
     * Da wir die Datenbank bereits oben bereitstellen, kann Hilt diese hier automatisch einsetzen.
     */
    @Provides
    fun provideBirthdayDao(database: BirthdayDatabase): BirthdayDao {
        return database.birthdayDao()
    }
}

/**
 * Modul zur Konfiguration des Hilt-Workers.
 * 
 * Dies ist notwendig, damit WorkManager und Hilt zusammenarbeiten können.
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
