package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for accessing and modifying label configurations.
 */
@Dao
interface LabelConfigDao {
    /**
     * Observes all label configurations as a [Flow].
     */
    @Query("SELECT * FROM label_configs")
    fun getAllConfigs(): Flow<List<LabelConfigEntity>>

    /**
     * Retrieves all label configurations immediately.
     */
    @Query("SELECT * FROM label_configs")
    suspend fun getAllConfigsImmediate(): List<LabelConfigEntity>

    /**
     * Retrieves the label configuration for a specific label name.
     */
    @Query("SELECT * FROM label_configs WHERE name = :name")
    suspend fun getConfigForLabel(name: String): LabelConfigEntity?

    /**
     * Inserts or updates a single label configuration.
     */
    @Upsert
    suspend fun upsertConfig(config: LabelConfigEntity)

    /**
     * Inserts or updates a list of label configurations.
     */
    @Upsert
    suspend fun upsertConfigs(configs: List<LabelConfigEntity>)

    /**
     * Deletes label configurations matching the provided list of case-insensitive names.
     */
    @Query("DELETE FROM label_configs WHERE LOWER(name) IN (:names)")
    suspend fun deleteConfigsByNames(names: List<String>)
}
