package com.heckmannch.birthdaybuddy2.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY birthday ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>)

    @Query("SELECT * FROM contacts WHERE lookupKey = :lookupKey")
    suspend fun getContactByLookupKey(lookupKey: String): Contact?

    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()

    @Transaction
    suspend fun refreshContacts(contacts: List<Contact>) {
        deleteAllContacts()
        insertContacts(contacts)
    }
}
