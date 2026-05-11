package com.heckmannch.birthdaybuddy2.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationRuleDao {
    @Query("SELECT * FROM notification_rules ORDER BY daysBefore ASC, hour ASC, minute ASC")
    fun getAllRules(): Flow<List<NotificationRule>>

    @Query("SELECT * FROM notification_rules ORDER BY daysBefore ASC, hour ASC, minute ASC")
    suspend fun getAllRulesImmediate(): List<NotificationRule>

    @Upsert
    suspend fun upsertRule(rule: NotificationRule)

    @Update
    suspend fun updateRule(rule: NotificationRule)

    @Delete
    suspend fun deleteRule(rule: NotificationRule)
}
