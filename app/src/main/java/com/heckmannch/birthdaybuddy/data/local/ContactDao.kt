package com.heckmannch.birthdaybuddy.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
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

    @Query("DELETE FROM contacts WHERE lookupKey NOT IN (:keys)")
    suspend fun deleteContactsNotIn(keys: List<String>)

    @Transaction
    suspend fun refreshContacts(contacts: List<Contact>) {
        if (contacts.isEmpty()) {
            deleteAllContacts()
        } else {
            upsertContacts(contacts)
            val currentKeys = contacts.map { it.lookupKey }
            deleteContactsNotIn(currentKeys)
        }
    }
}
