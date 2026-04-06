package com.heckmannch.birthdaybuddy.di

import com.heckmannch.birthdaybuddy.data.source.ContactDataSource
import com.heckmannch.birthdaybuddy.data.source.SystemContactDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Modul zur Bindung von Interfaces an ihre Implementierungen.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Binds
    @Singleton
    abstract fun bindContactDataSource(
        systemContactDataSource: SystemContactDataSource
    ): ContactDataSource
}
