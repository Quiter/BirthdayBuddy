package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingNotificationDao {
    @Query("SELECT * FROM pending_notifications WHERE isDone = 0")
    fun getActiveNotifications(): Flow<List<PendingNotification>>

    @Query("SELECT * FROM pending_notifications WHERE isDone = 0")
    suspend fun getActiveNotificationsImmediate(): List<PendingNotification>

    @Query("SELECT * FROM pending_notifications WHERE id = :id")
    suspend fun getNotificationById(id: Int): PendingNotification?

    @Query("SELECT EXISTS(SELECT 1 FROM pending_notifications WHERE year = :year AND daysBefore = :daysBefore AND contactLookupKeys LIKE :lookupKeyPattern)")
    suspend fun hasNotificationBeenScheduled(
        year: Int,
        daysBefore: Int,
        lookupKeyPattern: String
    ): Boolean

    @Upsert
    suspend fun upsert(notification: PendingNotification): Long

    @Query("UPDATE pending_notifications SET isDone = 1 WHERE id = :id")
    suspend fun markAsDone(id: Int)

    @Query("UPDATE pending_notifications SET dismissCount = dismissCount + 1 WHERE id = :id")
    suspend fun incrementDismissCount(id: Int)

    @Query("DELETE FROM pending_notifications")
    suspend fun deleteAll()
}
