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
import com.heckmannch.birthdaybuddy.domain.repository.TimeRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface HelperBindingsModule {

    @Binds
    @Singleton
    fun bindWidgetUpdater(updater: BirthdayWidgetUpdater): WidgetUpdater

    @Binds
    @Singleton
    fun bindNotificationScheduler(scheduler: NotificationSchedulerImpl): NotificationScheduler

    @Binds
    @Singleton
    fun bindCalendarDataSource(dataSource: com.heckmannch.birthdaybuddy.data.repository.SystemCalendarDataSourceImpl): com.heckmannch.birthdaybuddy.data.repository.SystemCalendarDataSource

    @Binds
    @Singleton
    fun bindContactRepository(repository: ContactRepositoryImpl): ContactRepository

    @Binds
    @Singleton
    fun bindNotificationRepository(repository: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    fun bindCalendarSyncRepository(repository: CalendarSyncRepositoryImpl): CalendarSyncRepository

    @Binds
    @Singleton
    fun bindTimeRepository(repository: TimeRepositoryImpl): TimeRepository
}
