package com.heckmannch.birthdaybuddy.di

import com.heckmannch.birthdaybuddy.util.BirthdayWidgetUpdater
import com.heckmannch.birthdaybuddy.util.ImagePrefetcher
import com.heckmannch.birthdaybuddy.util.ImagePrefetcherImpl
import com.heckmannch.birthdaybuddy.util.NotificationScheduler
import com.heckmannch.birthdaybuddy.util.NotificationSchedulerImpl
import com.heckmannch.birthdaybuddy.util.WidgetUpdater
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
    fun bindImagePrefetcher(prefetcher: ImagePrefetcherImpl): ImagePrefetcher
}
