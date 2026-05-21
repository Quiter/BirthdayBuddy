package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 0")
    fun getSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 0")
    suspend fun getSettingsImmediate(): AppSettings?

    @Upsert
    suspend fun upsertSettings(settings: AppSettings)
}
