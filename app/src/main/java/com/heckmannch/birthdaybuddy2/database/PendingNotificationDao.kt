package com.heckmannch.birthdaybuddy2.database

import androidx.room.*

@Dao
interface PendingNotificationDao {
    @Query("SELECT * FROM pending_notifications WHERE isDone = 0")
    suspend fun getActiveNotificationsImmediate(): List<PendingNotification>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: PendingNotification): Long

    @Query("UPDATE pending_notifications SET isDone = 1 WHERE id = :id")
    suspend fun markAsDone(id: Int)
}
