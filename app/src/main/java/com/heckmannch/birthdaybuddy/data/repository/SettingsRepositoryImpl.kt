package com.heckmannch.birthdaybuddy.data.repository

import com.heckmannch.birthdaybuddy.data.local.AppSettingsDao
import com.heckmannch.birthdaybuddy.data.local.AppSettingsEntity
import com.heckmannch.birthdaybuddy.data.mapper.AppSettingsMapper
import com.heckmannch.birthdaybuddy.di.DefaultDispatcher
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of [SettingsRepository] providing thread-safe mutation
 * and reactive observation of application settings stored in Room.
 */
class SettingsRepositoryImpl @Inject constructor(
    private val appSettingsDao: AppSettingsDao,
    private val appSettingsMapper: AppSettingsMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : SettingsRepository {

    private val settingsMutex = Mutex()

    override val settings: Flow<AppSettings> = appSettingsDao.getSettings()
        .map { entity -> appSettingsMapper.toDomain(entity ?: AppSettingsEntity()) }
        .flowOn(defaultDispatcher)
        .distinctUntilChanged()

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings): Unit =
        withContext(ioDispatcher) {
            settingsMutex.withLock {
                val currentEntity = appSettingsDao.getSettingsImmediate() ?: AppSettingsEntity()
                val currentDomain = appSettingsMapper.toDomain(currentEntity)
                val updatedDomain = transform(currentDomain)
                val updatedEntity = appSettingsMapper.toEntity(updatedDomain)
                appSettingsDao.upsertSettings(updatedEntity)
            }
        }

    override suspend fun getSettingsImmediate(): AppSettings = withContext(ioDispatcher) {
        val currentEntity = appSettingsDao.getSettingsImmediate() ?: AppSettingsEntity()
        appSettingsMapper.toDomain(currentEntity)
    }
}
