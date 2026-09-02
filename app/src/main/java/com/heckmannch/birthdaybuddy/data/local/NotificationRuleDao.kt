package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationRuleDao {
    @Query("SELECT * FROM notification_rules ORDER BY daysBefore ASC, hour ASC, minute ASC")
    fun getAllRules(): Flow<List<NotificationRuleEntity>>

    @Query("SELECT * FROM notification_rules ORDER BY daysBefore ASC, hour ASC, minute ASC")
    suspend fun getAllRulesImmediate(): List<NotificationRuleEntity>

    @Upsert
    suspend fun upsertRule(rule: NotificationRuleEntity)

    @Delete
    suspend fun deleteRule(rule: NotificationRuleEntity)
}
