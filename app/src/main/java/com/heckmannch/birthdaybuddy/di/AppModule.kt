package com.heckmannch.birthdaybuddy.di

import android.content.Context
import com.heckmannch.birthdaybuddy.data.local.AppDatabase
import com.heckmannch.birthdaybuddy.data.local.AppSettingsDao
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.LabelConfigDao
import com.heckmannch.birthdaybuddy.data.local.NotificationRuleDao
import com.heckmannch.birthdaybuddy.data.local.PendingNotificationDao
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

    @Provides
    fun provideAppSettingsDao(database: AppDatabase): AppSettingsDao {
        return database.appSettingsDao()
    }
}
