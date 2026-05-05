package com.heckmannch.birthdaybuddy2.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelConfigDao {
    @Query("SELECT * FROM label_configs")
    fun getAllConfigs(): Flow<List<LabelConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: LabelConfig)

    @Query("SELECT * FROM label_configs WHERE name = :name")
    suspend fun getConfigByName(name: String): LabelConfig?
}
