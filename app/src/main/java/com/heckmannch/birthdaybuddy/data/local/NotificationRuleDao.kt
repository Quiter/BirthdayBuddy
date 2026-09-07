package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing notification schedule rules.
 */
@Dao
interface NotificationRuleDao {
    /**
     * Observes all notification rules sorted by advance days, hour, and minute.
     */
    @Query("SELECT * FROM notification_rules ORDER BY daysBefore ASC, hour ASC, minute ASC")
    fun getAllRules(): Flow<List<NotificationRuleEntity>>

    /**
     * Retrieves all notification rules immediately, sorted by advance days, hour, and minute.
     */
    @Query("SELECT * FROM notification_rules ORDER BY daysBefore ASC, hour ASC, minute ASC")
    suspend fun getAllRulesImmediate(): List<NotificationRuleEntity>

    /**
     * Retrieves a notification rule matching the specified [daysBefore] setting.
     */
    @Query("SELECT * FROM notification_rules WHERE daysBefore = :daysBefore LIMIT 1")
    suspend fun getRuleByDaysBefore(daysBefore: Int): NotificationRuleEntity?

    /**
     * Deletes notification rules matching the specified [daysBefore] value.
     */
    @Query("DELETE FROM notification_rules WHERE daysBefore = :daysBefore")
    suspend fun deleteRuleByDaysBefore(daysBefore: Int)

    /**
     * Inserts or updates a notification rule.
     */
    @Upsert
    suspend fun upsertRule(rule: NotificationRuleEntity)

    /**
     * Deletes a specific notification rule.
     */
    @Delete
    suspend fun deleteRule(rule: NotificationRuleEntity)
}
