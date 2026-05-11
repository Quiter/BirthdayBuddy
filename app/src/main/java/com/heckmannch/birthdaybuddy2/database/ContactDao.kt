package com.heckmannch.birthdaybuddy2.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY birthday ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts")
    suspend fun getAllContactsImmediate(): List<Contact>

    @Upsert
    suspend fun upsertContacts(contacts: List<Contact>)

    @Upsert
    suspend fun upsertContact(contact: Contact)

    @Query("SELECT * FROM contacts WHERE lookupKey = :lookupKey")
    suspend fun getContactByLookupKey(lookupKey: String): Contact?

    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()

    @Transaction
    suspend fun refreshContacts(contacts: List<Contact>) {
        deleteAllContacts()
        upsertContacts(contacts)
    }
}
