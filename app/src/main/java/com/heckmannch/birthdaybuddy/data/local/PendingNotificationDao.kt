package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for querying and mutating pending notifications.
 */
@Dao
interface PendingNotificationDao {
    /**
     * Observes all active (undone) pending notifications as a [Flow].
     */
    @Query("SELECT * FROM pending_notifications WHERE isDone = 0")
    fun getActiveNotifications(): Flow<List<PendingNotificationEntity>>

    /**
     * Retrieves all active (undone) pending notifications immediately.
     */
    @Query("SELECT * FROM pending_notifications WHERE isDone = 0")
    suspend fun getActiveNotificationsImmediate(): List<PendingNotificationEntity>

    /**
     * Retrieves a pending notification by its unique [id].
     */
    @Query("SELECT * FROM pending_notifications WHERE id = :id")
    suspend fun getNotificationById(id: Int): PendingNotificationEntity?

    /**
     * Retrieves pending notifications scheduled for a specific [year] and advance days offset [daysBefore].
     */
    @Query("SELECT * FROM pending_notifications WHERE year = :year AND daysBefore = :daysBefore")
    suspend fun getScheduledNotifications(year: Int, daysBefore: Int): List<PendingNotificationEntity>

    /**
     * Checks whether a notification has already been scheduled for a given [year], [daysBefore] offset, and contact lookup key pattern.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM pending_notifications WHERE year = :year AND daysBefore = :daysBefore AND contactLookupKeys LIKE :lookupKeyPattern ESCAPE '\\')")
    suspend fun hasNotificationBeenScheduled(
        year: Int,
        daysBefore: Int,
        lookupKeyPattern: String
    ): Boolean

    /**
     * Inserts or updates a pending notification, returning its generated row ID.
     */
    @Upsert
    suspend fun upsert(notification: PendingNotificationEntity): Long

    /**
     * Marks a pending notification as completed/done.
     */
    @Query("UPDATE pending_notifications SET isDone = 1 WHERE id = :id")
    suspend fun markAsDone(id: Int)

    /**
     * Increments the swipe/dismiss attempt counter for a pending notification.
     */
    @Query("UPDATE pending_notifications SET dismissCount = dismissCount + 1 WHERE id = :id")
    suspend fun incrementDismissCount(id: Int)

    /**
     * Deletes stale notifications from previous years to prevent unbounded table growth.
     */
    @Query("DELETE FROM pending_notifications WHERE year < :currentYear - 1 OR (year < :currentYear AND isDone = 1)")
    suspend fun deleteOldNotifications(currentYear: Int)

    /**
     * Deletes all pending notifications from the database.
     */
    @Query("DELETE FROM pending_notifications")
    suspend fun deleteAll()
}
