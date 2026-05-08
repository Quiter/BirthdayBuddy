package com.heckmannch.birthdaybuddy2.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelConfigDao {
    @Query("SELECT * FROM label_configs")
    fun getAllConfigs(): Flow<List<LabelConfig>>

    @Query("SELECT * FROM label_configs")
    suspend fun getAllConfigsImmediate(): List<LabelConfig>

    @Query("SELECT * FROM label_configs WHERE name = :name")
    suspend fun getConfigForLabel(name: String): LabelConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: LabelConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfigs(configs: List<LabelConfig>)
}
