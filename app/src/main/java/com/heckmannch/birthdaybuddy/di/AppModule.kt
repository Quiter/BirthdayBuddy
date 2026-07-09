package com.heckmannch.birthdaybuddy.di

import android.content.Context
import android.util.Log
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
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides a single instance of the application-wide room database for contact-related entities.
     *
     * Design: Declared `@Singleton` because database instances are heavy-weight resources that must be reused.
     *
     * @param context The application context.
     * @return The [AppDatabase] instance.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    /**
     * Provides a single instance of the settings database storing local app configs and styling preferences.
     *
     * Design: Managed separately from main app database to isolate user preferences/app settings.
     *
     * @param context The application context.
     * @return The [SettingsDatabase] instance.
     */
    @Provides
    @Singleton
    fun provideSettingsDatabase(@ApplicationContext context: Context): SettingsDatabase {
        return SettingsDatabase.getDatabase(context)
    }

    /**
     * Provides the Data Access Object (DAO) for contacts.
     *
     * Design: Marked `@Reusable` so Dagger can reuse instances without enforcing strict singletons, improving performance.
     *
     * @param database The [AppDatabase] dependency.
     * @return The [ContactDao] to execute queries.
     */
    @Provides
    @Reusable
    fun provideContactDao(database: AppDatabase): ContactDao {
        return database.contactDao()
    }

    /**
     * Provides the Data Access Object (DAO) for managing pending/scheduled notifications.
     *
     * Design: Reusable DAO instance to query and mutate pending notification records.
     *
     * @param database The [AppDatabase] dependency.
     * @return The [PendingNotificationDao].
     */
    @Provides
    @Reusable
    fun providePendingNotificationDao(database: AppDatabase): PendingNotificationDao {
        return database.pendingNotificationDao()
    }

    /**
     * Provides the Data Access Object (DAO) for contact label settings and visibility configs.
     *
     * Design: Accesses the settings database where configuration details are persisted.
     *
     * @param database The [SettingsDatabase] dependency.
     * @return The [LabelConfigDao].
     */
    @Provides
    @Reusable
    fun provideLabelConfigDao(database: SettingsDatabase): LabelConfigDao {
        return database.labelConfigDao()
    }

    /**
     * Provides the Data Access Object (DAO) for customized notification schedule rules.
     *
     * @param database The [SettingsDatabase] dependency.
     * @return The [NotificationRuleDao].
     */
    @Provides
    @Reusable
    fun provideNotificationRuleDao(database: SettingsDatabase): NotificationRuleDao {
        return database.notificationRuleDao()
    }

    /**
     * Provides the Data Access Object (DAO) for reading/writing global application settings.
     *
     * @param database The [SettingsDatabase] dependency.
     * @return The [AppSettingsDao].
     */
    @Provides
    @Reusable
    fun provideAppSettingsDao(database: SettingsDatabase): AppSettingsDao {
        return database.appSettingsDao()
    }

    /**
     * Provides the Data Access Object (DAO) for custom user data annotated on specific contacts.
     *
     * @param database The [SettingsDatabase] dependency.
     * @return The [ContactUserDataDao].
     */
    @Provides
    @Reusable
    fun provideContactUserDataDao(database: SettingsDatabase): ContactUserDataDao {
        return database.contactUserDataDao()
    }

    /**
     * Provides the application-scoped CoroutineScope.
     *
     * Design: Runs on [Dispatchers.Default] with a [SupervisorJob], and catches all unhandled exceptions
     * to log them safely, preventing application crashes.
     *
     * @return The [CoroutineScope] mapped to the application lifecycle.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("ApplicationScope", "Unhandled exception in applicationScope", throwable)
        }
        return CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)
    }
}
