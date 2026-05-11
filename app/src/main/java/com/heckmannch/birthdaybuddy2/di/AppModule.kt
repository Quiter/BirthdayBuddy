package com.heckmannch.birthdaybuddy2.di

import android.content.Context
import com.heckmannch.birthdaybuddy2.database.AppDatabase
import com.heckmannch.birthdaybuddy2.database.ContactDao
import com.heckmannch.birthdaybuddy2.database.LabelConfigDao
import com.heckmannch.birthdaybuddy2.database.NotificationRuleDao
import com.heckmannch.birthdaybuddy2.database.PendingNotificationDao
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
    fun provideNotificationRuleDao(database: AppDatabase): NotificationRuleDao {
        return database.notificationRuleDao()
    }

    @Provides
    fun providePendingNotificationDao(database: AppDatabase): PendingNotificationDao {
        return database.pendingNotificationDao()
    }
}
