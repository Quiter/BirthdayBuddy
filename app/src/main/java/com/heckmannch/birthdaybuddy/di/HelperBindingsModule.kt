package com.heckmannch.birthdaybuddy.di

import com.heckmannch.birthdaybuddy.data.repository.BirthdayWidgetUpdater
import com.heckmannch.birthdaybuddy.data.repository.CalendarSyncRepositoryImpl
import com.heckmannch.birthdaybuddy.data.repository.ContactRepositoryImpl
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepositoryImpl
import com.heckmannch.birthdaybuddy.data.repository.NotificationSchedulerImpl
import com.heckmannch.birthdaybuddy.data.repository.TimeRepositoryImpl
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationScheduler
import com.heckmannch.birthdaybuddy.data.permission.AndroidPermissionChecker
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
import com.heckmannch.birthdaybuddy.domain.repository.TimeRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import com.heckmannch.birthdaybuddy.util.Clock
import com.heckmannch.birthdaybuddy.util.SystemClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface HelperBindingsModule {

    /**
     * Binds the clock abstraction interface to its system clock implementation.
     *
     * @param clock The [SystemClock] implementation.
     * @return The bound [Clock] interface.
     */
    @Binds
    @Singleton
    fun bindClock(clock: SystemClock): Clock

    /**
     * Binds the widget updater abstraction to its implementation.
     *
     * @param updater The [BirthdayWidgetUpdater] implementation.
     * @return The bound [WidgetUpdater] interface.
     */
    @Binds
    @Singleton
    fun bindWidgetUpdater(updater: BirthdayWidgetUpdater): WidgetUpdater

    /**
     * Binds the notification scheduler abstraction to its implementation.
     *
     * @param scheduler The [NotificationSchedulerImpl] implementation.
     * @return The bound [NotificationScheduler] interface.
     */
    @Binds
    @Singleton
    fun bindNotificationScheduler(scheduler: NotificationSchedulerImpl): NotificationScheduler

    /**
     * Binds the system calendar data source abstraction to its implementation.
     *
     * @param dataSource The [com.heckmannch.birthdaybuddy.data.repository.SystemCalendarDataSourceImpl] implementation.
     * @return The bound [com.heckmannch.birthdaybuddy.data.repository.SystemCalendarDataSource] interface.
     */
    @Binds
    @Singleton
    fun bindCalendarDataSource(dataSource: com.heckmannch.birthdaybuddy.data.repository.SystemCalendarDataSourceImpl): com.heckmannch.birthdaybuddy.data.repository.SystemCalendarDataSource

    /**
     * Binds the contact repository interface to its implementation.
     *
     * @param repository The [ContactRepositoryImpl] implementation.
     * @return The bound [ContactRepository] interface.
     */
    @Binds
    @Singleton
    fun bindContactRepository(repository: ContactRepositoryImpl): ContactRepository

    /**
     * Binds the notification repository interface to its implementation.
     *
     * @param repository The [NotificationRepositoryImpl] implementation.
     * @return The bound [NotificationRepository] interface.
     */
    @Binds
    @Singleton
    fun bindNotificationRepository(repository: NotificationRepositoryImpl): NotificationRepository

    /**
     * Binds the calendar sync repository interface to its implementation.
     *
     * @param repository The [CalendarSyncRepositoryImpl] implementation.
     * @return The bound [CalendarSyncRepository] interface.
     */
    @Binds
    @Singleton
    fun bindCalendarSyncRepository(repository: CalendarSyncRepositoryImpl): CalendarSyncRepository

    /**
     * Binds the time repository interface to its implementation.
     *
     * @param repository The [TimeRepositoryImpl] implementation.
     * @return The bound [TimeRepository] interface.
     */
    @Binds
    @Singleton
    fun bindTimeRepository(repository: TimeRepositoryImpl): TimeRepository

    /**
     * Binds the permission checker abstraction to its Android implementation.
     *
     * @param checker The [AndroidPermissionChecker] implementation.
     * @return The bound [PermissionChecker] interface.
     */
    @Binds
    @Singleton
    fun bindPermissionChecker(checker: AndroidPermissionChecker): PermissionChecker
}
