package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for accessing and modifying custom user data associated with contacts.
 */
@Dao
interface ContactUserDataDao {
    /**
     * Observes all contact user data entries as a [Flow].
     */
    @Query("SELECT * FROM contact_user_data")
    fun getAllUserData(): Flow<List<ContactUserData>>

    /**
     * Retrieves all contact user data entries immediately.
     */
    @Query("SELECT * FROM contact_user_data")
    suspend fun getAllUserDataImmediate(): List<ContactUserData>

    /**
     * Retrieves user data for a specific contact identified by its [lookupKey].
     */
    @Query("SELECT * FROM contact_user_data WHERE lookupKey = :lookupKey")
    suspend fun getUserDataForContact(lookupKey: String): ContactUserData?

    /**
     * Inserts or updates a single contact user data entry.
     */
    @Upsert
    suspend fun upsertUserData(userData: ContactUserData)

    /**
     * Inserts or updates a list of contact user data entries.
     */
    @Upsert
    suspend fun upsertUserDataList(userDataList: List<ContactUserData>)
}
