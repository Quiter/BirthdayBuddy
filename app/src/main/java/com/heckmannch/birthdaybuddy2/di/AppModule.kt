package com.heckmannch.birthdaybuddy.di

import android.content.Context
import com.heckmannch.birthdaybuddy.database.AppDatabase
import com.heckmannch.birthdaybuddy.database.ContactDao
import com.heckmannch.birthdaybuddy.database.LabelConfigDao
import com.heckmannch.birthdaybuddy.database.NotificationRuleDao
import com.heckmannch.birthdaybuddy.database.PendingNotificationDao
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
    fun provideAppSettingsDao(database: AppDatabase): com.heckmannch.birthdaybuddy.database.AppSettingsDao {
        return database.appSettingsDao()
    }
}
