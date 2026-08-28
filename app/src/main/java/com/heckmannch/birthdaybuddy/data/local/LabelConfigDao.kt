package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelConfigDao {
    @Query("SELECT * FROM label_configs")
    fun getAllConfigs(): Flow<List<LabelConfigEntity>>

    @Query("SELECT * FROM label_configs")
    suspend fun getAllConfigsImmediate(): List<LabelConfigEntity>

    @Query("SELECT * FROM label_configs WHERE name = :name")
    suspend fun getConfigForLabel(name: String): LabelConfigEntity?

    @Upsert
    suspend fun upsertConfig(config: LabelConfigEntity)

    @Upsert
    suspend fun upsertConfigs(configs: List<LabelConfigEntity>)

    @Query("DELETE FROM label_configs WHERE LOWER(name) IN (:names)")
    suspend fun deleteConfigsByNames(names: List<String>)
}
