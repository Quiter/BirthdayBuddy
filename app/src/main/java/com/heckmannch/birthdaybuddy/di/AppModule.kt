package com.heckmannch.birthdaybuddy.di

import android.content.Context
import com.heckmannch.birthdaybuddy.data.local.AppDatabase
import com.heckmannch.birthdaybuddy.data.local.AppSettingsDao
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.ContactUserDataDao
import com.heckmannch.birthdaybuddy.data.local.LabelConfigDao
import com.heckmannch.birthdaybuddy.data.local.NotificationRuleDao
import com.heckmannch.birthdaybuddy.data.local.PendingNotificationDao
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideSettingsDatabase(@ApplicationContext context: Context): SettingsDatabase {
        return SettingsDatabase.getDatabase(context)
    }

    @Provides
    fun provideContactDao(database: AppDatabase): ContactDao {
        return database.contactDao()
    }

    @Provides
    fun providePendingNotificationDao(database: AppDatabase): PendingNotificationDao {
        return database.pendingNotificationDao()
    }

    @Provides
    fun provideLabelConfigDao(database: SettingsDatabase): LabelConfigDao {
        return database.labelConfigDao()
    }

    @Provides
    fun provideNotificationRuleDao(database: SettingsDatabase): NotificationRuleDao {
        return database.notificationRuleDao()
    }

    @Provides
    fun provideAppSettingsDao(database: SettingsDatabase): AppSettingsDao {
        return database.appSettingsDao()
    }

    @Provides
    fun provideContactUserDataDao(database: SettingsDatabase): ContactUserDataDao {
        return database.contactUserDataDao()
    }
}
