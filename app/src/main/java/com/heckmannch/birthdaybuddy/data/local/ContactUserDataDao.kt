package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactUserDataDao {
    @Query("SELECT * FROM contact_user_data")
    fun getAllUserData(): Flow<List<ContactUserData>>

    @Query("SELECT * FROM contact_user_data")
    suspend fun getAllUserDataImmediate(): List<ContactUserData>

    @Query("SELECT * FROM contact_user_data WHERE lookupKey = :lookupKey")
    suspend fun getUserDataForContact(lookupKey: String): ContactUserData?

    @Upsert
    suspend fun upsertUserData(userData: ContactUserData)

    @Upsert
    suspend fun upsertUserDataList(userDataList: List<ContactUserData>)
}
