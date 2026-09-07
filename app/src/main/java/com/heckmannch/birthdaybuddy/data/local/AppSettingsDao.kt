package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing application settings.
 */
@Dao
interface AppSettingsDao {
    /**
     * Observes the application settings row as a [Flow].
     */
    @Query("SELECT * FROM app_settings WHERE id = 0")
    fun getSettings(): Flow<AppSettingsEntity?>

    /**
     * Retrieves the current application settings immediately.
     */
    @Query("SELECT * FROM app_settings WHERE id = 0")
    suspend fun getSettingsImmediate(): AppSettingsEntity?

    /**
     * Inserts or updates the application settings row.
     */
    @Upsert
    suspend fun upsertSettings(settings: AppSettingsEntity)
}
