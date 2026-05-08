package com.heckmannch.birthdaybuddy2.di

import android.content.Context
import com.heckmannch.birthdaybuddy2.database.AppDatabase
import com.heckmannch.birthdaybuddy2.database.ContactDao
import com.heckmannch.birthdaybuddy2.database.LabelConfigDao
import com.heckmannch.birthdaybuddy2.repository.ContactRepository
import com.heckmannch.birthdaybuddy2.repository.PreferenceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideContactDao(database: AppDatabase): ContactDao {
        return database.contactDao()
    }

    @Provides
    fun provideLabelConfigDao(database: AppDatabase): LabelConfigDao {
        return database.labelConfigDao()
    }

    @Provides
    @Singleton
    fun provideContactRepository(
        @ApplicationContext context: Context,
        contactDao: ContactDao,
        labelConfigDao: LabelConfigDao
    ): ContactRepository {
        return ContactRepository(context, contactDao, labelConfigDao)
    }

    @Provides
    @Singleton
    fun providePreferenceRepository(@ApplicationContext context: Context): PreferenceRepository {
        return PreferenceRepository(context)
    }
}
